package com.g4mesoft.captureplayback.module;

import java.util.ArrayList;
import java.util.List;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.playback.GSPlaybackEvent;
import com.g4mesoft.captureplayback.stream.playback.GSPlaybackStream;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

public final class GSPlaybackCommand {

	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("playback").then(CommandManager.literal("start").executes(context -> {
			return startPlayback(context.getSource());
		})));
	}
	
	private static int startPlayback(ServerCommandSource source) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackModule module = extension.getServerModule();

		GSTimeline timeline = module.getActiveTimeline();

		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).startPlaybackStream(createPlaybackStream(timeline));
		
		source.sendFeedback(new LiteralText("Playback has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static GSPlaybackStream createPlaybackStream(GSTimeline timeline) {
		List<GSPlaybackEvent> events = new ArrayList<>();

		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int z0 = Integer.MAX_VALUE;

		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		int z1 = Integer.MIN_VALUE;
		
		for (GSTrack track : timeline.getTracks()) {
			BlockPos pos = track.getInfo().getPos();
			
			if (pos.getX() < x0)
				x0 = pos.getX();
			if (pos.getY() < y0)
				y0 = pos.getY();
			if (pos.getZ() < z0)
				z0 = pos.getZ();

			if (pos.getX() > x1)
				x1 = pos.getX();
			if (pos.getY() > y1)
				y1 = pos.getY();
			if (pos.getZ() > z1)
				z1 = pos.getZ();
			
			for (GSTrackEntry entry : track.getEntries()) {
				GSETrackEntryType type = entry.getType();
				if (type == GSETrackEntryType.EVENT_BOTH || type == GSETrackEntryType.EVENT_START)
					events.add(new GSPlaybackEvent(pos, entry.getStartTime(), GSESignalEdge.RISING_EDGE));
				if (type == GSETrackEntryType.EVENT_BOTH || type == GSETrackEntryType.EVENT_END)
					events.add(new GSPlaybackEvent(pos, entry.getEndTime(), GSESignalEdge.FALLING_EDGE));
			}
		}
		
		GSBlockRegion blockRegion = new GSBlockRegion(x0, y0, z0, x1, y1, z1);
		return new GSPlaybackStream(blockRegion, events);
	}
}
