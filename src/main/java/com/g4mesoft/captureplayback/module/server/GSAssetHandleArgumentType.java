package com.g4mesoft.captureplayback.module.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.ui.util.GSTextUtil;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.server.command.ServerCommandSource;

public class GSAssetHandleArgumentType implements ArgumentType<GSAssetHandle> {

	public static final SimpleCommandExceptionType INVALID_HANDLE =
			new SimpleCommandExceptionType(GSTextUtil.translatable("argument.assetHandle.invalid"));
	private static final Collection<String> EXAMPLES = Arrays.asList("g:my_composition", "w:best_door", "g:small_0_6s_5x5");
	private static final Pattern VALID_CHARACTERS = Pattern.compile("^([wg]:[0-9A-Za-z_]+)");

	public static GSAssetHandle getHandle(CommandContext<ServerCommandSource> context, String name) {
		return context.getArgument(name, GSAssetHandle.class);
	}

	public static GSAssetHandleArgumentType handle() {
		return new GSAssetHandleArgumentType();
	}

	@Override
	public GSAssetHandle parse(StringReader reader) throws CommandSyntaxException {
		String remaining = reader.getRemaining();
		Matcher matcher = VALID_CHARACTERS.matcher(remaining);
		if (matcher.find()) {
			String identifier = matcher.group(1);
			try {
				GSAssetHandle handle = GSAssetHandle.fromString(identifier);
				reader.setCursor(reader.getCursor() + identifier.length());
				return handle;
			} catch (IllegalArgumentException ignore) {
				// handled below.
			}
		}
		throw INVALID_HANDLE.create();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
