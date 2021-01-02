package com.g4mesoft.captureplayback.gui.timeline;

import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;
import com.g4mesoft.gui.GSECursorType;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.event.GSEvent;
import com.g4mesoft.gui.event.GSFocusEvent;
import com.g4mesoft.gui.event.GSIFocusEventListener;
import com.g4mesoft.gui.event.GSIKeyListener;
import com.g4mesoft.gui.event.GSIMouseListener;
import com.g4mesoft.gui.event.GSKeyEvent;
import com.g4mesoft.gui.event.GSMouseEvent;
import com.g4mesoft.gui.text.GSETextAlignment;
import com.g4mesoft.gui.text.GSITextCaret;
import com.g4mesoft.gui.text.GSITextModel;
import com.g4mesoft.gui.text.GSTextField;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSTimelineTrackHeaderPanel extends GSParentPanel implements GSITimelineListener, GSITimelineModelViewListener,
                                                                         GSIMouseListener, GSIKeyListener {

	public static final int TRACK_HEADER_COLOR = 0xDA0A0A0A;
	
	public static final int TRACK_HOVER_COLOR = 0x30FFFFFF;
	public static final int TRACK_SPACING_COLOR = 0xFF444444;
	
	private final GSTimeline timeline;
	private final GSTimelineModelView modelView;
	
	private UUID hoveredTrackUUID;

	private final GSTextField trackNameField;

	private boolean editable;
	private UUID editingTrackUUID;
	
	public GSTimelineTrackHeaderPanel(GSTimeline timeline, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.modelView = modelView;
	
		hoveredTrackUUID = null;
		
		trackNameField = new GSTextField();
		trackNameField.setBackgroundColor(0x00000000);
		trackNameField.setTextAlignment(GSETextAlignment.CENTER);
		trackNameField.setBorderWidth(0);
		trackNameField.setVerticalMargin(0);
		trackNameField.setHorizontalMargin(0);
		
		trackNameField.addFocusEventListener(new GSIFocusEventListener() {
			@Override
			public void focusLost(GSFocusEvent event) {
				resetNameFieldCaret();
			}
		});
		
		editingTrackUUID = null;
		
		addMouseEventListener(this);
		addKeyEventListener(this);

		setEditable(true);
	}
	
	@Override
	protected void onBoundsChanged() {
		if (editingTrackUUID != null)
			updateNameFieldBounds();
	}
	
	@Override
	public void onShown() {
		super.onShown();

		modelView.addModelViewListener(this);
		timeline.addTimelineListener(this);
		
		setCurrentEditingTrack(hoveredTrackUUID, false);
	}
	
	@Override
	public void onHidden() {
		super.onHidden();

		modelView.removeModelViewListener(this);
		timeline.removeTimelineListener(this);
	
		setCurrentEditingTrack(null, false);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, TRACK_HEADER_COLOR);
		renderer.drawVLine(width - 1, 0, height, GSTimelineColumnHeaderPanel.COLUMN_LINE_COLOR);
		
		renderTrackLabels(renderer);

		super.render(renderer);
	}
	
	protected void renderTrackLabels(GSIRenderer2D renderer) {
		renderer.pushClip(0, 0, width, height);
		
		for (GSTrack track : timeline.getTracks()) {
			int ty = modelView.getTrackY(track.getTrackUUID());
			if (ty + modelView.getTrackHeight() > 0 && ty < height)
				renderTrackLabel(renderer, track, ty);
		}

		renderer.popClip();
	}
	
	private void renderTrackLabel(GSIRenderer2D renderer, GSTrack track, int y) {
		int th = modelView.getTrackHeight();
		
		if (track.getTrackUUID().equals(hoveredTrackUUID))
			renderer.fillRect(0, y, width, th, TRACK_HOVER_COLOR);
		
		if (!track.getTrackUUID().equals(editingTrackUUID)) {
			String name = renderer.trimString(track.getInfo().getName(), width);
			int xt = (width - (int)Math.ceil(renderer.getTextWidth(name))) / 2;
			int yt = y + (modelView.getTrackHeight() - renderer.getTextHeight() + 1) / 2;
			renderer.drawText(name, xt, yt, track.getInfo().getColor());
		}

		renderer.fillRect(0, y + th, width, modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	@Override
	public GSECursorType getCursor() {
		if (hoveredTrackUUID != null)
			return trackNameField.getCursor();
		return super.getCursor();
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT && !trackNameField.isFocused()) {
			if (editable)
				updateTrackNameInfo();
			
			setCurrentEditingTrack(hoveredTrackUUID, true);
			
			if (editingTrackUUID != null)
				trackNameField.dispatchMouseEvent(event, this);

			event.consume();
		}
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (editable && trackNameField.isFocused()) {
			switch (event.getKeyCode()) {
			case GSKeyEvent.KEY_ESCAPE:
				setCurrentEditingTrack(null, false);
				event.consume();
				break;
			case GSKeyEvent.KEY_ENTER:
				updateTrackNameInfo();
				setCurrentEditingTrack(null, false);
				event.consume();
				break;
			case GSKeyEvent.KEY_TAB:
				if (editNextTrack(true, event.isModifierHeld(GSEvent.MODIFIER_SHIFT)))
					event.consume();
				break;
			case GSKeyEvent.KEY_DOWN:
				if (editNextTrack(false, false))
					event.consume();
				break;
			case GSKeyEvent.KEY_UP:
				if (editNextTrack(false, true))
					event.consume();
				break;
			}
		}
	}
	
	private boolean editNextTrack(boolean selectAll, boolean descending) {
		if (trackNameField.isFocused() && editingTrackUUID != null) {
			UUID nextTrackUUID = modelView.getNextTrackUUID(editingTrackUUID, descending);
			
			updateTrackNameInfo();
			setCurrentEditingTrack(nextTrackUUID, true);

			if (selectAll && nextTrackUUID != null)
				selectAllNameFieldText();

			return true;
		}
		
		return false;
	}

	private void setCurrentEditingTrack(UUID trackUUID, boolean autoFocus) {
		if (!Objects.equals(editingTrackUUID, trackUUID)) {
			editingTrackUUID = trackUUID;

			if (editingTrackUUID != null) {
				resetNameFieldText();

				if (!trackNameField.isAdded())
					add(trackNameField);
				
				updateNameFieldBounds();
			} else if (trackNameField.isAdded()) {
				boolean wasFocused = trackNameField.isFocused();
				remove(trackNameField);
				
				if (wasFocused) {
					// Ensure that the user can still trigger hotkeys.
					requestFocus();
				}
			}
		}
		
		if (trackNameField.isAdded() && !trackNameField.isFocused()) {
			if (autoFocus)
				trackNameField.requestFocus();

			resetNameFieldCaret();
		}
	}
	
	private void updateNameFieldBounds() {
		int ty = modelView.getTrackY(editingTrackUUID);
		int th = modelView.getTrackHeight();
		
		if (ty < 0 || ty + th > height) {
			// The name field is out of bounds. We have to cancel
			// the editing.
			setCurrentEditingTrack(null, false);
		} else {
			trackNameField.setBounds(0, ty, width, th);
		}
	}

	private void resetNameFieldText() {
		GSTrack editingTrack = timeline.getTrack(editingTrackUUID);
		if (editingTrack != null) {
			GSTrackInfo editingInfo = editingTrack.getInfo();
			trackNameField.setText(editingInfo.getName());
			trackNameField.setEditableTextColor(editingInfo.getColor());
			trackNameField.setUneditableTextColor(editingInfo.getColor());
		}
	}
	
	private void resetNameFieldCaret() {
		GSITextModel textModel = trackNameField.getTextModel();
		GSITextCaret caret = trackNameField.getCaret();
		caret.setCaretLocation(textModel.getLength());
	}
	
	private void selectAllNameFieldText() {
		GSITextModel textModel = trackNameField.getTextModel();
		GSITextCaret caret = trackNameField.getCaret();
		caret.setCaretDot(textModel.getLength());
		caret.setCaretMark(0);
	}
	
	private void updateTrackNameInfo() {
		String name = trackNameField.getText();

		if (name.isEmpty()) {
			resetNameFieldText();
		} else {
			GSTrack editingTrack = timeline.getTrack(editingTrackUUID);
			
			if (editingTrack != null) {
				GSTrackInfo info = editingTrack.getInfo();
				
				if (!info.getName().equals(name))
					editingTrack.setInfo(info.withName(name));
			}
		}
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		updateNameFieldBounds();
	}
	
	@Override
	public void trackRemoved(GSTrack track) {
		if (track.getTrackUUID().equals(editingTrackUUID)) {
			if (trackNameField.isFocused()) {
				// Make sure to unfocus the track name field. This is
				// to ensure that it is no longer focused in case the
				// hoveredTrackUUID has not been updated yet.
				requestFocus();
			}
			
			setCurrentEditingTrack(hoveredTrackUUID, false);
		} else {
			updateNameFieldBounds();
		}
	}
	
	@Override
	public void trackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
		// In case the user is hovering, but not editing, a track and another
		// user changes the name of that track, we have to update the name.
		if (!trackNameField.isFocused() && track.getTrackUUID().equals(editingTrackUUID))
			resetNameFieldText();
	}
	
	@Override
	public void modelViewChanged() {
		updateNameFieldBounds();
	}

	public UUID getHoveredTrackUUID() {
		return hoveredTrackUUID;
	}
	
	void setHoveredTrackUUID(UUID hoveredTrackUUID) {
		this.hoveredTrackUUID = hoveredTrackUUID;
		
		if (!trackNameField.isFocused())
			setCurrentEditingTrack(hoveredTrackUUID, false);
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	
		if (!editable && editingTrackUUID != null)
			resetNameFieldText();
		
		trackNameField.setEditable(editable);
	}
}
