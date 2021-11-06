package com.g4mesoft.captureplayback.module.server;

import java.io.File;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public final class GSCompositionCommand {

	private GSCompositionCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("composition");
		
		command.then(CommandManager.literal("save").executes(context -> {
			return saveComposition(context.getSource());
		})).then(CommandManager.literal("new").then(CommandManager.argument("fileName", StringArgumentType.word()).executes(context -> {
			return newComposition(context.getSource(), StringArgumentType.getString(context, "fileName"));
		}))).then(CommandManager.literal("load").then(CommandManager.argument("fileName", StringArgumentType.word()).executes(context -> {
			return loadComposition(context.getSource(), StringArgumentType.getString(context, "fileName"));
		}))).then(CommandManager.literal("list").executes(context -> {
			return listCompositions(context.getSource());
		}));
		
		dispatcher.register(command);
	}
	
	private static int saveComposition(ServerCommandSource source) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		if (module.writeComposition()) {
			source.sendFeedback(new LiteralText("Composition saved successfully."), true);
		} else {
			source.sendError(new LiteralText("Failed to save composition."));
		}
		
		return Command.SINGLE_SUCCESS;
	}

	private static int newComposition(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		// Always save the composition before loading a new one.
		module.writeComposition();
		
		if (module.createComposition(fileName)) {
			source.sendFeedback(new LiteralText("Composition '" + fileName + "' created successfully."), true);
		} else {
			source.sendError(new LiteralText("Failed to load composition."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int loadComposition(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		
		// Always save the composition before loading a new one.
		module.writeComposition();
				
		if (module.setComposition(fileName)) {
			source.sendFeedback(new LiteralText("Composition '" + fileName + "' loaded successfully."), true);
		} else {
			source.sendError(new LiteralText("Failed to load composition."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int listCompositions(ServerCommandSource source) {
		// TODO: definitely remove this

		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		
		try {
			for (File file : module.getCompositionDirectory().listFiles()) {
				if (file.isFile()) {
					String fileName = file.getName();
					
					if (fileName.endsWith(GSCapturePlaybackServerModule.COMPOSITION_EXTENSION)) {
						int len = fileName.length() - GSCapturePlaybackServerModule.COMPOSITION_EXTENSION.length();
						String compositionName = fileName.substring(0, len);
						
						Text compositionNameText = Texts.bracketed(new LiteralText(compositionName).styled((style) -> {
							return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/composition load " + compositionName))
									.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Load Composition")))
									.withColor(Formatting.GREEN);
						}));
						
						source.sendFeedback(compositionNameText, false);
					}
				}
			}
		} catch (SecurityException e) {
			source.sendError(new LiteralText("Failed to list compositions"));
		}
		
		return Command.SINGLE_SUCCESS;
	}
}
