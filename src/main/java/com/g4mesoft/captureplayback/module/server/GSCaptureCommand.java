package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerLevelAccess;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;

public final class GSCaptureCommand {

	private GSCaptureCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("capture");
		
		command.then(Commands.literal("start")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return startCapture(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(Commands.literal("stop")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider()).executes(context -> {
					return stopCapture(context.getSource(), GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(Commands.literal("stopAll")
			.executes(context -> {
				return stopAllCaptures(context.getSource());
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int startCapture(CommandSourceStack source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendFailure(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}
		
		GSEAssetType type = info.getType();
		if (type == null) {
			source.sendFailure(GSTextUtil.literal("Unknown asset type."));
			return 0;
		}
		
		if (!type.isStreamable()) {
			source.sendFailure(GSTextUtil.literal("Asset is not streamable."));
			return 0;
		}
		
		ServerLevel world = source.getLevel();
		if (((GSIServerLevelAccess)world).gcp_hasCaptureStream(info.getAssetUUID())) {
			source.sendFailure(GSTextUtil.literal("Already capturing '" + handle + "'."));
			return 0;
		}
		
		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendFailure(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}
		
		GSICaptureStream stream = ref.get().getCaptureStream();
		stream.addCloseListener(ref::release);
		((GSIServerLevelAccess)world).gcp_addCaptureStream(info.getAssetUUID(), stream);

		source.sendSuccess(() -> GSTextUtil.literal("Capture of " + GSAssetCommand.toNameString(info) + " started."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopCapture(CommandSourceStack source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info == null) {
			source.sendFailure(GSTextUtil.literal("Asset with handle '" + handle + "' does not exist."));
			return 0;
		}
		
		ServerLevel world = source.getLevel();
		GSICaptureStream stream = ((GSIServerLevelAccess)world).gcp_getCaptureStream(info.getAssetUUID());
		if (stream == null) {
			source.sendFailure(GSTextUtil.literal("No active capture found."));
			return 0;
		}
		
		stream.close();
		source.sendSuccess(() -> GSTextUtil.literal("Capture of " + GSAssetCommand.toNameString(info) + " stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopAllCaptures(CommandSourceStack source) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, null);
		
		ServerLevel world = source.getLevel();
		((GSIServerLevelAccess)world).gcp_getCaptureStreams().forEach(GSICaptureStream::close);
		
		source.sendSuccess(() -> GSTextUtil.literal("All captures stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
