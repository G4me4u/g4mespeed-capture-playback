package com.g4mesoft.captureplayback.module.server;

import java.util.concurrent.CompletableFuture;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

	private final GSEAssetType assetType;
	
	public GSAssetSuggestionProvider() {
		this(null);
	}

	public GSAssetSuggestionProvider(GSEAssetType assetType) {
		this.assetType = assetType;
	}
	
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		for (GSAssetInfo info : module.getAssetManager().getStoredHistory()) {
			if (info.hasPermission(player) && (assetType == null || info.getType() == assetType))
				builder.suggest(info.getHandle().toString(), new LiteralMessage(info.getAssetName()));
		}
		return builder.buildFuture();
	}
}
