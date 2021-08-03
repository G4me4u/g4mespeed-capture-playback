package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.panel.sequence.GSSequencePanel;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.panel.GSPanel;

public class GSSequenceEditPanel extends GSAbstractEditPanel implements GSISequenceListener {

	private final GSSequence sequence;
	
	private final GSPanel contentPanel;

	public GSSequenceEditPanel(GSSession session) {
		if (session.getType() != GSESessionType.SEQUENCE)
			throw new IllegalArgumentException("Session is not of type sequence");
		
		this.sequence = session.get(GSSession.S_SEQUENCE);
		
		contentPanel = new GSSequencePanel(session, new GSDefaultChannelProvider());

		nameField.setText(sequence.getName());

		add(contentPanel);
	}
	
	@Override
	public void onAdded(GSPanel parent) {
		super.onAdded(parent);

		contentPanel.requestFocus();
	}

	@Override
	protected void onShown() {
		super.onShown();

		sequence.addSequenceListener(this);
	}

	@Override
	protected void onHidden() {
		super.onHidden();
		
		sequence.removeSequenceListener(this);
	}

	@Override
	protected void layoutContent(int x, int y, int width, int height) {
		contentPanel.setBounds(x, y, width, height);
	}
	
	@Override
	protected void handleNameChanged(String name) {
		boolean visible = isVisible();
		try {
			if (visible)
				sequence.removeSequenceListener(this);
			sequence.setName(name);
		} finally {
			if (visible)
				sequence.addSequenceListener(this);
		}
	}

	@Override
	public void sequenceNameChanged(String oldName) {
		nameField.setText(sequence.getName());
	}
}
