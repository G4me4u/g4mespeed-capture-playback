package com.g4mesoft.captureplayback.mixin.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.g4mesoft.G4mespeedMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.access.GSIWorldAccess;
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
import com.g4mesoft.captureplayback.stream.handler.GSPoweredState;
import com.g4mesoft.captureplayback.stream.handler.GSServerWorldSignalEventContext;
import com.g4mesoft.core.compat.GSICarpetTickrateManager;
import com.g4mesoft.core.server.GSServerController;

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
import net.minecraft.util.math.Direction;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

@Mixin(ServerWorld.class)
public abstract class GSServerWorldMixin extends World implements GSIServerWorldAccess, GSIWorldAccess {

	@Shadow @Final private MinecraftServer server;
	
	@Unique
	private final GSCapturePlaybackExtension gcp_extension = GSCapturePlaybackExtension.getInstance();
	
	@Unique
	private final Map<UUID, GSIPlaybackStream> gcp_playbackStreams = new HashMap<>();
	@Unique
	private final Map<UUID, GSICaptureStream> gcp_captureStreams = new HashMap<>();
	
	@Unique
	private LinkedList<GSSignalEvent> gcp_capturedEvents = new LinkedList<>();
	@Unique
	private GSISignalFrame gcp_signalFrame;
	@Unique
	private Map<BlockPos, GSPoweredState> gcp_poweredStates = new HashMap<>();
	@Unique
	private GSISignalEventContext gcp_signalEventContext = new GSServerWorldSignalEventContext((ServerWorld)(Object)this);
	
	private GSETickPhase gcp_phase = GSETickPhase.BLOCK_EVENTS;
	private int gcp_blockEventCount = 0;
	private int gcp_microtick = -1;
	
	protected GSServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryKey,
			DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
		super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
	}
	
	@Shadow protected abstract boolean processBlockEvent(BlockEvent blockEvent);

	@Override @Shadow public abstract void addSyncedBlockEvent(BlockPos pos, Block block, int type, int data);
	
	@Inject(
		method = "tick",
		at = @At("HEAD")
	)
	public void onTickHead(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		// Make sure carpet is not doing tick freeze
		GSICarpetTickrateManager tickrateManager = G4mespeedMod.getCarpetCompat().getClientTickrateManager();
		if (!gcp_playbackStreams.isEmpty() && tickrateManager.runsNormally()) {
			GSMergedSignalFrame mergedFrame = new GSMergedSignalFrame();
			
			Iterator<GSIPlaybackStream> playbackStreamItr = gcp_playbackStreams.values().iterator();
			while (playbackStreamItr.hasNext()) {
				GSIPlaybackStream playbackStream = playbackStreamItr.next();
				if (playbackStream.isClosed()) {
					playbackStreamItr.remove();
				} else {
					mergedFrame.merge(playbackStream.read());
				}
			}
			
			gcp_signalFrame = mergedFrame;
		} else {
			gcp_signalFrame = GSISignalFrame.EMPTY;
		}
		
		// Clean up closed capture streams
		Iterator<GSICaptureStream> captureStreamItr = gcp_captureStreams.values().iterator();
		while (captureStreamItr.hasNext()) {
			GSICaptureStream captureStream = captureStreamItr.next();
			if (captureStream.isClosed())
				captureStreamItr.remove();
		}

		// Handle immediate playback events
		while (gcp_signalFrame.hasNext() && gcp_signalFrame.peek().getPhase() == GSETickPhase.IMMEDIATE)
			handleSignalEvent(gcp_signalFrame.next());

		// Prepare capturing of events
		gcp_capturedEvents.clear();
		gcp_signalFrame.mark();
	}
	
	@Unique
	private void handleReadySignalEvents() {
		// Handle playback events in the current breath.
		while (gcp_signalFrame.hasNext() && isEventReady(gcp_signalFrame.peek()))
			handleSignalEvent(gcp_signalFrame.next());
	}
	
	@Unique
	private void handleSignalEvent(GSSignalEvent event) {
		boolean rising = (event.getEdge() == GSESignalEdge.RISING_EDGE);

		// Turn position into a powered position.
		if (rising) {
			incrementPowered(event.getPos());
		} else {
			decrementPowered(event.getPos());
		}
		
		if (!event.isShadow()) {
			BlockState state = getBlockState(event.getPos());
	
			GSISignalEventHandler handler = gcp_extension.getSignalEventHandler(state.getBlock());
			if (handler != null)
				handler.handle(state, event, gcp_signalEventContext);
		}
	}
	
	@Unique
	private boolean isEventReady(GSSignalEvent event) {
		// We are not in the correct phase
		if (gcp_phase.isBefore(event.getPhase()))
			return false;
		// If we are in block event phase, check microtick
		if (gcp_phase == GSETickPhase.BLOCK_EVENTS && event.getPhase() == gcp_phase)
			return (gcp_microtick >= event.getMicrotick());
		return true;
	}
	
	@Inject(
		method = "tick",
		at = @At("RETURN")
	)
	public void onTickReturn(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		if (!gcp_captureStreams.isEmpty()) {
			gcp_signalFrame.reset();
			
			GSISignalFrame capturedFrame;
			if (gcp_capturedEvents.isEmpty()) {
				capturedFrame = gcp_signalFrame;
			} else {
				GSMergedSignalFrame mergedFrame = new GSMergedSignalFrame();
				mergedFrame.merge(gcp_signalFrame);
				mergedFrame.merge(new GSBasicSignalFrame(gcp_capturedEvents));
				capturedFrame = mergedFrame;
			}
			
			for (GSICaptureStream captureStream : gcp_captureStreams.values())
				captureStream.write(capturedFrame);
		}
	}

	@Inject(
		method = "processSyncedBlockEvents",
		at = @At("HEAD")
	)
	public void onBlockActionHead(CallbackInfo ci) {
		gcp_phase = GSETickPhase.BLOCK_EVENTS;
		
		gcp_blockEventCount = 0;
		gcp_microtick = -1;
	}

	@Redirect(
		method = "processSyncedBlockEvents",
		at = @At(
			value = "INVOKE",
			ordinal = 0,
			target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;isEmpty()Z"
		)
	)
	public boolean onProcessSyncedBlockEventsLoop(ObjectLinkedOpenHashSet<BlockEvent> blockEventQueue) {
		if (gcp_blockEventCount == 0) {
			gcp_blockEventCount = blockEventQueue.size();
			gcp_microtick++;
		}

		if (gcp_signalFrame.hasNext()) {
			// No more block events from external sources,
			// so we have to skip a few microticks.
			if (gcp_blockEventCount == 0) {
				do {
					gcp_microtick = gcp_signalFrame.peek().getMicrotick();
					handleReadySignalEvents();
					// Check if we had new events added to the queue
					gcp_blockEventCount = blockEventQueue.size();
				} while (gcp_signalFrame.hasNext() && gcp_blockEventCount == 0);
			} else {
				handleReadySignalEvents();
			}
		}
		
		return (gcp_blockEventCount == 0);
	}
	
	@Inject(
		method = "processSyncedBlockEvents",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/server/world/ServerWorld;processBlockEvent(" +
					"Lnet/minecraft/server/world/BlockEvent;" +
				")Z"
		)
	)
	public void onProcessSyncedBlockEventsProcessing(CallbackInfo ci) {
		gcp_blockEventCount--;
	}
	
	@Inject(
		method = "processSyncedBlockEvents",
		locals = LocalCapture.CAPTURE_FAILEXCEPTION,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/server/MinecraftServer;getPlayerManager(" +
				")Lnet/minecraft/server/PlayerManager;"
		)
	)
	public void onProcessSyncedBlockEventsSuccess(CallbackInfo ci, BlockEvent blockEvent) {
		if (gcp_isCapturePosition(blockEvent.getPos())) {
			Block block = blockEvent.getBlock();
			
			if (block == Blocks.STICKY_PISTON || block == Blocks.PISTON) {
				// TODO: move this out of the world mixin
				GSESignalEdge edge = (blockEvent.getType() == 0) ? GSESignalEdge.RISING_EDGE :
				                                                   GSESignalEdge.FALLING_EDGE;
				gcp_handleCaptureEvent(edge, blockEvent.getPos());
			}
		}
	}
	
	@Inject(
		method = "processSyncedBlockEvents",
		at = @At("RETURN")
	)
	public void onProcessSyncedBlockEventsReturn(CallbackInfo ci) {
		// We are done with the block event phase
		gcp_microtick = -1;
	}
	
	@Override
	public boolean gcp_isPoweredByPlayback(BlockPos pos) {
		Block block = getBlockState(pos).getBlock();
		if (gcp_extension.hasSignalEventHandler(block)) {
			// Check if the playback is currently powering this block
			return gcp_isPlaybackPowering(pos);
		}
		return false;
	}
	
	@Override
	public void gcp_handleCaptureEvent(GSESignalEdge edge, BlockPos pos) {
		if (!gcp_captureStreams.isEmpty())
			gcp_capturedEvents.add(new GSSignalEvent(gcp_phase, gcp_microtick, gcp_capturedEvents.size(), edge, pos, false));
	}
	
	@Override
	public boolean gcp_hasPlaybackStream(UUID assetUUID) {
		return gcp_playbackStreams.containsKey(assetUUID);
	}
	
	@Override
	public void gcp_addPlaybackStream(UUID assetUUID, GSIPlaybackStream playbackStream) {
		if (!playbackStream.isClosed())
			gcp_playbackStreams.put(assetUUID, playbackStream);
	}
	
	@Override
	public GSIPlaybackStream gcp_getPlaybackStream(UUID assetUUID) {
		return gcp_playbackStreams.get(assetUUID);
	}
	
	@Override
	public Collection<GSIPlaybackStream> gcp_getPlaybackStreams() {
		return Collections.unmodifiableCollection(gcp_playbackStreams.values());
	}

	@Override
	public boolean gcp_hasCaptureStream(UUID assetUUID) {
		return gcp_captureStreams.containsKey(assetUUID);
	}
	
	@Override
	public void gcp_addCaptureStream(UUID assetUUID, GSICaptureStream captureStream) {
		if (!captureStream.isClosed())
			gcp_captureStreams.put(assetUUID, captureStream);
	}

	@Override
	public GSICaptureStream gcp_getCaptureStream(UUID assetUUID) {
		return gcp_captureStreams.get(assetUUID);
	}
	
	@Override
	public Collection<GSICaptureStream> gcp_getCaptureStreams() {
		return Collections.unmodifiableCollection(gcp_captureStreams.values());
	}
	
	@Override
	public boolean gcp_isPlaybackPosition(BlockPos pos) {
		return isPositionInStreams(gcp_playbackStreams.values(), pos);
	}
	
	@Override
	public boolean gcp_isCapturePosition(BlockPos pos) {
		return isPositionInStreams(gcp_captureStreams.values(), pos);
	}
	
	@Unique
	private boolean isPositionInStreams(Collection<? extends GSIStream> streams, BlockPos pos) {
		for (GSIStream stream : streams) {
			if (stream.getBlockRegion().contains(pos.getX(), pos.getY(), pos.getZ()))
				return true;
		}
		
		return false;
	}
	
	@Override
	public boolean gcp_isPlaybackPowering(BlockPos pos) {
		GSPoweredState poweredState = gcp_poweredStates.get(pos);
		return (poweredState != null && poweredState.isPowered());
	}
	
	@Unique
	private void incrementPowered(BlockPos pos) {
		GSPoweredState poweredState = gcp_poweredStates.get(pos);
		
		if (poweredState != null) {
			poweredState.increment();
		} else {
			gcp_poweredStates.put(pos, new GSPoweredState(1));
		}
	}

	@Unique
	private void decrementPowered(BlockPos pos) {
		GSPoweredState poweredState = gcp_poweredStates.get(pos);
		
		if (poweredState != null) {
			poweredState.decrement();
		
			if (!poweredState.isPowered())
				gcp_poweredStates.remove(pos);
		}
	}
	
	@Override
	public boolean gcp_dispatchBlockEvent(BlockPos pos, Block block, int type, int data) {
		BlockEvent blockAction = new BlockEvent(pos, block, type, data);

		if (this.processBlockEvent(blockAction)) {
			Packet<?> packet = new BlockEventS2CPacket(pos, block, type, data);

			double dist = 64.0;
			if (block instanceof PistonBlock) {
				// This is a g4mespeed specific feature that allows the user
				// to change the distance at which the block actions are sent.
				dist = 16.0 * GSServerController.getInstance().getTpsModule().sBlockEventDistance.get();
			}

			PlayerManager playerManager = server.getPlayerManager();
			playerManager.sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), dist, getRegistryKey(), packet);

			return true;
		}

		return false;
	}
	
	@Override
	public void gcp_dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir) {
		updateNeighbor(pos, fromBlock, pos.offset(fromDir));
	}
	
	@Override
	public boolean gcp_setState(BlockPos pos, BlockState state, int flags) {
		return setBlockState(pos, state, flags);
	}
}
