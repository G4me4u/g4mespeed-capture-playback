package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.panel.composition.GSIChannelProvider;
import com.g4mesoft.captureplayback.panel.sequence.GSChannelHeaderPanel;
import com.g4mesoft.captureplayback.panel.sequence.GSExpandedColumnModel;
import com.g4mesoft.captureplayback.panel.sequence.GSSequenceButtonPanel;
import com.g4mesoft.captureplayback.panel.sequence.GSSequenceColumnHeaderPanel;
import com.g4mesoft.captureplayback.panel.sequence.GSSequenceInfoPanel;
import com.g4mesoft.captureplayback.panel.sequence.GSSequenceModelView;
import com.g4mesoft.captureplayback.panel.sequence.GSSequencePanel;
import com.g4mesoft.captureplayback.panel.sequence.GSSequencePreviewScrollBar;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.panel.GSLocation;
import com.g4mesoft.panel.GSPanelUtil;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.scroll.GSScrollPanelCorner;
import com.google.common.base.Objects;

public class GSSequenceEditPanel extends GSAbstractEditPanel implements GSISequenceListener, GSIModelViewListener,
                                                                        GSIMouseListener, GSIKeyListener {

	private final GSSequence sequence;
	
	private final GSIChannelProvider channelProvider;
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;

	private final GSSequencePanel content;
	private final GSSequenceColumnHeaderPanel columnHeader;
	private final GSChannelHeaderPanel channelHeader;
	
	private final GSSequenceInfoPanel infoPanel;
	private final GSSequenceButtonPanel buttonPanel;
	
	private int hoveredMouseX;
	private int hoveredMouseY;
	private int hoveredColumnIndex;
	private UUID hoveredChannelUUID;

	private boolean changingName;
	
	public GSSequenceEditPanel(GSSession session, GSIChannelProvider channelProvider) {
		super(session, GSESessionType.SEQUENCE);
	
		this.sequence = session.get(GSSession.SEQUENCE);
		this.channelProvider = channelProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSSequenceModelView(sequence, expandedColumnModel);
		modelView.addModelViewListener(this);
		
		content = new GSSequencePanel(sequence, modelView);
		columnHeader = new GSSequenceColumnHeaderPanel(sequence, modelView);
		channelHeader = new GSChannelHeaderPanel(sequence, modelView, content);
		
		infoPanel = new GSSequenceInfoPanel(session);
		buttonPanel = new GSSequenceButtonPanel();
		
		setContent(content);
		setColumnHeader(columnHeader);
		setRowHeader(channelHeader);
		setHorizontalScrollBar(new GSSequencePreviewScrollBar(sequence, modelView));
		
		scrollPanel.setTopLeftCorner(infoPanel);
		scrollPanel.setBottomLeftCorner(buttonPanel);
		scrollPanel.setTopRightCorner(new GSScrollPanelCorner(GSChannelHeaderPanel.CHANNEL_HEADER_COLOR));
		
		changingName = false;
		sequenceNameChanged(null);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
		
		setEditable(true);
	}
	
	@Override
	protected void onShown() {
		modelView.installListeners();
		modelView.updateModelView();

		sequence.addSequenceListener(this);
		
		super.onShown();
	}
	
	@Override
	protected void onHidden() {
		modelView.uninstallListeners();
		setHoveredCell(-1, null);
		
		sequence.removeSequenceListener(this);
		
		super.onHidden();
	}

	@Override
	protected void onNameChanged(String name) {
		changingName = true;
		sequence.setName(name);
		changingName = false;
	}
	
	private void setHoveredCell(int columnIndex, UUID channelUUID) {
		if (columnIndex != hoveredColumnIndex || !Objects.equal(channelUUID, hoveredChannelUUID)) {
			hoveredChannelUUID = channelUUID;
			hoveredColumnIndex = columnIndex;
			
			content.setHoveredCell(columnIndex, channelUUID);
			channelHeader.setHoveredChannelUUID(channelUUID);
			columnHeader.setHoveredColumn(columnIndex);
		}
	}
	
	@Override
	public void setEditable(boolean editable) {
		super.setEditable(editable);
		
		channelHeader.setEditable(editable);
		content.setEditable(editable);
		nameField.setEditable(editable);
	}

	@Override
	public void sequenceNameChanged(String oldName) {
		if (!changingName)
			nameField.setText(sequence.getName());
	}
	
	@Override
	public void modelViewChanged() {
		updateHoveredCell();
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		GSLocation viewLocation = GSPanelUtil.getViewLocation(this);
		GSLocation contentViewLocation = GSPanelUtil.getViewLocation(content);
		// Offset mouse coordinates to absolute and then back relative to content.
		hoveredMouseX = event.getX() + viewLocation.getX() - contentViewLocation.getX();
		hoveredMouseY = event.getY() + viewLocation.getY() - contentViewLocation.getY();
		
		updateHoveredCell();
	}
	
	private void updateHoveredCell() {
		int columnIndex = modelView.getColumnIndexFromX(hoveredMouseX);
		UUID channelUUID;
		if (content.getDraggedChannelUUID() != null) {
			channelUUID = content.getDraggedChannelUUID();
		} else {
			channelUUID = modelView.getChannelUUIDFromView(hoveredMouseY);
		}
		
		setHoveredCell(columnIndex, channelUUID);
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		switch (event.getKeyCode()) {
		case GSKeyEvent.KEY_T:
			if (isEditable()) {
				if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
					if (hoveredChannelUUID != null && sequence.removeChannel(hoveredChannelUUID))
						event.consume();
				} else {
					sequence.addChannel(channelProvider.createChannelInfo(sequence));
					event.consume();
				}
			}
			break;
		case GSKeyEvent.KEY_E:
			if (!event.isRepeating()) {
				if (expandedColumnModel.hasExpandedColumn()) {
					expandedColumnModel.clearExpandedColumns();
				} else {
					expandedColumnModel.setExpandedColumnRange(0, Integer.MAX_VALUE);
				}
				event.consume();
			}
			break;
		}
	}
}
