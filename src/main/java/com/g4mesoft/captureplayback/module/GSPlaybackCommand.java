package com.g4mesoft.captureplayback.module;

import java.util.ArrayList;
import java.util.List;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSPlaybackEntry;
import com.g4mesoft.captureplayback.stream.GSPlaybackStream;
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

		GSSequence sequence = module.getActiveSequence();

		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).playStream(createPlaybackStream(sequence));
		
		source.sendFeedback(new LiteralText("Playback has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static GSPlaybackStream createPlaybackStream(GSSequence sequence) {
		List<GSPlaybackEntry> entries = new ArrayList<>();

		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int z0 = Integer.MAX_VALUE;

		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		int z1 = Integer.MIN_VALUE;
		
		for (GSChannel channel : sequence.getChannels()) {
			BlockPos pos = channel.getInfo().getPos();
			
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
			
			for (GSChannelEntry entry : channel.getEntries()) {
				GSEChannelEntryType type = entry.getType();
				if (type == GSEChannelEntryType.EVENT_BOTH || type == GSEChannelEntryType.EVENT_START)
					entries.add(new GSPlaybackEntry(entry.getStartTime(), GSESignalEdge.RISING_EDGE, pos));
				if (type == GSEChannelEntryType.EVENT_BOTH || type == GSEChannelEntryType.EVENT_END)
					entries.add(new GSPlaybackEntry(entry.getEndTime(), GSESignalEdge.FALLING_EDGE, pos));
			}
		}
		
		GSBlockRegion blockRegion = new GSBlockRegion(x0, y0, z0, x1, y1, z1);
		return new GSPlaybackStream(blockRegion, entries);
	}
}
