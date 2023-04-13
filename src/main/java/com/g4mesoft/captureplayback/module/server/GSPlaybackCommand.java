package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;
import com.g4mesoft.captureplayback.common.asset.GSAbstractAsset;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.stream.GSDelayedPlaybackStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public final class GSPlaybackCommand {

	private static final int REPEAT_FOREVER = -1;
	
	private GSPlaybackCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("playback");
		
		command.then(CommandManager.literal("start")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
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
		).then(CommandManager.literal("repeat")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
					.executes(context -> {
						return startPlayback(
							context.getSource(),
							GSAssetHandleArgumentType.getHandle(context, "handle"),
							IntegerArgumentType.getInteger(context, "delay"),
							REPEAT_FOREVER
						);
					})
					.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
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
		).then(CommandManager.literal("stop")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSStreamableAssetSuggestionProvider())
				.executes(context -> {
					return stopPlayback(
						context.getSource(),
						GSAssetHandleArgumentType.getHandle(context, "handle")
					);
				})
			)
		).then(CommandManager.literal("stopAll")
			.executes(context -> {
				return stopAllPlaybacks(context.getSource());
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int startPlayback(ServerCommandSource source, GSAssetHandle handle, int delay, int repeatCount) throws CommandSyntaxException {
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
		
		startPlaybackImpl(world, ref, delay, repeatCount, true);
		
		source.sendFeedback(new LiteralText("Playback of " + GSAssetCommand.toNameString(info) + " started."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static void startPlaybackImpl(ServerWorld world, GSAssetRef ref, int delay, int repeatCount, boolean first) {
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
		((GSIServerWorldAccess)world).gcp_addPlaybackStream(asset.getUUID(), stream);
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
