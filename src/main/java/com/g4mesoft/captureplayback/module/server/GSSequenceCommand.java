package com.g4mesoft.captureplayback.module.server;

public final class GSSequenceCommand {

	private GSSequenceCommand() {
	}
	
	/*
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("sequence");
		
		command.then(CommandManager.literal("save").then(CommandManager.argument("fileName", StringArgumentType.word()).executes(context -> {
			return saveSequence(context.getSource(), StringArgumentType.getString(context, "fileName"));
		}))).then(CommandManager.literal("load").then(CommandManager.argument("fileName", StringArgumentType.word()).executes(context -> {
			return loadSequence(context.getSource(), StringArgumentType.getString(context, "fileName"));
		}))).then(CommandManager.literal("list").executes(context -> {
			return listSequences(context.getSource());
		}));
		
		dispatcher.register(command);
	}
	
	private static int saveSequence(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSSequence sequence = module.getActiveSequence();
		
		if (module.writeSequence(sequence, fileName)) {
			source.sendFeedback(new LiteralText("Sequence '" + fileName + "' saved successfully."), true);
		} else {
			source.sendError(new LiteralText("Failed to save sequence."));
		}
		
		return Command.SINGLE_SUCCESS;
	}

	private static int loadSequence(ServerCommandSource source, String fileName) {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSCapturePlaybackServerModule module = extension.getServerModule();

		GSSequence sequence = module.readSequence(fileName);

		if (sequence != null) {
			module.setPrimarySequence(sequence);
			
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
					
					if (fileName.endsWith(GSCapturePlaybackServerModule.COMPOSITION_EXTENSION)) {
						int len = fileName.length() - GSCapturePlaybackServerModule.COMPOSITION_EXTENSION.length();
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
	}*/
}
