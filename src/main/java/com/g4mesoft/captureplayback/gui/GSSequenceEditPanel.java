package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.captureplayback.panel.sequence.GSSequencePanel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanel;

public class GSSequenceEditPanel extends GSAbstractEditPanel {

	private final GSSequence sequence;
	private final GSPanel contentPanel;
	
	public GSSequenceEditPanel(GSSequence sequence) {
		this.sequence = sequence;
	
		contentPanel = new GSSequencePanel(getSequenceSession(), new GSDefaultChannelProvider());
		add(contentPanel);
		
		nameField.setText(sequence.getName());
	}
	
	@Override
	public void onAdded(GSPanel parent) {
		super.onAdded(parent);

		contentPanel.requestFocus();
	}
	
	@Override
	protected void handleNameChanged(String name) {
		sequence.setName(name);
	}
	
	@Override
	protected void layoutContent(int x, int y, int width, int height) {
		contentPanel.setBounds(x, y, width, height);
	}
	
	private GSSequenceSession getSequenceSession() {
		return CapturePlaybackMod.getInstance().getExtension().getClientModule().getSequenceSession();
	}
}
