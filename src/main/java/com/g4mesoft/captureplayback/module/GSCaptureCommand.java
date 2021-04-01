package com.g4mesoft.captureplayback.module;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.world.dimension.DimensionType;

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
		GSCapturePlaybackModule module = extension.getServerModule();

		GSSequence sequence = module.getActiveSequence();
		
		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).addCaptureStream(sequence.getCaptureStream());
		
		source.sendFeedback(new LiteralText("Capture has started."), true);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int stopAllCaptures(ServerCommandSource source) {
		ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
		((GSIServerWorldAccess)world).getCaptureStreams().forEach(GSICaptureStream::close);
		
		source.sendFeedback(new LiteralText("All captures have stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
}
