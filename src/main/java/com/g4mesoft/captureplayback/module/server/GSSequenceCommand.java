package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
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
		GSSequence sequence = null;
		
		try {
			sequence = readSequence(getSequenceFile(fileName));
		} catch (IOException ignore) {
		}
		
		return sequence;
	}
	
	private static GSSequence readSequence(File sequenceFile) throws IOException {
		GSSequence sequence;
		
		try (FileInputStream fis = new FileInputStream(sequenceFile)) {
			byte[] data = IOUtils.toByteArray(fis);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(data));
			sequence = GSSequence.read(buffer);
			buffer.release();
		} catch (Throwable throwable) {
			throw new IOException("Unable to read sequence", throwable);
		}
		
		return sequence;
	}

	private static File getSequenceFile(String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		return new File(module.getCompositionDirectory(), fileName + SEQUENCE_EXTENSION);
	}
	
	private static int loadSequence(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSSequence sequence = readSequence(fileName);

		if (sequence != null) {
			GSComposition composition = new GSComposition(UUID.randomUUID(), fileName);
			GSTrackGroup group = composition.addGroup("Group #1");
			GSTrack track = composition.addTrack(sequence.getSequenceUUID(), "Sequence", 0xFFFFFFFF, group.getGroupUUID());
			track.getSequence().set(sequence);
			module.setComposition(composition, fileName);
			
			source.sendFeedback(new LiteralText("Sequence '" + fileName + "' loaded successfully."), true);
		} else {
			source.sendError(new LiteralText("Failed to load sequence."));
		}

		return Command.SINGLE_SUCCESS;
	}
	
	private static int listSequences(ServerCommandSource source) {
		// TODO: definitely remove this

		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();
		
		try {
			for (File file : module.getCompositionDirectory().listFiles()) {
				if (file.isFile()) {
					String fileName = file.getName();
					
					if (fileName.endsWith(SEQUENCE_EXTENSION)) {
						int len = fileName.length() - SEQUENCE_EXTENSION.length();
						String sequenceName = fileName.substring(0, len);
						
						Text sequenceNameText = Texts.bracketed(new LiteralText(sequenceName).styled((style) -> {
							return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sequence load " + sequenceName))
									.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Load Sequence")))
									.withColor(Formatting.GREEN);
						}));
						
						source.sendFeedback(sequenceNameText, false);
					}
				}
			}
		} catch (SecurityException e) {
			source.sendError(new LiteralText("Failed to list sequences"));
		}
		
		return Command.SINGLE_SUCCESS;
	}
}
