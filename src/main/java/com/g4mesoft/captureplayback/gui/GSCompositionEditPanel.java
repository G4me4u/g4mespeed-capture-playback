package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionColumnHeaderPanel;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionModelView;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionTrackHeaderPanel;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.scroll.GSScrollPanelCorner;

public class GSCompositionEditPanel extends GSAbstractEditPanel implements GSIKeyListener, GSICompositionListener {

	private final GSComposition composition;
	
	private final GSCompositionModelView modelView;
	private final GSCompositionPanel content;
	private final GSCompositionColumnHeaderPanel columnHeader;
	private final GSCompositionTrackHeaderPanel trackHeader;
	
	private boolean changingName;

	public GSCompositionEditPanel(GSSession session) {
		super(session, GSESessionType.COMPOSITION);
		
		this.composition = session.get(GSSession.COMPOSITION);
		
		modelView = new GSCompositionModelView(composition);
		content = new GSCompositionPanel(composition, modelView);
		columnHeader = new GSCompositionColumnHeaderPanel(modelView);
		trackHeader = new GSCompositionTrackHeaderPanel(composition, modelView);
		
		scrollPanel.setTopLeftCorner(new GSScrollPanelCorner(GSCompositionColumnHeaderPanel.BACKGROUND_COLOR));
		scrollPanel.setBottomLeftCorner(new GSScrollPanelCorner(GSCompositionPanel.BACKGROUND_COLOR));
		scrollPanel.setTopRightCorner(new GSScrollPanelCorner(GSCompositionColumnHeaderPanel.BACKGROUND_COLOR));
		
		setContent(content);
		setColumnHeader(columnHeader);
		setRowHeader(trackHeader);

		changingName = false;
		compositionNameChanged(null);
		
		addKeyEventListener(this);
		
		setEditable(true);
	}
	
	@Override
	protected void onShown() {
		modelView.installListeners();
		modelView.updateModelView();

		super.onShown();
	}
	
	@Override
	protected void onHidden() {
		modelView.uninstallListeners();

		super.onHidden();
	}
	
	@Override
	protected void retrieveSessionFields() {
		super.retrieveSessionFields();
		modelView.setGametickWidth(session.get(GSSession.GAMETICK_WIDTH));
	}

	@Override
	protected void offerSessionFields() {
		super.offerSessionFields();
		session.set(GSSession.GAMETICK_WIDTH, modelView.getGametickWidth());
	}
	
	@Override
	public void setEditable(boolean editable) {
		super.setEditable(editable);
	
		// TODO: Implement editable on compositions
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_T) {
			// TODO: change this a lot.
			String trackName = "Track #" + composition.getTracks().size();
			int color = 0xFF000000 | (int)(Math.random() * 0xFFFFFF);
			GSTrackGroup group = composition.addGroup("testgroup");
			composition.addTrack(trackName, color, group.getGroupUUID());
		}
	}
	
	@Override
	protected void onNameChanged(String name) {
		changingName = true;
		composition.setName(name);
		changingName = false;
	}
	
	@Override
	public void compositionNameChanged(String oldName) {
		if (!changingName)
			nameField.setText(composition.getName());
	}
}
