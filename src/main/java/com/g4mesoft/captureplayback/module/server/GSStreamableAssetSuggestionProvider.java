package com.g4mesoft.captureplayback.module.server;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.session.GSSession;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSStreamableAssetSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		// Iterate through active sessions
		GSSessionManager sessionManager = module.getSessionManager();
		GSIAssetHistory storedHistory = module.getAssetManager().getStoredHistory();
		Iterator<GSSession> itr = sessionManager.iterateSessions(player);
		while (itr.hasNext()) {
			GSSession session = itr.next();
			UUID assetUUID = session.get(GSSession.ASSET_UUID);
			GSAssetInfo info = storedHistory.get(assetUUID);
			if (info != null && info.getType() != null && info.getType().isStreamable())
				builder.suggest(info.getHandle().toString(), new LiteralMessage(info.getAssetName()));
		}
		return builder.buildFuture();
	}
}
