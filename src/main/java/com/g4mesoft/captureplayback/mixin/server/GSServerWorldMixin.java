package com.g4mesoft.captureplayback.mixin.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.GSEGameTickPhase;
import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.playback.GSPlaybackEvent;
import com.g4mesoft.captureplayback.stream.playback.GSPlaybackFrame;
import com.g4mesoft.captureplayback.stream.playback.GSPlaybackStream;
import com.g4mesoft.core.server.GSControllerServer;

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
public abstract class GSServerWorldMixin extends World implements GSIServerWorldAccess {

	@Shadow @Final private MinecraftServer server;
	
	protected GSServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryKey,
			DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
		super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
	}

	private final List<GSPlaybackStream> playbackStreams = new ArrayList<GSPlaybackStream>();

	@Shadow protected abstract boolean processBlockEvent(BlockEvent blockEvent);
	
	@Inject(method = "processSyncedBlockEvents", at = @At("RETURN"))
	public void onProcessSyncedBlockEventsReturn(CallbackInfo ci) {
		Iterator<GSPlaybackStream> streamItr = playbackStreams.iterator();
		
		while (streamItr.hasNext()) {
			GSPlaybackStream stream = streamItr.next();
			GSPlaybackFrame frame = stream.read();
			
			Iterator<GSPlaybackEvent> frameItr = frame.getPhaseIterator(GSEGameTickPhase.BLOCK_EVENTS);
			while (frameItr.hasNext()) {
				GSPlaybackEvent event = frameItr.next();

				BlockPos pos = event.getPos();
				BlockState blockState = getBlockState(pos);
				Block block = blockState.getBlock();
				
				if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
					int type = (event.getEdge() == GSESignalEdge.RISING_EDGE) ? 0 : 1;
					int data = blockState.get(PistonBlock.FACING).getId();
					BlockEvent blockEvent = new BlockEvent(pos, block, type, data);
					
					if (this.processBlockEvent(blockEvent)) {
			            PlayerManager playerManager = server.getPlayerManager();
			            Packet<?> packet = new BlockEventS2CPacket(pos, block, type, data);

			            double dist = 16.0 * GSControllerServer.getInstance().getTpsModule().sBlockEventDistance.getValue();
			            playerManager.sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), dist, getRegistryKey(), packet);
					}
				}
			}
			
			if (stream.isClosed())
				streamItr.remove();
		}
	}
	
	@Override
	public void startPlaybackStream(GSPlaybackStream playbackStream) {
		if (!playbackStream.isClosed())
			playbackStreams.add(playbackStream);
	}
	
	@Override
	public boolean isPlaybackPosition(BlockPos pos) {
		for (GSPlaybackStream stream : playbackStreams) {
			if (stream.getBlockRegion().contains(pos.getX(), pos.getY(), pos.getZ()))
				return true;
		}
		
		return false;
	}
}
