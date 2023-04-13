package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionColumnHeaderPanel;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionModelView;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionTrackHeaderPanel;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.ui.panel.GSLocation;
import com.g4mesoft.ui.panel.GSPanelUtil;
import com.g4mesoft.ui.panel.event.GSEvent;
import com.g4mesoft.ui.panel.event.GSIKeyListener;
import com.g4mesoft.ui.panel.event.GSIMouseListener;
import com.g4mesoft.ui.panel.event.GSKeyEvent;
import com.g4mesoft.ui.panel.event.GSMouseEvent;
import com.g4mesoft.ui.panel.scroll.GSScrollPanelCorner;

public class GSCompositionEditPanel extends GSAbstractEditPanel implements GSICompositionListener, GSIModelViewListener,
                                                                           GSIMouseListener, GSIKeyListener {

	private final GSComposition composition;
	
	private final GSCompositionModelView modelView;
	
	private final GSCompositionPanel content;
	private final GSCompositionColumnHeaderPanel columnHeader;
	private final GSCompositionTrackHeaderPanel trackHeader;
	
	private int hoveredMouseY;
	private UUID hoveredTrackUUID;
	
	private boolean changingName;

	public GSCompositionEditPanel(GSSession session) {
		super(session, GSESessionType.COMPOSITION);
		
		this.composition = session.get(GSSession.COMPOSITION);
		
		modelView = new GSCompositionModelView(composition);
		modelView.addModelViewListener(this);
		
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
		
		addMouseEventListener(this);
		addKeyEventListener(this);
		
		setEditable(true);
	}
	
	@Override
	protected void onShown() {
		modelView.installListeners();
		modelView.updateModelView();
		
		composition.addCompositionListener(this);

		super.onShown();
	}
	
	@Override
	protected void onHidden() {
		modelView.uninstallListeners();

		composition.removeCompositionListener(this);
		
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
	
		content.setEditable(editable);
		trackHeader.setEditable(editable);
		nameField.setEditable(editable);
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
	
	@Override
	public void modelViewChanged() {
		updateHoveredTrack();
	}

	@Override
	public void mouseMoved(GSMouseEvent event) {
		GSLocation viewLocation = GSPanelUtil.getViewLocation(this);
		GSLocation contentViewLocation = GSPanelUtil.getViewLocation(content);
		// Offset mouse coordinates to absolute and then back relative to content.
		hoveredMouseY = event.getY() + viewLocation.getY() - contentViewLocation.getY();
		updateHoveredTrack();
	}
	
	private void updateHoveredTrack() {
		GSTrack hoveredTrack = modelView.getTrackFromY(hoveredMouseY);
		hoveredTrackUUID = (hoveredTrack == null) ? null : hoveredTrack.getTrackUUID();
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_T && isEditable()) {
			// TODO: change this a lot.
			if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
				if (hoveredTrackUUID != null)
					composition.removeTrack(hoveredTrackUUID);
			} else {
				String trackName = "Track #" + composition.getTracks().size();
				int color = 0xFF000000 | (int)(Math.random() * 0xFFFFFF);
				GSTrackGroup group = composition.addGroup("testgroup");
				composition.addTrack(trackName, color, group.getGroupUUID());
			}
		}
	}
}
