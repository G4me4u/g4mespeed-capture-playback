package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

public final class GSCaptureCommand {

	private GSCaptureCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("capture");
		
		command.then(CommandManager.literal("start")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return startCapture(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(CommandManager.literal("stop")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider()).executes(context -> {
					return stopCapture(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(CommandManager.literal("stopAll")
			.executes(context -> {
				return stopAllCaptures(context.getSource());
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int startCapture(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendError(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}
		
		if (!info.getType().isStreamable()) {
			source.sendError(GSTextUtil.literal("Asset is not streamable."));
			return 0;
		}
		
		ServerWorld world = source.getWorld();
		if (((GSIServerWorldAccess)world).gcp_hasCaptureStream(info.getAssetUUID())) {
			source.sendError(GSTextUtil.literal("Already capturing '" + handle + "'."));
			return 0;
		}
		
		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendError(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}
		
		GSICaptureStream stream = ref.get().getCaptureStream();
		stream.addCloseListener(ref::release);
		((GSIServerWorldAccess)world).gcp_addCaptureStream(info.getAssetUUID(), stream);

		source.sendFeedback(() -> GSTextUtil.literal("Capture of " + GSAssetCommand.toNameString(info) + " started."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopCapture(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info == null) {
			source.sendError(GSTextUtil.literal("Asset with handle '" + handle + "' does not exist."));
			return 0;
		}
		
		ServerWorld world = source.getWorld();
		GSICaptureStream stream = ((GSIServerWorldAccess)world).gcp_getCaptureStream(info.getAssetUUID());
		if (stream == null) {
			source.sendError(GSTextUtil.literal("No active capture found."));
			return 0;
		}
		
		stream.close();
		source.sendFeedback(() -> GSTextUtil.literal("Capture of " + GSAssetCommand.toNameString(info) + " stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopAllCaptures(ServerCommandSource source) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, null);
		
		ServerWorld world = source.getWorld();
		((GSIServerWorldAccess)world).gcp_getCaptureStreams().forEach(GSICaptureStream::close);
		
		source.sendFeedback(() -> GSTextUtil.literal("All captures stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
