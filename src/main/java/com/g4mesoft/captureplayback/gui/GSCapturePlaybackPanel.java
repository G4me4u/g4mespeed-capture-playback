package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.legend.GSButtonPanel;
import com.g4mesoft.renderer.GSTexture;

import net.minecraft.util.Identifier;

public class GSCapturePlaybackPanel extends GSParentPanel {

	/* Icon sheet used for Capture & Playback UI elements. */
	private static final Identifier ICONS_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);
	
	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editCompositionButton;

	public GSCapturePlaybackPanel(GSCapturePlaybackClientModule module) {
		editCompositionButton = new GSButtonPanel("Edit Composition", () -> {
			module.requestSession(GSESessionType.COMPOSITION, GSESessionRequestType.REQUEST_START, UUID.randomUUID());
		});
		
		add(editCompositionButton);
	}
	
	@Override
	protected void layout() {
		editCompositionButton.setPreferredBounds(width / 2 - 45, TOP_MARGIN, 90);
	}
}
