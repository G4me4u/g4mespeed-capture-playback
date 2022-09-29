package com.g4mesoft.captureplayback.module.server;

import java.util.UUID;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public final class GSPlaybackCommand {

	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("playback");
		
		command.then(CommandManager.literal("start").then(CommandManager.argument("assetUUID", UuidArgumentType.uuid()).suggests(new GSAssetSuggestionProvider()).executes(context -> {
			return startPlayback(context.getSource(), UuidArgumentType.getUuid(context, "assetUUID"));
		}))).then(CommandManager.literal("stop").then(CommandManager.argument("assetUUID", UuidArgumentType.uuid()).suggests(new GSAssetSuggestionProvider()).executes(context -> {
			return stopPlayback(context.getSource(), UuidArgumentType.getUuid(context, "assetUUID"));
		}))).then(CommandManager.literal("stopAll").executes(context -> {
			return stopAllPlayback(context.getSource());
		}));
		
		dispatcher.register(command);
	}
	
	private static int startPlayback(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, assetUUID);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		if (((GSIServerWorldAccess)world).gcp_hasPlaybackStream(assetUUID)) {
			source.sendError(new LiteralText("Already playing back '" + assetUUID + "'."));
			return 0;
		}
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		boolean success = module.getAssetStorage().requestReadOnce((asset) -> {
			((GSIServerWorldAccess)world).gcp_addPlaybackStream(assetUUID, asset.getPlaybackStream());
		}, assetUUID);
		
		if (!success) {
			source.sendError(new LiteralText("Failed to load asset."));
			return 0;
		}
		
		source.sendFeedback(new LiteralText("Playback of '" + assetUUID + "' started."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int stopPlayback(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, assetUUID);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		GSIPlaybackStream stream = ((GSIServerWorldAccess)world).gcp_getPlaybackStream(assetUUID);
		if (stream != null)
			stream.close();
		
		source.sendFeedback(new LiteralText("Playback of '" + assetUUID + "' stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	
	private static int stopAllPlayback(ServerCommandSource source) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, null);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_getPlaybackStreams().forEach(GSIPlaybackStream::close);
		
		source.sendFeedback(new LiteralText("All playbacks stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
