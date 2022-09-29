package com.g4mesoft.captureplayback.module.server;

import java.util.UUID;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetStorage;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.core.server.GSServerController;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class GSAssetCommands {

    private static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("command.assetCommands.insufficientPermission"));
	
	private GSAssetCommands() {
	}
	
	protected static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		for (GSEAssetType assetType : GSEAssetType.values()) {
			LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal(assetType.getName());
			
			command.then(CommandManager.literal("new").then(CommandManager.argument("assetName", StringArgumentType.greedyString()).executes(context -> {
				return createAsset(context.getSource(), assetType, StringArgumentType.getString(context, "assetName"));
			}))).then(CommandManager.literal("edit").then(CommandManager.argument("assetUUID", UuidArgumentType.uuid()).suggests(new GSAssetSuggestionProvider(assetType)).executes(context -> {
				return editAsset(context.getSource(), assetType, UuidArgumentType.getUuid(context, "assetUUID"));
			}))).then(CommandManager.literal("list").executes(context -> {
				return listAssets(context.getSource(), assetType);
			}));
			
			dispatcher.register(command);
		}
	}
	
	private static int createAsset(ServerCommandSource source, GSEAssetType assetType, String assetName) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetStorage assetStorage = module.getAssetStorage();

		assetStorage.createAsset(new GSAssetInfo(
			assetType, 
			GSUUIDUtil.randomUnique(assetStorage::hasAsset),
			assetName,
			System.currentTimeMillis(),
			player.getUuid()
		));

		source.sendFeedback(new LiteralText("Asset '" + assetName + "' created successfully."), false);
		
		return Command.SINGLE_SUCCESS;
	}

	private static int editAsset(ServerCommandSource source, GSEAssetType assetType, UUID assetUUID) throws CommandSyntaxException {
		checkPermission(source, assetUUID);

		ServerPlayerEntity player = source.getPlayer();
		
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		GSAssetInfo info = module.getAssetStorage().getInfo(assetUUID);
		
		if (info != null && info.getType() == assetType && module.onSessionRequest(player, GSESessionRequestType.REQUEST_START, assetUUID)) {
			source.sendFeedback(new LiteralText("Session of '" + assetUUID + "' started."), false);
		} else {
			source.sendError(new LiteralText("Failed to edit asset."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int listAssets(ServerCommandSource source, GSEAssetType assetType) {
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		
		String commandPrefix = "/" + assetType.getName() + " edit ";
		Text hintText = new LiteralText("Edit " + assetType.getName());
		for (GSAssetInfo info : module.getAssetStorage().getStoredInfoSet()) {
			if (info.getType() == assetType) {
				source.sendFeedback(Texts.bracketed(new LiteralText(info.getAssetName()).styled((style) -> {
					return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandPrefix + info.getAssetUUID()))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hintText))
							.withColor(Formatting.GREEN);
				})), false);
			}
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	public static void checkPermission(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		if (!hasPermission(source, assetUUID))
			throw INSUFFICIENT_PERMISSION_EXCEPTION.create();
	}
	
	public static boolean hasPermission(ServerCommandSource source, UUID assetUUID) throws CommandSyntaxException {
		if (source.hasPermissionLevel(GSServerController.OP_PERMISSION_LEVEL)) {
			// Contexts regarding OP players or command blocks etc. have access to all assets
			return true;
		}
		if (assetUUID == null) {
			// Only contexts with OP have access to all assets...
			return false;
		}
		GSCapturePlaybackServerModule module = GSCapturePlaybackExtension.getInstance().getServerModule();
		return module.getAssetStorage().hasPermission(source.getPlayer(), assetUUID);
	}
}
