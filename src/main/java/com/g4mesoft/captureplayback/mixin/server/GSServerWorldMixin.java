package com.g4mesoft.captureplayback.mixin.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
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
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

@Mixin(ServerWorld.class)
public abstract class GSServerWorldMixin extends World implements GSIServerWorldAccess, GSISignalEventContext {

	@Shadow @Final private MinecraftServer server;
	@Shadow @Final private ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;
	
	private final List<GSIPlaybackStream> playbackStreams = new ArrayList<>();
	private final List<GSICaptureStream> captureStreams = new ArrayList<>();
	
	private LinkedList<GSSignalEvent> capturedEvents = new LinkedList<>();
	private GSISignalFrame signalFrame;
	
	private GSETickPhase phase = GSETickPhase.BLOCK_EVENTS;
	private int blockEventCount = 0;
	private int microtick = -1;
	
	protected GSServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryKey,
			DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
		super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
	}
	
	@Shadow protected abstract boolean processBlockEvent(BlockEvent blockEvent);
	
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

	@Inject(method = "processSyncedBlockEvents", at = @At("RETURN"))
	public void onBlockActionHead(CallbackInfo ci) {
		phase = GSETickPhase.BLOCK_EVENTS;
	}

	@Inject(method = "processSyncedBlockEvents", at = @At(value = "INVOKE", shift = Shift.BEFORE,
			target = "Lnet/minecraft/server/world/ServerWorld;processBlockEvent(Lnet/minecraft/server/world/BlockEvent;)Z"))
	public void onProcessSyncedBlockEventsProcessing(CallbackInfo ci) {
		if (blockEventCount == 0) {
			// At this point we have already removed 1 block event.
			blockEventCount = this.syncedBlockEventQueue.size();
			microtick++;
		} else {
			blockEventCount--;
		}
	
		handleBlockEventPlayback(false);
	}
	
	@Inject(method = "processSyncedBlockEvents", locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(value = "INVOKE", shift = Shift.BEFORE,
			target = "Lnet/minecraft/server/MinecraftServer;getPlayerManager()Lnet/minecraft/server/PlayerManager;"))
	public void onProcessSyncedBlockEventsSuccess(CallbackInfo ci, BlockEvent blockEvent) {
		if (isCapturePosition(blockEvent.getPos())) {
			Block block = blockEvent.getBlock();
			
			if (block == Blocks.STICKY_PISTON || block == Blocks.PISTON) {
				// TODO: move this out of the world mixin
				GSESignalEdge edge = (blockEvent.getType() == 0) ? GSESignalEdge.RISING_EDGE :
				                                                    GSESignalEdge.FALLING_EDGE;
				handleCaptureEvent(edge, blockEvent.getPos());
			}
		}
	}
	
	@Inject(method = "processSyncedBlockEvents", at = @At("RETURN"))
	public void onProcessSyncedBlockEventsReturn(CallbackInfo ci) {
		handleBlockEventPlayback(true);
		// We are done with the block event phase
		microtick = -1;
	}
	
	private void handleBlockEventPlayback(boolean skipTimeCheck) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		Map<Block, GSISignalEventHandler> handlerRegistry = extension.getSignalEventHandlerRegistry();
		
		while (signalFrame.hasNext() && (skipTimeCheck || signalFrame.peek().getMicrotick() == microtick)) {
			GSSignalEvent event = signalFrame.next();
			
			BlockState state = getBlockState(event.getPos());

			GSISignalEventHandler handler = handlerRegistry.get(state.getBlock());
			if (handler != null) {
				microtick = event.getMicrotick();
				handler.handle(state, event, this);
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
		BlockEvent blockAction = new BlockEvent(pos, block, type, data);
		
		if (this.processBlockEvent(blockAction)) {
            Packet<?> packet = new BlockEventS2CPacket(pos, block, type, data);

            double dist = 64.0;
            if (block instanceof PistonBlock) {
            	// This is a g4mespeed specific feature that allows the user
            	// to change the distance at which the block actions are sent.
	            dist = 16.0 * GSControllerServer.getInstance().getTpsModule().sBlockEventDistance.getValue();
            }

            PlayerManager playerManager = server.getPlayerManager();
            playerManager.sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), dist, getRegistryKey(), packet);
            
            return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setState0(BlockPos pos, BlockState state, int flags) {
		return setBlockState(pos, state, flags);
	}
}
