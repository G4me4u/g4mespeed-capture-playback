package com.g4mesoft.captureplayback.mixin.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSETickPhase;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.captureplayback.stream.GSIStream;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;
import com.g4mesoft.captureplayback.stream.frame.GSBasicSignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSMergedSignalFrame;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventContext;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventHandler;
import com.g4mesoft.core.server.GSControllerServer;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockActionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.BlockAction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;

@Mixin(ServerWorld.class)
public abstract class GSServerWorldMixin extends World implements GSIServerWorldAccess, GSISignalEventContext {

	@Shadow @Final private MinecraftServer server;
	@Shadow @Final private ObjectLinkedOpenHashSet<BlockAction> pendingBlockActions;
	
	private final List<GSIPlaybackStream> playbackStreams = new ArrayList<>();
	private final List<GSICaptureStream> captureStreams = new ArrayList<>();
	
	private LinkedList<GSSignalEvent> capturedEvents = new LinkedList<>();
	private GSISignalFrame signalFrame;
	
	private GSETickPhase phase = GSETickPhase.BLOCK_EVENTS;
	private int blockEventCount = 0;
	private int microtick = -1;
	
	protected GSServerWorldMixin(LevelProperties levelProperties, DimensionType dimensionType,
			BiFunction<World, Dimension, ChunkManager> chunkManagerProvider, Profiler profiler, boolean isClient) {
		super(levelProperties, dimensionType, chunkManagerProvider, profiler, isClient);
	}

	@Shadow protected abstract boolean method_14174(BlockAction blockAction);
	
	@Inject(method = "tick", at = @At("HEAD"))
	public void onTickHead(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		if (playbackStreams.isEmpty()) {
			signalFrame = GSISignalFrame.EMPTY;
		} else {
			GSMergedSignalFrame mergedFrame = new GSMergedSignalFrame();
			
			Iterator<GSIPlaybackStream> playbackStreamItr = playbackStreams.iterator();
			while (playbackStreamItr.hasNext()) {
				GSIPlaybackStream playbackStream = playbackStreamItr.next();
				if (playbackStream.isClosed()) {
					playbackStreamItr.remove();
				} else {
					mergedFrame.merge(playbackStream.read());
				}
			}
			
			signalFrame = mergedFrame;
		}
		
		// Clean up closed capture streams
		Iterator<GSICaptureStream> captureStreamItr = captureStreams.iterator();
		while (captureStreamItr.hasNext()) {
			GSICaptureStream captureStream = captureStreamItr.next();
			if (captureStream.isClosed())
				captureStreamItr.remove();
		}

		capturedEvents.clear();
		signalFrame.mark();
	}
	
	@Inject(method = "tick", at = @At("RETURN"))
	public void onTickReturn(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		if (!captureStreams.isEmpty()) {
			signalFrame.reset();
			
			GSISignalFrame capturedFrame;
			if (capturedEvents.isEmpty()) {
				capturedFrame = signalFrame;
			} else {
				GSMergedSignalFrame mergedFrame = new GSMergedSignalFrame();
				mergedFrame.merge(signalFrame);
				mergedFrame.merge(new GSBasicSignalFrame(capturedEvents));
				capturedFrame = mergedFrame;
			}
			
			for (GSICaptureStream captureStream : captureStreams)
				captureStream.write(capturedFrame);
		}
	}

	@Inject(method = "sendBlockActions", at = @At("RETURN"))
	public void onBlockActionHead(CallbackInfo ci) {
		phase = GSETickPhase.BLOCK_EVENTS;
	}

	@Inject(method = "sendBlockActions", at = @At(value = "INVOKE", shift = Shift.BEFORE,
			target = "Lnet/minecraft/server/world/ServerWorld;method_14174(Lnet/minecraft/server/world/BlockAction;)Z"))
	public void onBlockActionProcessing(CallbackInfo ci) {
		if (blockEventCount == 0) {
			// At this point we have already removed 1 block event.
			blockEventCount = this.pendingBlockActions.size();
			microtick++;
		} else {
			blockEventCount--;
		}
	
		handleBlockEventPlayback(false);
	}
	
	@Inject(method = "sendBlockActions", locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(value = "INVOKE", shift = Shift.BEFORE,
			target = "Lnet/minecraft/server/MinecraftServer;getPlayerManager()Lnet/minecraft/server/PlayerManager;"))
	public void onBlockActionSuccess(CallbackInfo ci, BlockAction blockAction) {
		if (isCapturePosition(blockAction.getPos())) {
			Block block = blockAction.getBlock();
			
			if (block == Blocks.STICKY_PISTON || block == Blocks.PISTON) {
				// TODO: move this out of the world mixin
				GSESignalEdge edge = (blockAction.getType() == 0) ? GSESignalEdge.RISING_EDGE :
				                                                    GSESignalEdge.FALLING_EDGE;
				handleCaptureEvent(edge, blockAction.getPos());
			}
		}
	}
	
	@Inject(method = "sendBlockActions", at = @At("RETURN"))
	public void onSendBlockActionsReturn(CallbackInfo ci) {
		handleBlockEventPlayback(true);
		// We are done with the block event phase
		microtick = -1;
	}
	
	private void handleBlockEventPlayback(boolean skipMicrotickCheck) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		Map<Block, GSISignalEventHandler> handlerRegistry = extension.getSignalEventHandlerRegistry();
		
		while (signalFrame.hasNext()) {
			GSSignalEvent event = signalFrame.next();
			
			if (skipMicrotickCheck || event.getMicrotick() == microtick) {
				BlockState state = getBlockState(event.getPos());

				GSISignalEventHandler handler = handlerRegistry.get(state.getBlock());
				if (handler != null) {
					microtick = event.getMicrotick();
					handler.handle(state, event, this);
				}
			}
		}
	}
	
	@Override
	public void handleCaptureEvent(GSESignalEdge edge, BlockPos pos) {
		capturedEvents.add(new GSSignalEvent(phase, microtick, capturedEvents.size(), edge, pos));
	}
	
	@Override
	public void addPlaybackStream(GSIPlaybackStream playbackStream) {
		if (!playbackStream.isClosed())
			playbackStreams.add(playbackStream);
	}
	
	@Override
	public List<GSIPlaybackStream> getPlaybackStreams() {
		return Collections.unmodifiableList(playbackStreams);
	}
	
	@Override
	public void addCaptureStream(GSICaptureStream captureStream) {
		if (!captureStream.isClosed())
			captureStreams.add(captureStream);
	}
	
	@Override
	public List<GSICaptureStream> getCaptureStreams() {
		return Collections.unmodifiableList(captureStreams);
	}
	
	@Override
	public boolean isPlaybackPosition(BlockPos pos) {
		return isPositionInStreams(playbackStreams, pos);
	}
	
	@Override
	public boolean isCapturePosition(BlockPos pos) {
		return isPositionInStreams(captureStreams, pos);
	}
	
	private boolean isPositionInStreams(List<? extends GSIStream> streams, BlockPos pos) {
		for (GSIStream stream : streams) {
			if (stream.getBlockRegion().contains(pos.getX(), pos.getY(), pos.getZ()))
				return true;
		}
		
		return false;
	}
	
	@Override
	public boolean dispatchBlockAction(BlockPos pos, Block block, int type, int data) {
		BlockAction blockAction = new BlockAction(pos, block, type, data);
		
		if (this.method_14174(blockAction)) {
            Packet<?> packet = new BlockActionS2CPacket(pos, block, type, data);

            double dist = 64.0;
            if (block instanceof PistonBlock) {
            	// This is a g4mespeed specific feature that allows the user
            	// to change the distance at which the block actions are sent.
	            dist = 16.0 * GSControllerServer.getInstance().getTpsModule().sBlockEventDistance.getValue();
            }

            PlayerManager playerManager = server.getPlayerManager();
            playerManager.sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), dist, dimension.getType(), packet);
            
            return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setState0(BlockPos pos, BlockState state, int flags) {
		return setBlockState(pos, state, flags);
	}
}
