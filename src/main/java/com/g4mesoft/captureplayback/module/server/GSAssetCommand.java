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

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public class GSAssetCommand {

    private static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION_EXCEPTION =
    		new SimpleCommandExceptionType(GSTextUtil.translatable("command.assetCommands.insufficientPermission"));
	
	private GSAssetCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, GSEAssetType assetType) {
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(assetType.getName());
		
		LiteralArgumentBuilder<CommandSourceStack> newCommand = Commands.literal("new");
		for (GSEAssetNamespace namespace : GSEAssetNamespace.values()) {
			newCommand.then(Commands.literal(namespace.getName())
				.then(Commands.argument("assetName", StringArgumentType.greedyString())
					.executes(context -> {
						return createAsset(context.getSource(), assetType, namespace, StringArgumentType.getString(context, "assetName"));
					})
				)
			);
		}
		command.then(newCommand);
		
		command.then(Commands.literal("edit")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSAssetSuggestionProvider(assetType))
				.executes(context -> {
					return editAsset(context.getSource(), assetType, GSAssetHandleArgumentType.getHandle(context, "handle"));
				})
			)
		).then(Commands.literal("move")
			.then(Commands.argument("handle", GSAssetHandleArgumentType.handle())
				.suggests(new GSAssetSuggestionProvider(assetType))
				.then(Commands.literal("relative")
					.then(Commands.argument("dx", IntegerArgumentType.integer())
						.then(Commands.argument("dy", IntegerArgumentType.integer())
							.then(Commands.argument("dz", IntegerArgumentType.integer())
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
				.then(Commands.literal("absolute")
					.then(Commands.argument("newOrigin", BlockPosArgument.blockPos())
						.executes(context -> {
							GSAssetHandle handle = GSAssetHandleArgumentType.getHandle(context, "handle");
							BlockPos newOrigin = BlockPosArgument.getBlockPos(context, "newOrigin");
							return moveAssetAbsolute(context.getSource(), assetType, handle, newOrigin);
						})
					)
				)
			)
		).then(Commands.literal("list")
			.executes(context -> {
				return listAssets(context.getSource(), assetType);
			})
		);
		
		dispatcher.register(command);
	}
	
	private static int createAsset(CommandSourceStack source, GSEAssetType assetType, GSEAssetNamespace namespace, String assetName) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();

		assetManager.createAsset(assetType, namespace, assetName, player.getUUID());

		source.sendSuccess(() -> GSTextUtil.literal("Asset '" + assetName + "' created successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int editAsset(CommandSourceStack source, GSEAssetType assetType, GSAssetHandle handle) throws CommandSyntaxException {
		checkPermission(source, handle);

		ServerPlayer player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info != null && info.getTypeIndex() == assetType.getIndex() && module.onSessionRequest(player, GSESessionRequestType.REQUEST_START, info.getAssetUUID())) {
			source.sendSuccess(() -> GSTextUtil.literal("Session of " + toNameString(info) + " started."), false);
		} else {
			source.sendFailure(GSTextUtil.literal("Failed to edit " + assetType.getName() + "."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int moveAssetRelative(CommandSourceStack source, GSEAssetType assetType, GSAssetHandle handle, int dx, int dy, int dz) throws CommandSyntaxException {
		checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendFailure(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}

		if (info.getTypeIndex() != assetType.getIndex()) {
			source.sendFailure(GSTextUtil.literal("Asset is not a " + assetType.getName() + "."));
			return 0;
		}
		
		// Note: info.getType() will never return null due to check above.
		if (!info.getType().hasOrigin()) {
			source.sendFailure(GSTextUtil.literal("Asset is not movable."));
			return 0;
		}

		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendFailure(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}
		
		ref.get().offsetOrigin(dx, dy, dz);
		ref.release();

		source.sendSuccess(() -> GSTextUtil.literal("Moved " + toNameString(info) + " by " + dx + ", " + dy + ", " + dz + " successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int moveAssetAbsolute(CommandSourceStack source, GSEAssetType assetType, GSAssetHandle handle, BlockPos newOrigin) throws CommandSyntaxException {
		checkPermission(source, handle);

		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetManager assetManager = module.getAssetManager();
		GSAssetInfo info = assetManager.getInfoFromHandle(handle);

		if (info == null) {
			source.sendFailure(GSTextUtil.literal("Asset does not exist."));
			return 0;
		}

		if (info.getTypeIndex() != assetType.getIndex()) {
			source.sendFailure(GSTextUtil.literal("Asset is not a " + assetType.getName() + "."));
			return 0;
		}
		
		// Note: info.getType() will never return null due to check above.
		if (!info.getType().hasOrigin()) {
			source.sendFailure(GSTextUtil.literal("Asset does not have an origin."));
			return 0;
		}

		GSAssetRef ref = assetManager.requestAsset(info.getAssetUUID());
		if (ref == null) {
			source.sendFailure(GSTextUtil.literal("Failed to load asset."));
			return 0;
		}

		BlockPos currOrigin = ref.get().getOrigin();
		int dx = newOrigin.getX() - currOrigin.getX();
		int dy = newOrigin.getY() - currOrigin.getY();
		int dz = newOrigin.getZ() - currOrigin.getZ();

		ref.get().offsetOrigin(dx, dy, dz);
		ref.release();

		String newOriginStr = "(" + newOrigin.getX() + ", " + newOrigin.getY() + ", " + newOrigin.getZ() + ")";
		source.sendSuccess(() -> GSTextUtil.literal("Moved " + toNameString(info) + " origin to " + newOriginStr + " successfully."), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int listAssets(CommandSourceStack source, GSEAssetType assetType) {
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		
		String commandPrefix = "/" + assetType.getName() + " edit ";
		Component hintText = GSTextUtil.literal("Edit " + assetType.getName());
		for (GSAssetInfo info : module.getAssetManager().getStoredHistory()) {
			if (info.getTypeIndex() == assetType.getIndex()) {
				source.sendSuccess(() -> ComponentUtils.wrapInSquareBrackets(GSTextUtil.literal(info.getAssetName()).withStyle((style) -> {
					return style.withClickEvent(new ClickEvent.SuggestCommand(commandPrefix + info.getHandle()))
							.withHoverEvent(new HoverEvent.ShowText(hintText))
							.withColor(ChatFormatting.GREEN);
				})), false);
			}
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	public static void checkPermission(CommandSourceStack source, GSAssetHandle handle) throws CommandSyntaxException {
		if (!hasPermission(source, handle))
			throw INSUFFICIENT_PERMISSION_EXCEPTION.create();
	}
	
	public static boolean hasPermission(CommandSourceStack source, GSAssetHandle handle) throws CommandSyntaxException {
		if (source.permissions().hasPermission(GSServerController.OP_PERMISSION)) {
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
