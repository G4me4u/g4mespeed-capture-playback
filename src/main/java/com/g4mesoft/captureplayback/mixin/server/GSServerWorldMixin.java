package com.g4mesoft.captureplayback.mixin.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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

	private final List<GSPlaybackStream> playbackStreams = new ArrayList<>();
	private GSISignalFrame signalFrame;
	
	protected GSServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryKey,
			DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
		super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
	}
	
	@Shadow protected abstract boolean processBlockEvent(BlockEvent blockEvent);
	
	@Inject(method = "tick", at = @At("HEAD"))
	public void onTickHead(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
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
	
	@Inject(method = "processSyncedBlockEvents", at = @At("RETURN"))
	public void onProcessSyncedBlockEventsReturn(CallbackInfo ci) {
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
}
