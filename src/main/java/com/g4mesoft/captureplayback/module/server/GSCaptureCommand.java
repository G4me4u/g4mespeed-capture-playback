package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public final class GSCaptureCommand {

	private GSCaptureCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("capture").then(CommandManager.literal("start").executes(context -> {
			return startCapture(context.getSource());
		})).then(CommandManager.literal("stopAll").executes(context -> {
			return stopAllCaptures(context.getSource());
		})));
	}
	
	private static int startCapture(ServerCommandSource source) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSComposition composition = module.getComposition();
		
		ServerWorld world = source.getServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_addCaptureStream(composition.getCaptureStream());
		
		source.sendFeedback(new LiteralText("Capture has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int stopAllCaptures(ServerCommandSource source) {
		ServerWorld world = source.getServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_getCaptureStreams().forEach(GSICaptureStream::close);
		
		source.sendFeedback(new LiteralText("All captures have stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
}
