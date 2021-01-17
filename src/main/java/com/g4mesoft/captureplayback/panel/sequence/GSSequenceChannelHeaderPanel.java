package com.g4mesoft.captureplayback.panel.sequence;

import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSECursorType;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.text.GSETextAlignment;
import com.g4mesoft.panel.text.GSITextCaret;
import com.g4mesoft.panel.text.GSITextModel;
import com.g4mesoft.panel.text.GSTextField;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSSequenceChannelHeaderPanel extends GSParentPanel implements GSISequenceListener, GSISequenceModelViewListener,
                                                                         GSIMouseListener, GSIKeyListener {

	public static final int CHANNEL_HEADER_COLOR = 0xFF171717;
	public static final int CHANNEL_HOVER_COLOR = 0x30FFFFFF;
	public static final int CHANNEL_SPACING_COLOR = 0xFF202020;
	
	private final GSSequence sequence;
	private final GSSequenceModelView modelView;
	
	private UUID hoveredChannelUUID;

	private final GSTextField channelNameField;

	private boolean editable;
	private UUID editingChannelUUID;
	
	public GSSequenceChannelHeaderPanel(GSSequence sequence, GSSequenceModelView modelView) {
		this.sequence = sequence;
		this.modelView = modelView;
	
		hoveredChannelUUID = null;
		
		channelNameField = new GSTextField();
		channelNameField.setBackgroundColor(0x00000000);
		channelNameField.setTextAlignment(GSETextAlignment.CENTER);
		channelNameField.setBorderWidth(0);
		channelNameField.setVerticalMargin(0);
		channelNameField.setHorizontalMargin(0);
		
		channelNameField.addFocusEventListener(new GSIFocusEventListener() {
			@Override
			public void focusLost(GSFocusEvent event) {
				resetNameFieldCaret();
			}
		});
		
		editingChannelUUID = null;
		
		addMouseEventListener(this);
		addKeyEventListener(this);

		setEditable(true);
	}
	
	@Override
	protected void onBoundsChanged() {
		if (editingChannelUUID != null)
			updateNameFieldBounds();
	}
	
	@Override
	public void onShown() {
		super.onShown();

		modelView.addModelViewListener(this);
		sequence.addSequenceListener(this);
		
		setCurrentEditingChannel(hoveredChannelUUID, false);
	}
	
	@Override
	public void onHidden() {
		super.onHidden();

		modelView.removeModelViewListener(this);
		sequence.removeSequenceListener(this);
	
		setCurrentEditingChannel(null, false);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, CHANNEL_HEADER_COLOR);
		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		
		renderChannelLabels(renderer);

		super.render(renderer);
	}
	
	protected void renderChannelLabels(GSIRenderer2D renderer) {
		renderer.pushClip(0, 0, width, height);
		
		for (GSChannel channel : sequence.getChannels()) {
			int ty = modelView.getChannelY(channel.getChannelUUID());
			if (ty + modelView.getChannelHeight() > 0 && ty < height)
				renderChannelLabel(renderer, channel, ty);
		}

		renderer.popClip();
	}
	
	private void renderChannelLabel(GSIRenderer2D renderer, GSChannel channel, int y) {
		int th = modelView.getChannelHeight();
		
		if (channel.getChannelUUID().equals(hoveredChannelUUID))
			renderer.fillRect(0, y, width, th, CHANNEL_HOVER_COLOR);
		
		if (!channel.getChannelUUID().equals(editingChannelUUID)) {
			String name = renderer.trimString(channel.getInfo().getName(), width);
			int xt = (width - (int)Math.ceil(renderer.getTextWidth(name))) / 2;
			int yt = y + (modelView.getChannelHeight() - renderer.getTextHeight() + 1) / 2;
			renderer.drawText(name, xt, yt, channel.getInfo().getColor());
		}

		renderer.fillRect(0, y + th, width, modelView.getChannelSpacing(), CHANNEL_SPACING_COLOR);
	}
	
	@Override
	public GSECursorType getCursor() {
		if (hoveredChannelUUID != null)
			return channelNameField.getCursor();
		return super.getCursor();
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT && !channelNameField.isFocused()) {
			if (editable)
				updateChannelNameInfo();
			
			setCurrentEditingChannel(hoveredChannelUUID, true);
			
			if (editingChannelUUID != null)
				channelNameField.dispatchMouseEvent(event, this);

			event.consume();
		}
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (editable && channelNameField.isFocused()) {
			switch (event.getKeyCode()) {
			case GSKeyEvent.KEY_ESCAPE:
				setCurrentEditingChannel(null, false);
				event.consume();
				break;
			case GSKeyEvent.KEY_ENTER:
				updateChannelNameInfo();
				setCurrentEditingChannel(null, false);
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
	
	private boolean editNextChannel(boolean selectAll, boolean descending) {
		if (channelNameField.isFocused() && editingChannelUUID != null) {
			UUID nextChannelUUID = modelView.getNextChannelUUID(editingChannelUUID, descending);
			
			updateChannelNameInfo();
			setCurrentEditingChannel(nextChannelUUID, true);

			if (selectAll && nextChannelUUID != null)
				selectAllNameFieldText();

			return true;
		}
		
		return false;
	}

	private void setCurrentEditingChannel(UUID channelUUID, boolean autoFocus) {
		if (!Objects.equals(editingChannelUUID, channelUUID)) {
			editingChannelUUID = channelUUID;

			if (editingChannelUUID != null) {
				resetNameFieldText();

				if (!channelNameField.isAdded())
					add(channelNameField);
				
				updateNameFieldBounds();
			} else if (channelNameField.isAdded()) {
				boolean wasFocused = channelNameField.isFocused();
				remove(channelNameField);
				
				if (wasFocused) {
					// Ensure that the user can still trigger hotkeys.
					requestFocus();
				}
			}
		}
		
		if (channelNameField.isAdded() && !channelNameField.isFocused()) {
			if (autoFocus)
				channelNameField.requestFocus();

			resetNameFieldCaret();
		}
	}
	
	private void updateNameFieldBounds() {
		int ty = modelView.getChannelY(editingChannelUUID);
		int th = modelView.getChannelHeight();
		
		if (ty < 0 || ty + th > height) {
			// The name field is out of bounds. We have to cancel
			// the editing.
			setCurrentEditingChannel(null, false);
		} else {
			channelNameField.setBounds(0, ty, width, th);
		}
	}

	private void resetNameFieldText() {
		GSChannel editingChannel = sequence.getChannel(editingChannelUUID);
		if (editingChannel != null) {
			GSChannelInfo editingInfo = editingChannel.getInfo();
			channelNameField.setText(editingInfo.getName());
			channelNameField.setEditableTextColor(editingInfo.getColor());
			channelNameField.setUneditableTextColor(editingInfo.getColor());
		}
	}
	
	private void resetNameFieldCaret() {
		GSITextModel textModel = channelNameField.getTextModel();
		GSITextCaret caret = channelNameField.getCaret();
		caret.setCaretLocation(textModel.getLength());
	}
	
	private void selectAllNameFieldText() {
		GSITextModel textModel = channelNameField.getTextModel();
		GSITextCaret caret = channelNameField.getCaret();
		caret.setCaretDot(textModel.getLength());
		caret.setCaretMark(0);
	}
	
	private void updateChannelNameInfo() {
		String name = channelNameField.getText();

		if (name.isEmpty()) {
			resetNameFieldText();
		} else {
			GSChannel editingChannel = sequence.getChannel(editingChannelUUID);
			
			if (editingChannel != null) {
				GSChannelInfo info = editingChannel.getInfo();
				
				if (!info.getName().equals(name))
					editingChannel.setInfo(info.withName(name));
			}
		}
	}
	
	@Override
	public void channelAdded(GSChannel channel) {
		updateNameFieldBounds();
	}
	
	@Override
	public void channelRemoved(GSChannel channel) {
		if (channel.getChannelUUID().equals(editingChannelUUID)) {
			if (channelNameField.isFocused()) {
				// Make sure to unfocus the channel name field. This is
				// to ensure that it is no longer focused in case the
				// hoveredChannelUUID has not been updated yet.
				requestFocus();
			}
			
			setCurrentEditingChannel(hoveredChannelUUID, false);
		} else {
			updateNameFieldBounds();
		}
	}
	
	@Override
	public void channelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
		// In case the user is hovering, but not editing, a channel and another
		// user changes the name of that channel, we have to update the name.
		if (!channelNameField.isFocused() && channel.getChannelUUID().equals(editingChannelUUID))
			resetNameFieldText();
	}
	
	@Override
	public void modelViewChanged() {
		updateNameFieldBounds();
	}

	public UUID getHoveredChannelUUID() {
		return hoveredChannelUUID;
	}
	
	void setHoveredChannelUUID(UUID hoveredChannelUUID) {
		this.hoveredChannelUUID = hoveredChannelUUID;
		
		if (!channelNameField.isFocused())
			setCurrentEditingChannel(hoveredChannelUUID, false);
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	
		if (!editable && editingChannelUUID != null)
			resetNameFieldText();
		
		channelNameField.setEditable(editable);
	}
}
