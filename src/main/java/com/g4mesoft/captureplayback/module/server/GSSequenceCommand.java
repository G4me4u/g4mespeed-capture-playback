package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSFileUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public final class GSSequenceCommand {

	private static final String SEQUENCE_EXTENSION = ".gsq";
	
	private GSSequenceCommand() {
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("sequence");
		
		command.then(CommandManager.literal("load").then(CommandManager.argument("fileName", StringArgumentType.word()).executes(context -> {
			return loadSequence(context.getSource(), StringArgumentType.getString(context, "fileName"));
		}))).then(CommandManager.literal("list").executes(context -> {
			return listSequences(context.getSource());
		}));
		
		dispatcher.register(command);
	}
	
	private static GSSequence readSequence(String fileName) {
		try {
			return GSFileUtil.readFile(getSequenceFile(fileName), GSSequence::read);
		} catch (IOException ignore) {
			return null;
		}
	}
	
	private static File getSequenceFile(String fileName) {
		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		return new File(module.getCompositionDirectory(), fileName + SEQUENCE_EXTENSION);
	}
	
	private static int loadSequence(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSSequence sequence = readSequence(fileName);

		if (sequence != null) {
			GSComposition composition = new GSComposition(UUID.randomUUID(), fileName);
			GSTrackGroup group = composition.addGroup("Group #1");
			GSTrack track = composition.addTrack("Sequence", 0xFFFFFFFF, group.getGroupUUID());
			track.getSequence().set(sequence);
			track.addEntry(0L);
			
			// Always save the composition before loading a new one.
			module.writeComposition();
			
			module.setComposition(composition, fileName);
			module.startSequenceSessionForAll(track.getTrackUUID());
			
			source.sendFeedback(Text.literal("Sequence '" + fileName + "' loaded successfully."), true);
		} else {
			source.sendError(Text.literal("Failed to load sequence."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int listSequences(ServerCommandSource source) {
		// TODO: definitely remove this

		GSCapturePlaybackExtension extension = GSCapturePlaybackExtension.getInstance();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		
		try {
			for (File file : module.getCompositionDirectory().listFiles()) {
				if (file.isFile()) {
					String fileName = file.getName();
					
					if (fileName.endsWith(SEQUENCE_EXTENSION)) {
						int len = fileName.length() - SEQUENCE_EXTENSION.length();
						String sequenceName = fileName.substring(0, len);
						
						Text sequenceNameText = Texts.bracketed(Text.literal(sequenceName).styled((style) -> {
							return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sequence load " + sequenceName))
									.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Load Sequence")))
									.withColor(Formatting.GREEN);
						}));
						
						source.sendFeedback(sequenceNameText, false);
					}
				}
			}
		} catch (SecurityException e) {
			source.sendError(Text.literal("Failed to list sequences"));
		}
		
		return Command.SINGLE_SUCCESS;
	}
}
