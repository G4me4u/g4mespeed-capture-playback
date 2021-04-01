package com.g4mesoft.captureplayback.module;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.world.dimension.DimensionType;

public final class GSPlaybackCommand {

	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("playback").then(CommandManager.literal("start").executes(context -> {
			return startPlayback(context.getSource());
		})).then(CommandManager.literal("stopAll").executes(context -> {
			return stopAllPlaybacks(context.getSource());
		})));
	}
	
	private static int startPlayback(ServerCommandSource source) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackModule module = extension.getServerModule();

		GSSequence sequence = module.getActiveSequence();

		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).addPlaybackStream(sequence.getPlaybackStream());
		
		source.sendFeedback(new LiteralText("Playback has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int stopAllPlaybacks(ServerCommandSource source) {
		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).getPlaybackStreams().forEach(GSIPlaybackStream::close);
		
		source.sendFeedback(new LiteralText("All playbacks have stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
}
