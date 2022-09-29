package com.g4mesoft.captureplayback.module.server;

import java.util.UUID;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.asset.GSAbstractAsset;
import com.g4mesoft.captureplayback.common.asset.GSAssetStorage;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public final class GSCaptureCommand {

	private GSCaptureCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("capture");
		
		command.then(CommandManager.literal("start").then(CommandManager.argument("assetUUID", UuidArgumentType.uuid()).suggests(new GSAssetSuggestionProvider()).executes(context -> {
			return startCapture(context.getSource(), UuidArgumentType.getUuid(context, "assetUUID"));
		}))).then(CommandManager.literal("stop").then(CommandManager.argument("assetUUID", UuidArgumentType.uuid()).suggests(new GSAssetSuggestionProvider()).executes(context -> {
			return stopCapture(context.getSource(), UuidArgumentType.getUuid(context, "assetUUID"));
		}))).then(CommandManager.literal("stopAll").executes(context -> {
			return stopAllCaptures(context.getSource());
		}));
		
		dispatcher.register(command);
	}
	
	private static int startCapture(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, assetUUID);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		if (((GSIServerWorldAccess)world).gcp_hasCaptureStream(assetUUID)) {
			source.sendError(new LiteralText("Already capturing '" + assetUUID + "'."));
			return 0;
		}
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetStorage assetStorage = module.getAssetStorage();
		
		if (!assetStorage.isLoaded(assetUUID)) {
			source.sendError(new LiteralText("Asset is not loaded."));
			return 0;
		}
		
		GSAbstractAsset asset = assetStorage.isLoaded(assetUUID) ? assetStorage.requestAsset(assetUUID) : null;
		if (asset != null) {
			((GSIServerWorldAccess)world).gcp_addCaptureStream(assetUUID, asset.getCaptureStream());
			source.sendFeedback(new LiteralText("Capture of '" + assetUUID + "' started."), true);
			return Command.SINGLE_SUCCESS;
		}
		
		source.sendError(new LiteralText("Failed to load asset."));
		return 0;
	}
	
	private static int stopCapture(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, assetUUID);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		GSICaptureStream stream = ((GSIServerWorldAccess)world).gcp_getCaptureStream(assetUUID);
		if (stream != null)
			stream.close();
		
		source.sendFeedback(new LiteralText("Capture of '" + assetUUID + "' stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	
	private static int stopAllCaptures(ServerCommandSource source) throws CommandSyntaxException {
		GSAssetCommands.checkPermission(source, null);
		
		ServerWorld world = source.getMinecraftServer().getOverworld();
		((GSIServerWorldAccess)world).gcp_getCaptureStreams().forEach(GSICaptureStream::close);
		
		source.sendFeedback(new LiteralText("All captures stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
