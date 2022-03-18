package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

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
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSComposition composition = module.getComposition();

		ServerWorld world = source.getServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_addPlaybackStream(composition.getPlaybackStream());
		
		source.sendFeedback(new LiteralText("Playback has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int stopAllPlaybacks(ServerCommandSource source) {
		ServerWorld world = source.getServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_getPlaybackStreams().forEach(GSIPlaybackStream::close);
		
		source.sendFeedback(new LiteralText("All playbacks have stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
}
