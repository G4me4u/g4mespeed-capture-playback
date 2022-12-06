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
		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		for (GSAssetInfo info : module.getAssetStorage().getStoredHistory()) {
			if (assetType == null || info.getType() == assetType)
				builder.suggest(info.getAssetUUID().toString(), new LiteralMessage(info.getAssetName()));
		}
		return builder.buildFuture();
	}
}
