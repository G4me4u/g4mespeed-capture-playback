package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSEAssetNamespace;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.core.server.GSServerController;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public class GSAssetCommand {

    private static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("command.assetCommands.insufficientPermission"));
	
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

		source.sendFeedback(Text.literal("Asset '" + assetName + "' created successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int editAsset(ServerCommandSource source, GSEAssetType assetType, GSAssetHandle handle) throws CommandSyntaxException {
		checkPermission(source, handle);

		ServerPlayerEntity player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetManager().getInfoFromHandle(handle);
		
		if (info != null && info.getType() == assetType && module.onSessionRequest(player, GSESessionRequestType.REQUEST_START, info.getAssetUUID())) {
			source.sendFeedback(Text.literal("Session of " + toNameString(info) + " started."), false);
		} else {
			source.sendError(Text.literal("Failed to edit asset."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int listAssets(ServerCommandSource source, GSEAssetType assetType) {
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		
		String commandPrefix = "/" + assetType.getName() + " edit ";
		Text hintText = Text.literal("Edit " + assetType.getName());
		for (GSAssetInfo info : module.getAssetManager().getStoredHistory()) {
			if (info.getType() == assetType) {
				source.sendFeedback(Texts.bracketed(Text.literal(info.getAssetName()).styled((style) -> {
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
		return "'" + info.getAssetName() + "' (" + info.getHandle() + ")";
	}
}
