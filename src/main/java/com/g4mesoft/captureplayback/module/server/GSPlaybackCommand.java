package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public final class GSPlaybackCommand {

	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("playback");
		
		command.then(CommandManager.literal("start")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return startPlayback(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(CommandManager.literal("stop")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return stopPlayback(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(CommandManager.literal("stopAll")
			.executes(context -> {
				return stopAllPlaybacks(context.getSource());
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int startPlayback(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendError(new LiteralText("Asset does not exist."));
			return 0;
		}
		
		if (!info.getType().isStreamable()) {
			source.sendError(new LiteralText("Asset is not streamable."));
			return 0;
		}
		
		ServerWorld world = source.getWorld();
		if (((GSIServerWorldAccess)world).gcp_hasPlaybackStream(info.getAssetUUID())) {
			source.sendError(new LiteralText("Already playing back '" + handle + "'."));
			return 0;
		}
		
		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendError(new LiteralText("Failed to load asset."));
			return 0;
		}
		
		GSIPlaybackStream stream = ref.get().getPlaybackStream();
		stream.addCloseListener(ref::release);
		((GSIServerWorldAccess)world).gcp_addPlaybackStream(info.getAssetUUID(), stream);
		
		source.sendFeedback(new LiteralText("Playback of " + GSAssetCommand.toNameString(info) + " started."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int stopPlayback(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info == null) {
			source.sendError(new LiteralText("Asset with handle '" + handle + "' does not exist."));
			return 0;
		}
		
		ServerWorld world = source.getWorld();
		GSIPlaybackStream stream = ((GSIServerWorldAccess)world).gcp_getPlaybackStream(info.getAssetUUID());
		if (stream == null) {
			source.sendError(new LiteralText("No active playback found."));
			return 0;
		}
		
		stream.close();
		source.sendFeedback(new LiteralText("Playback of " + GSAssetCommand.toNameString(info) + " stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopAllPlaybacks(ServerCommandSource source) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, null);
		
		ServerWorld world = source.getWorld();
		((GSIServerWorldAccess)world).gcp_getPlaybackStreams().forEach(GSIPlaybackStream::close);
		
		source.sendFeedback(new LiteralText("All playbacks stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
