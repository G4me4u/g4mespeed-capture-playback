package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerLevelAccess;
import com.g4mesoft.captureplayback.common.asset.GSAbstractAsset;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.stream.GSDelayedPlaybackStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;

public final class GSPlaybackCommand {

	private static final int REPEAT_FOREVER = -1;
	
	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("playback");
		
		command.then(Commands.literal("start")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return startPlayback(
						context.getSource(),
						GSAssetHandleArgumentType.getHandle(context, "handle"),
						0,
						1
					);
				})
			)
		).then(Commands.literal("repeat")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.then(Commands.argument("delay", IntegerArgumentType.integer(0))
					.executes(context -> {
						return startPlayback(
							context.getSource(),
							GSAssetHandleArgumentType.getHandle(context, "handle"),
							IntegerArgumentType.getInteger(context, "delay"),
							REPEAT_FOREVER
						);
					})
					.then(Commands.argument("count", IntegerArgumentType.integer(1))
						.executes(context -> {
							return startPlayback(
								context.getSource(),
								GSAssetHandleArgumentType.getHandle(context, "handle"),
								IntegerArgumentType.getInteger(context, "delay"),
								IntegerArgumentType.getInteger(context, "count")
							);
						})
					)
				)
			)
		).then(Commands.literal("stop")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return stopPlayback(
						context.getSource(),
						GSAssetHandleArgumentType.getHandle(context, "handle")
					);
				})
			)
		).then(Commands.literal("stopAll")
			.executes(context -> {
				return stopAllPlaybacks(context.getSource());
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int startPlayback(CommandSourceStack source, GSAssetHandle handle, int delay, int repeatCount) throws CommandSyntaxException {
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
		if (((GSIServerLevelAccess)world).gcp_hasPlaybackStream(info.getAssetUUID())) {
			source.sendFailure(GSTextUtil.literal("Already playing back '" + handle + "'."));
			return 0;
		}
		
		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendFailure(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}
		
		startPlaybackImpl(world, ref, delay, repeatCount, true);
		
		source.sendSuccess(() -> GSTextUtil.literal("Playback of " + GSAssetCommand.toNameString(info) + " started."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static void startPlaybackImpl(ServerLevel world, GSAssetRef ref, int delay, int repeatCount, boolean first) {
		if (repeatCount <= 0 && repeatCount != REPEAT_FOREVER) {
			// Conservative check if we have repeated all
			ref.release();
			return;
		}
		GSAbstractAsset asset = ref.get();
		GSIPlaybackStream stream = first ? asset.getPlaybackStream() :
			new GSDelayedPlaybackStream(asset.getPlaybackStream(), delay);
		stream.addCloseListener(() -> {
			if (stream.isForceClosed()) {
				// Forcibly closed with /playback stop
				ref.release();
			} else {
				if (repeatCount == REPEAT_FOREVER) {
					startPlaybackImpl(world, ref, delay, REPEAT_FOREVER, false);
				} else {
					//assert(repeatCount > 0)
					startPlaybackImpl(world, ref, delay, repeatCount - 1, false);
				}
			}
		});
		((GSIServerLevelAccess)world).gcp_addPlaybackStream(asset.getUUID(), stream);
	}

	private static int stopPlayback(CommandSourceStack source, GSAssetHandle handle) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, handle);
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info == null) {
			source.sendFailure(GSTextUtil.literal("Asset with handle '" + handle + "' does not exist."));
			return 0;
		}
		
		ServerLevel world = source.getLevel();
		GSIPlaybackStream stream = ((GSIServerLevelAccess)world).gcp_getPlaybackStream(info.getAssetUUID());
		if (stream == null) {
			source.sendFailure(GSTextUtil.literal("No active playback found."));
			return 0;
		}
		
		stream.close();
		source.sendSuccess(() -> GSTextUtil.literal("Playback of " + GSAssetCommand.toNameString(info) + " stopped."), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int stopAllPlaybacks(CommandSourceStack source) throws CommandSyntaxException {
		GSAssetCommand.checkPermission(source, null);
		
		ServerLevel world = source.getLevel();
		((GSIServerLevelAccess)world).gcp_getPlaybackStreams().forEach(GSIPlaybackStream::close);
		
		source.sendSuccess(() -> GSTextUtil.literal("All playbacks stopped."), true);

		return Command.SINGLE_SUCCESS;
	}
}
