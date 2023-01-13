package com.g4mesoft.captureplayback.panel.sequence;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSECursorType;
import com.g4mesoft.panel.GSETextAlignment;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.GSTexturedIcon;
import com.g4mesoft.panel.button.GSButton;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSILayoutEventListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSLayoutEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.field.GSTextField;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.util.math.BlockPos;

public class GSChannelHeaderPanel extends GSParentPanel implements GSIScrollable, GSISequenceListener, 
                                                                   GSIModelViewListener, GSIMouseListener {

	private static final GSIcon EDIT_ICON            = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 0,  0, 9, 9));
	private static final GSIcon HOVERED_EDIT_ICON    = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 0,  9, 9, 9));
	private static final GSIcon DISABLED_EDIT_ICON   = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 0, 18, 9, 9));
	private static final GSIcon DELETE_ICON          = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 9,  0, 9, 9));
	private static final GSIcon HOVERED_DELETE_ICON  = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 9,  9, 9, 9));
	private static final GSIcon DISABLED_DELETE_ICON = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion( 9, 18, 9, 9));
	private static final GSIcon MOVE_ICON            = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(18,  0, 9, 9));
	private static final GSIcon HOVERED_MOVE_ICON    = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(18,  9, 9, 9));
	private static final GSIcon DISABLED_MOVE_ICON   = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(18, 18, 9, 9));
	
	private static final int BUTTON_MARGIN = 1;
	private static final int NAME_FIELD_HORIZONTAL_MARGIN = 4;
	
	public static final int CHANNEL_HEADER_COLOR  = 0xFF171717;
	public static final int CHANNEL_HOVER_COLOR   = 0x30FFFFFF;
	public static final int CHANNEL_SPACING_COLOR = 0xFF202020;
	
	private static final int CROSSHAIR_TARGET_BORDER_COLOR = 0xFFE0E0E0;
	
	private static final int CHANNEL_HEADER_PREFERRED_WIDTH = 130;
	
	private final GSSequence sequence;
	private final GSSequenceModelView modelView;
	private final GSSequencePanel sequencePanel;
	
	private final Map<UUID, GSChannelLabelPanel> uuidToLabel;

	private UUID hoveredChannelUUID;
	private boolean editable;
	
	private int dragMouseOffsetY;
	private boolean dragging;
	
	public GSChannelHeaderPanel(GSSequence sequence, GSSequenceModelView modelView, GSSequencePanel sequencePanel) {
		this.sequence = sequence;
		this.modelView = modelView;
		this.sequencePanel = sequencePanel;
		
		uuidToLabel = new HashMap<>();
		
		hoveredChannelUUID = null;
		editable = true;
		
		dragMouseOffsetY = 0;
		dragging = false;
		
		addMouseEventListener(this);
	}
	
	@Override
	protected void layout() {
		layoutChannelLabels();
	}

	@Override
	protected void onShown() {
		super.onShown();

		sequence.addSequenceListener(this);
		modelView.addModelViewListener(this);

		invalidate();
	}
	
	@Override
	protected void onHidden() {
		super.onHidden();

		sequence.removeSequenceListener(this);
		modelView.removeModelViewListener(this);
		
		removeAllChannelLabels();
	}
	
	private void layoutChannelLabels() {
		// TODO: make this better :-)
		
		for (GSChannel channel : sequence.getChannels()) {
			UUID channelUUID = channel.getChannelUUID();

			if (!channelUUID.equals(sequencePanel.getDraggedChannelUUID()))
				layoutChannelLabel(channelUUID);
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
			uuidToLabel.put(channelUUID, labelPanel);
			add(labelPanel);
		}
	}
	
	private void removeAllChannelLabels() {
		for (GSChannelLabelPanel labelPanel : uuidToLabel.values())
			remove(labelPanel);
		uuidToLabel.clear();
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
		
		renderChannelSpacing(renderer);
		super.render(renderer);
	}
	
	protected void renderChannelSpacing(GSIRenderer2D renderer) {
		int cs = modelView.getChannelSpacing();
		int ch = modelView.getChannelHeight();
		for (UUID channelUUID : uuidToLabel.keySet()) {
			int cy = modelView.getChannelY(channelUUID);
			renderer.fillRect(0, cy + ch, width, cs, CHANNEL_SPACING_COLOR);
		}
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		return new GSDimension(CHANNEL_HEADER_PREFERRED_WIDTH, modelView.getMinimumHeight());
	}

	private void startDragging(UUID channelUUID, int draggedChannelY, int dragMouseOffsetY) {
		this.dragMouseOffsetY = dragMouseOffsetY;
		
		sequencePanel.setDraggedChannelUUID(channelUUID);
		sequencePanel.setDraggedChannelY(draggedChannelY);
		
		dragging = true;
	}
	
	private void stopDragging() {
		UUID draggedChannelUUID = sequencePanel.getDraggedChannelUUID();
		sequencePanel.setDraggedChannelUUID(null);
		dragging = false;
		
		layoutChannelLabel(draggedChannelUUID);
	}
	
	private void onChannelDragged(int mouseY) {
		int draggedChannelY = mouseY - dragMouseOffsetY;
		sequencePanel.setDraggedChannelY(draggedChannelY);

		UUID hoveredChannelUUID = modelView.getChannelUUIDFromView(mouseY);
		UUID draggedChannelUUID = sequencePanel.getDraggedChannelUUID();

		GSChannelLabelPanel labelPanel = uuidToLabel.get(draggedChannelUUID);
		
		if (labelPanel != null) {
			labelPanel.setBounds(labelPanel.x, draggedChannelY, labelPanel.width, labelPanel.height);
			
			if (draggedChannelUUID != null && !draggedChannelUUID.equals(hoveredChannelUUID)) {
				boolean moveBefore = (modelView.getChannelY(draggedChannelUUID) >= draggedChannelY);
				if (hoveredChannelUUID == null)
					moveBefore = !moveBefore;
				
				if (moveBefore) {
					sequence.moveChannelBefore(draggedChannelUUID, hoveredChannelUUID);
				} else {
					sequence.moveChannelAfter(draggedChannelUUID, hoveredChannelUUID);
				}
			}
		}
	}
	
	public void setHoveredChannelUUID(UUID hoveredChannelUUID) {
		if (!Objects.equals(this.hoveredChannelUUID, hoveredChannelUUID)) {
			GSChannelLabelPanel labelPanel = uuidToLabel.get(this.hoveredChannelUUID);
			if (labelPanel != null)
				labelPanel.onHoveredChanged(false);
			
			this.hoveredChannelUUID = hoveredChannelUUID;
	
			labelPanel = uuidToLabel.get(hoveredChannelUUID);
			if (labelPanel != null)
				labelPanel.onHoveredChanged(true);
		}
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	
		for (GSChannelLabelPanel labelPanel : uuidToLabel.values())
			labelPanel.onEditableChanged();
	}
	
	@Override
	public boolean isScrollableHeightFilled() {
		return true;
	}
	
	@Override
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
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
		
		invalidate();
	}
	
	@Override
	public void mouseDragged(GSMouseEvent event) {
		if (dragging && event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			onChannelDragged(event.getY());
			event.consume();
		}
	}
	
	private class GSChannelLabelPanel extends GSParentPanel implements GSIMouseListener, GSIKeyListener {

		private final GSChannel channel;

		private final GSTextField nameField;
		private final GSButton deleteButton;
		private final GSButton editButton;
		private final GSButton moveButton;
		
		private boolean buttonsHidden;
		
		public GSChannelLabelPanel(GSChannel channel) {
			this.channel = channel;
			
			nameField = new GSTextField();
			nameField.setBackgroundColor(0x00000000);
			nameField.setTextAlignment(GSETextAlignment.LEFT);
			nameField.setBorderWidth(0);
			nameField.setVerticalMargin(0);
			nameField.setHorizontalMargin(NAME_FIELD_HORIZONTAL_MARGIN);
			
			nameField.addFocusEventListener(new GSIFocusEventListener() {
				@Override
				public void focusLost(GSFocusEvent event) {
					if (nameField.isVisible() && !nameField.hasPopupVisible())
						updateChannelName();
				}
			});
			
			deleteButton = new GSButton(DELETE_ICON);
			deleteButton.setHoveredIcon(HOVERED_DELETE_ICON);
			deleteButton.setDisabledIcon(DISABLED_DELETE_ICON);
			deleteButton.setCursor(GSECursorType.HAND);
			deleteButton.setBackgroundColor(0);
			deleteButton.setHoveredBackgroundColor(0);
			deleteButton.setDisabledBackgroundColor(0);
			deleteButton.setBorderWidth(0);
			deleteButton.addActionListener(() -> {
				sequence.removeChannel(this.channel.getChannelUUID());
				GSChannelHeaderPanel.this.requestFocus();
			});
			
			editButton = new GSButton(EDIT_ICON);
			editButton.setHoveredIcon(HOVERED_EDIT_ICON);
			editButton.setDisabledIcon(DISABLED_EDIT_ICON);
			editButton.setCursor(GSECursorType.HAND);
			editButton.setBackgroundColor(0);
			editButton.setHoveredBackgroundColor(0);
			editButton.setDisabledBackgroundColor(0);
			editButton.setBorderWidth(0);
			editButton.addActionListener(this::openEditor);
			
			moveButton = new GSButton(MOVE_ICON);
			moveButton.setHoveredIcon(HOVERED_MOVE_ICON);
			moveButton.setDisabledIcon(DISABLED_MOVE_ICON);
			moveButton.setCursor(GSECursorType.VRESIZE);
			moveButton.setBackgroundColor(0);
			moveButton.setHoveredBackgroundColor(0);
			moveButton.setDisabledBackgroundColor(0);
			moveButton.setBorderWidth(0);
			moveButton.setClickSound(null);
			moveButton.addMouseEventListener(new GSIMouseListener() {
				@Override
				public void mousePressed(GSMouseEvent event) {
					if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
						int draggedChannelY = GSChannelLabelPanel.this.getY();
						int dragMouseOffsetY = moveButton.getY() + event.getY();
						startDragging(channel.getChannelUUID(), draggedChannelY, dragMouseOffsetY);
						event.consume();
					}
				}

				@Override
				public void mouseReleased(GSMouseEvent event) {
					stopDragging();
					event.consume();
				}
			});
			moveButton.addFocusEventListener(new GSIFocusEventListener() {
				@Override
				public void focusLost(GSFocusEvent event) {
					stopDragging();
					event.consume();
				};
			});
			
			buttonsHidden = true;
			
			this.addMouseEventListener(this);
			this.addKeyEventListener(this);
			
			add(nameField);

			onChannelInfoChanged();
			onEditableChanged();
		}
		
		private void openEditor() {
			GSEditorPanel editor = new GSChannelEditorPanel(this.channel);
			editor.show(null);
			editor.addLayoutEventListener(new GSILayoutEventListener() {
				@Override
				public void panelHidden(GSLayoutEvent event) {
					/* force focus to channel header */
					GSChannelHeaderPanel.this.requestFocus();
				}
			});
		}
		
		private void onChannelInfoChanged() {
			GSChannelInfo info = channel.getInfo();
			nameField.setEditableTextColor(info.getColor());
			nameField.setUneditableTextColor(info.getColor());
			nameField.setText(info.getName());
		}
		
		@Override
		protected void layout() {
			int bs = Math.max(1, height - 2 * BUTTON_MARGIN);

			int nw = width;
			if (!buttonsHidden)
				nw -= 3 * (bs + 2 * BUTTON_MARGIN);
			
			int x = 0;
			nameField.setBounds(x, 0, Math.max(0, nw), height);
			x += nw;

			if (!buttonsHidden) {
				x += BUTTON_MARGIN;
				deleteButton.setBounds(x, BUTTON_MARGIN, bs, bs);
				x += bs + 2 * BUTTON_MARGIN;
				editButton.setBounds(x, BUTTON_MARGIN, bs, bs);
				x += bs + 2 * BUTTON_MARGIN;
				moveButton.setBounds(x, BUTTON_MARGIN, bs, bs);
			}
		}
		
		@Override
		public void render(GSIRenderer2D renderer) {
			if (channel.getChannelUUID().equals(hoveredChannelUUID))
				renderer.fillRect(0, 0, width, height, CHANNEL_HOVER_COLOR);
			
			super.render(renderer);
		
			BlockPos target = GSCapturePlaybackClientModule.getCrosshairTarget();
			if (target != null && channel.getInfo().getPositions().contains(target))
				renderer.drawRect(0, 0, width, height, CROSSHAIR_TARGET_BORDER_COLOR);
		}
		
		@Override
		public void setPassingEvents(boolean passingEvents) {
			super.setPassingEvents(passingEvents);

			for (GSPanel child : GSChannelLabelPanel.this.getChildren())
				child.setPassingEvents(passingEvents);
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
		
		public void showButtons() {
			if (buttonsHidden) {
				buttonsHidden = false;
				add(deleteButton);
				add(editButton);
				add(moveButton);
			}
		}

		public void hideButtons() {
			if (!buttonsHidden) {
				if (deleteButton.isFocused() || editButton.isFocused() || moveButton.isFocused()) {
					// Ensure that focus stays in the panel
					GSChannelLabelPanel.this.requestFocus();
				}

				buttonsHidden = true;
				remove(deleteButton);
				remove(editButton);
				remove(moveButton);
			}
		}
		
		public void onEditableChanged() {
			nameField.setEditable(editable);
			
			if (!editable)
				hideButtons();
		}
		
		public void onHoveredChanged(boolean hovered) {
			if (editable && hovered) {
				showButtons();
			} else {
				hideButtons();
			}
		}
	}
}
