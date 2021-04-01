package com.g4mesoft.captureplayback.panel.sequence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSIChannelSelectionModel;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSSingleChannelSelectionModel;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.button.GSRadioButton;
import com.g4mesoft.panel.button.GSRadioButtonGroup;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.text.GSETextAlignment;
import com.g4mesoft.panel.text.GSTextField;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSChannelHeaderPanel extends GSParentPanel implements GSISequenceListener, GSISequenceModelViewListener {

	public static final int CHANNEL_HEADER_COLOR  = 0xFF171717;
	public static final int CHANNEL_HOVER_COLOR   = 0x30FFFFFF;
	public static final int CHANNEL_SPACING_COLOR = 0xFF202020;
	
	private final GSSequence sequence;
	private final GSSequenceModelView modelView;
	
	private final Map<UUID, GSChannelLabelPanel> uuidToLabel;
	private final GSIChannelSelectionModel selectionModel;
	private final GSRadioButtonGroup selectionButtonGroup;

	private UUID hoveredChannelUUID;
	private boolean editable;
	
	public GSChannelHeaderPanel(GSSequence sequence, GSSequenceModelView modelView) {
		this.sequence = sequence;
		this.modelView = modelView;
		
		uuidToLabel = new HashMap<>();
		selectionModel = new GSSingleChannelSelectionModel();
		selectionButtonGroup = new GSRadioButtonGroup();
		
		hoveredChannelUUID = null;
		editable = true;
	}
	
	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();

		layoutChannelLabels();
	}

	@Override
	protected void onShown() {
		super.onShown();

		sequence.addSequenceListener(this);
		modelView.addModelViewListener(this);
		
		layoutChannelLabels();
	}
	
	@Override
	protected void onHidden() {
		super.onHidden();

		sequence.removeSequenceListener(this);
		modelView.removeModelViewListener(this);
	}
	
	private void layoutChannelLabels() {
		int ch = modelView.getChannelHeight();

		for (GSChannel channel : sequence.getChannels()) {
			UUID channelUUID = channel.getChannelUUID();
			int cy = modelView.getChannelY(channelUUID);
			
			if (cy + ch >= 0 && cy < height) {
				layoutChannelLabel(channelUUID);
			} else {
				removeChannelLabel(channelUUID);
			}
		}
	}

	private void layoutChannelLabel(UUID channelUUID) {
		if (!uuidToLabel.containsKey(channelUUID))
			addChannelLabel(channelUUID);
		
		GSChannelLabelPanel labelPanel = uuidToLabel.get(channelUUID);

		if (labelPanel != null) {
			int cy = modelView.getChannelY(channelUUID);
			int ch = modelView.getChannelHeight();
			labelPanel.setBounds(0, cy, width, ch);
		}
	}
	
	private void addChannelLabel(UUID channelUUID) {
		GSChannel channel = sequence.getChannel(channelUUID);
		
		if (channel != null) {
			GSChannelLabelPanel labelPanel = new GSChannelLabelPanel(channel);
			labelPanel.setEditable(editable);
			uuidToLabel.put(channelUUID, labelPanel);
			add(labelPanel);
		}
	}
	
	private void removeChannelLabel(UUID channelUUID) {
		GSChannelLabelPanel labelPanel = uuidToLabel.remove(channelUUID);
		if (labelPanel != null)
			remove(labelPanel);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, CHANNEL_HEADER_COLOR);
		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		
		renderer.pushClip(0, 0, width, height);
		
		renderChannelSpacing(renderer);
		super.render(renderer);
		
		renderer.popClip();
	}
	
	protected void renderChannelSpacing(GSIRenderer2D renderer) {
		int cs = modelView.getChannelSpacing();
		int ch = modelView.getChannelHeight();
		for (UUID channelUUID : uuidToLabel.keySet()) {
			int cy = modelView.getChannelY(channelUUID);
			renderer.fillRect(0, cy + ch, width, cs, CHANNEL_SPACING_COLOR);
		}
	}
	
	void setHoveredChannelUUID(UUID hoveredChannelUUID) {
		this.hoveredChannelUUID = hoveredChannelUUID;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	
		for (GSChannelLabelPanel labelPanel : uuidToLabel.values())
			labelPanel.setEditable(editable);
	}
	
	@Override
	public void channelAdded(GSChannel channel) {
		layoutChannelLabels();
	}

	@Override
	public void channelRemoved(GSChannel channel) {
		removeChannelLabel(channel.getChannelUUID());
		layoutChannelLabels();
	}

	@Override
	public void channelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
		GSChannelLabelPanel labelPanel = uuidToLabel.get(channel.getChannelUUID());
		if (labelPanel != null)
			labelPanel.onChannelInfoChanged();
	}
	
	@Override
	public void modelViewChanged() {
		layoutChannelLabels();
	}
	
	private class GSChannelLabelPanel extends GSParentPanel implements GSIMouseListener, GSIKeyListener {

		private static final int SELECTION_BUTTON_MARGIN      = 2;
		private static final int NAME_FIELD_HORIZONTAL_MARGIN = 4;
		
		private final GSChannel channel;

		private final GSRadioButton selectionButton;
		private final GSTextField nameField;
		
		public GSChannelLabelPanel(GSChannel channel) {
			this.channel = channel;
			
			selectionButton = new GSRadioButton();
			selectionButton.addActionListener(() -> {
				selectionModel.setSelectedChannel(channel.getChannelUUID());
			});
			
			nameField = new GSTextField();
			nameField.setBackgroundColor(0x00000000);
			nameField.setTextAlignment(GSETextAlignment.LEFT);
			nameField.setBorderWidth(0);
			nameField.setVerticalMargin(0);
			nameField.setHorizontalMargin(0);
			
			nameField.addFocusEventListener(new GSIFocusEventListener() {
				@Override
				public void focusLost(GSFocusEvent event) {
					if (nameField.isVisible() && !nameField.hasPopupVisible()) {
						updateChannelName();
						nameField.unselect();
					}
				}
			});
			
			this.addMouseEventListener(this);
			this.addKeyEventListener(this);
			
			add(selectionButton);
			add(nameField);

			onChannelSelectionChanged();
			onChannelInfoChanged();
		}
		
		private void onChannelSelectionChanged() {
			selectionButton.setSelected(selectionModel.isChannelSelected(channel.getChannelUUID()));
		}
		
		private void onChannelInfoChanged() {
			GSChannelInfo info = channel.getInfo();
			nameField.setEditableTextColor(info.getColor());
			nameField.setUneditableTextColor(info.getColor());
			nameField.setText(info.getName());
		}

		@Override
		public void onAdded(GSPanel parent) {
			super.onAdded(parent);
			
			selectionButtonGroup.addRadioButton(selectionButton);
		}
		
		@Override
		public void onRemoved(GSPanel parent) {
			super.onRemoved(parent);

			selectionButtonGroup.removeRadioButton(selectionButton);
		}
		
		@Override
		protected void onBoundsChanged() {
			super.onBoundsChanged();

			int bs = Math.max(1, height - 2 * SELECTION_BUTTON_MARGIN);
			int nx = bs + 2 * SELECTION_BUTTON_MARGIN + NAME_FIELD_HORIZONTAL_MARGIN;
			selectionButton.setBounds(SELECTION_BUTTON_MARGIN, SELECTION_BUTTON_MARGIN, bs, bs);
			nameField.setBounds(nx, 0, width - nx - NAME_FIELD_HORIZONTAL_MARGIN, height);
		}
		
		@Override
		public void render(GSIRenderer2D renderer) {
			if (channel.getChannelUUID().equals(hoveredChannelUUID))
				renderer.fillRect(0, 0, width, height, CHANNEL_HOVER_COLOR);
			
			super.render(renderer);
		}
		
		@Override
		public void keyPressed(GSKeyEvent event) {
			if (editable && nameField.isFocused()) {
				switch (event.getKeyCode()) {
				case GSKeyEvent.KEY_ESCAPE:
					onChannelInfoChanged();
					requestFocus();
					event.consume();
					break;
				case GSKeyEvent.KEY_ENTER:
					if (nameField.getTextModel().getLength() == 0) {
						onChannelInfoChanged();
					} else {
						updateChannelName();
					}
					requestFocus();
					event.consume();
					break;
				case GSKeyEvent.KEY_TAB:
					if (editNextChannel(true, event.isModifierHeld(GSEvent.MODIFIER_SHIFT)))
						event.consume();
					break;
				case GSKeyEvent.KEY_DOWN:
					if (editNextChannel(false, false))
						event.consume();
					break;
				case GSKeyEvent.KEY_UP:
					if (editNextChannel(false, true))
						event.consume();
					break;
				}
			}
		}

		private void updateChannelName() {
			channel.setInfo(channel.getInfo().withName(nameField.getText()));
		}
		
		private boolean editNextChannel(boolean selectAll, boolean descending) {
			UUID channelUUID = modelView.getNextChannelUUID(channel.getChannelUUID(), descending);
			GSChannelLabelPanel labelPanel = uuidToLabel.get(channelUUID);
			
			if (labelPanel != null) {
				labelPanel.nameField.requestFocus();
				if (selectAll)
					labelPanel.nameField.selectAll();
				return true;
			} else {
				requestFocus();
			}
			
			return false;
		}
		
		public void setEditable(boolean editable) {
			nameField.setEditable(editable);
		}
	}
}
