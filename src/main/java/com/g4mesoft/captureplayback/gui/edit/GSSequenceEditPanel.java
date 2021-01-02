package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.sequence.GSSequencePanel;
import com.g4mesoft.captureplayback.sequence.GSSequence;

public class GSSequenceEditPanel extends GSAbstractEditPanel {

	private final GSSequence sequence;
	
	public GSSequenceEditPanel(GSSequence sequence) {
		super(new GSSequencePanel(sequence, new GSDefaultChannelProvider()));
	
		this.sequence = sequence;
		
		nameField.setText(sequence.getName());
	}
	
	@Override
	protected void handleNameChanged(String name) {
		sequence.setName(name);
	}
}
