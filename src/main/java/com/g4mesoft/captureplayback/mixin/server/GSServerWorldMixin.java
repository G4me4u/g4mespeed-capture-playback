package com.g4mesoft.captureplayback.mixin.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.stream.GSPlaybackStream;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSMergedSignalFrame;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventContext;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventHandler;
import com.g4mesoft.core.server.GSControllerServer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
	
	private final List<GSPlaybackStream> playbackStreams = new ArrayList<>();
	private GSISignalFrame signalFrame;
	
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
			
			Iterator<GSPlaybackStream> streamItr = playbackStreams.iterator();
			while (streamItr.hasNext()) {
				GSPlaybackStream stream = streamItr.next();
				if (stream.isClosed()) {
					streamItr.remove();
				} else {
					mergedFrame.merge(stream.read());
				}
			}
			
			signalFrame = mergedFrame;
		}
	}
	
	@Inject(method = "sendBlockActions", at = @At("RETURN"))
	public void onSendBlockActionsReturn(CallbackInfo ci) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		Map<Block, GSISignalEventHandler> handlerRegistry = extension.getSignalEventHandlerRegistry();
		
		while (signalFrame.hasNext()) {
			GSSignalEvent event = signalFrame.next();
			BlockState state = getBlockState(event.getPos());

			GSISignalEventHandler handler = handlerRegistry.get(state.getBlock());
			if (handler != null)
				handler.handle(state, event, this);
		}
	}
	
	@Override
	public void playStream(GSPlaybackStream stream) {
		if (!stream.isClosed())
			playbackStreams.add(stream);
	}
	
	@Override
	public boolean isPlaybackPosition(BlockPos pos) {
		for (GSPlaybackStream stream : playbackStreams) {
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
}
