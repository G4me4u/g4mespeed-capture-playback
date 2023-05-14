package com.g4mesoft.captureplayback.module.server;

import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSEAssetNamespace;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

public class GSAssetHandleArgumentType {

	public static final SimpleCommandExceptionType INVALID_HANDLE =
			new SimpleCommandExceptionType(GSTextUtil.translatable("argument.assetHandle.invalid"));

	private GSAssetHandleArgumentType() {
	}
	
	public static GSAssetHandle getHandle(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
		Identifier id = IdentifierArgumentType.getIdentifier(context, name);
		// Convert into asset namespace
		GSEAssetNamespace namespace = null;
		if (id.getNamespace().length() == 1) {
			namespace = GSEAssetNamespace.fromIdentifier(
					id.getNamespace().charAt(0));
		}
		if (namespace != null) {
			try {
				return new GSAssetHandle(namespace, id.getPath());
			} catch (IllegalArgumentException e) {
				// Path is not base62 or underscore.
			}
		}
		throw INVALID_HANDLE.create();
	}

	public static IdentifierArgumentType handle() {
		return IdentifierArgumentType.identifier();
	}
}
