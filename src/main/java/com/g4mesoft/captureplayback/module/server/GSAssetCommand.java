package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.common.asset.GSEAssetNamespace;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class GSAssetCommand {

    private static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION_EXCEPTION =
    		new SimpleCommandExceptionType(GSTextUtil.translatable("command.assetCommands.insufficientPermission"));
	
	private GSAssetCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, GSEAssetType assetType) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal(assetType.getName());
		
		LiteralArgumentBuilder<ServerCommandSource> newCommand = CommandManager.literal("new");
		for (GSEAssetNamespace namespace : GSEAssetNamespace.values()) {
			newCommand.then(CommandManager.literal(namespace.getName())
				.then(CommandManager.argument("assetName", StringArgumentType.greedyString())
					.executes(context -> {
						return createAsset(context.getSource(), assetType, namespace, StringArgumentType.getString(context, "assetName"));
					})
				)
			);
		}
		command.then(newCommand);
		
		command.then(CommandManager.literal("edit")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSAssetSuggestionProvider(assetType))
				.executes(context -> {
					return editAsset(context.getSource(), assetType, GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(CommandManager.literal("move")
			.then(CommandManager.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSAssetSuggestionProvider(assetType))
				.then(CommandManager.literal("relative")
					.then(CommandManager.argument("dx", IntegerArgumentType.integer())
						.then(CommandManager.argument("dy", IntegerArgumentType.integer())
							.then(CommandManager.argument("dz", IntegerArgumentType.integer())
								.executes(context -> {
									GSAssetHandle handle = GSAssetHandleArgumentType.getHandle(context, "handle");
									int dx = IntegerArgumentType.getInteger(context, "dx");
									int dy = IntegerArgumentType.getInteger(context, "dy");
									int dz = IntegerArgumentType.getInteger(context, "dz");
									return moveAssetRelative(context.getSource(), assetType, handle, dx, dy, dz);
								})
							)
						)
					)
				)
				.then(CommandManager.literal("absolute")
					.then(CommandManager.argument("newOrigin", BlockPosArgumentType.blockPos())
						.executes(context -> {
							GSAssetHandle handle = GSAssetHandleArgumentType.getHandle(context, "handle");
							BlockPos newOrigin = BlockPosArgumentType.getBlockPos(context, "newOrigin");
							return moveAssetAbsolute(context.getSource(), assetType, handle, newOrigin);
						})
					)
				)
			)
		).then(CommandManager.literal("list")
			.executes(context -> {
				return listAssets(context.getSource(), assetType);
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int createAsset(ServerCommandSource source, GSEAssetType assetType, GSEAssetNamespace namespace, String assetName) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();

		assetManager.createAsset(assetType, namespace, assetName, player.getUuid());

		source.sendFeedback(() -> GSTextUtil.literal("Asset '" + assetName + "' created successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int editAsset(ServerCommandSource source, GSEAssetType assetType, GSAssetHandle handle) throws CommandSyntaxException {
		checkPermission(source, handle);

		ServerPlayerEntity player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info != null && info.getTypeIndex() == assetType.getIndex() && module.onSessionRequest(player, GSESessionRequestType.REQUEST_START, info.getAssetUUID())) {
			source.sendFeedback(() -> GSTextUtil.literal("Session of " + toNameString(info) + " started."), false);
		} else {
			source.sendError(GSTextUtil.literal("Failed to edit " + assetType.getName() + "."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int moveAssetRelative(ServerCommandSource source, GSEAssetType assetType, GSAssetHandle handle, int dx, int dy, int dz) throws CommandSyntaxException {
		checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendError(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}

		if (info.getTypeIndex() != assetType.getIndex()) {
			source.sendError(GSTextUtil.literal("Asset is not a " + assetType.getName() + "."));
			return 0;
		}
		
		// Note: info.getType() will never return null due to check above.
		if (!info.getType().hasOrigin()) {
			source.sendError(GSTextUtil.literal("Asset is not movable."));
			return 0;
		}

		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendError(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}
		
		ref.get().offsetOrigin(dx, dy, dz);
		ref.release();

		source.sendFeedback(() -> GSTextUtil.literal("Moved " + toNameString(info) + " by " + dx + ", " + dy + ", " + dz + " successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int moveAssetAbsolute(ServerCommandSource source, GSEAssetType assetType, GSAssetHandle handle, BlockPos newOrigin) throws CommandSyntaxException {
		checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendError(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}

		if (info.getTypeIndex() != assetType.getIndex()) {
			source.sendError(GSTextUtil.literal("Asset is not a " + assetType.getName() + "."));
			return 0;
		}
		
		// Note: info.getType() will never return null due to check above.
		if (!info.getType().hasOrigin()) {
			source.sendError(GSTextUtil.literal("Asset does not have an origin."));
			return 0;
		}

		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendError(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}

		BlockPos currOrigin = ref.get().getOrigin();
		int dx = newOrigin.getX() - currOrigin.getX();
		int dy = newOrigin.getY() - currOrigin.getY();
		int dz = newOrigin.getZ() - currOrigin.getZ();

		ref.get().offsetOrigin(dx, dy, dz);
		ref.release();

		String newOriginStr = "(" + newOrigin.getX() + ", " + newOrigin.getY() + ", " + newOrigin.getZ() + ")";
		source.sendFeedback(() -> GSTextUtil.literal("Moved " + toNameString(info) + " origin to " + newOriginStr + " successfully."), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int listAssets(ServerCommandSource source, GSEAssetType assetType) {
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		
		String commandPrefix = "/" + assetType.getName() + " edit ";
		Text hintText = GSTextUtil.literal("Edit " + assetType.getName());
		for (GSAssetInfo info : module.getAssetManager().getStoredHistory()) {
			if (info.getTypeIndex() == assetType.getIndex()) {
				source.sendFeedback(() -> Texts.bracketed(GSTextUtil.literal(info.getAssetName()).styled((style) -> {
					return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandPrefix + info.getHandle()))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hintText))
							.withColor(Formatting.GREEN);
				})), false);
			}
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	public static void checkPermission(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		if (!hasPermission(source, handle))
			throw INSUFFICIENT_PERMISSION_EXCEPTION.create();
	}
	
	public static boolean hasPermission(ServerCommandSource source, GSAssetHandle handle) throws CommandSyntaxException {
		if (source.hasPermissionLevel(GSServerController.OP_PERMISSION_LEVEL)) {
			// Contexts regarding OP players or command blocks etc. have access to all assets
			return true;
		}
		if (handle == null) {
			// Only contexts with OP have access to all assets...
			return false;
		}
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		return module.getAssetManager().hasPermission(source.getPlayer(), handle);
	}

	public static String toNameString(GSAssetInfo info) {
		// Note: Users can 'inject' chat formatting into the
		//       asset name. Reset here to handle that case,
		//       so it looks intentional...
		return "'" + info.getAssetName() + "\u00A7r' (" + info.getHandle() + ")";
	}
}
