package com.g4mesoft.captureplayback.gui.timeline;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;
import com.g4mesoft.gui.GSECursorType;
import com.g4mesoft.gui.GSIElement;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.event.GSEvent;
import com.g4mesoft.gui.event.GSIKeyListener;
import com.g4mesoft.gui.event.GSIMouseListener;
import com.g4mesoft.gui.event.GSKeyEvent;
import com.g4mesoft.gui.event.GSMouseEvent;
import com.g4mesoft.gui.renderer.GSIRenderer2D;
import com.g4mesoft.gui.text.GSETextAlignment;
import com.g4mesoft.gui.text.GSITextCaret;
import com.g4mesoft.gui.text.GSITextModel;
import com.g4mesoft.gui.text.GSTextField;

public class GSTimelineTrackHeaderGUI extends GSParentPanel implements GSITimelineListener, GSITimelineModelViewListener,
                                                                       GSIMouseListener, GSIKeyListener {

	public static final int TRACK_HEADER_COLOR = 0x60000000;
	
	public static final int TRACK_HOVER_COLOR = 0x30FFFFFF;
	public static final int TRACK_SPACING_COLOR = 0xFF444444;
	
	private final GSTimeline timeline;
	private final GSTimelineModelView modelView;
	
	private int currentMouseY;
	private UUID hoveredTrackUUID;

	private final GSTextField trackNameField;

	private UUID editingTrackUUID;
	
	public GSTimelineTrackHeaderGUI(GSTimeline timeline, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.modelView = modelView;
	
		trackNameField = new GSTextField();
		trackNameField.setBackgroundColor(0x00000000);
		trackNameField.setTextAlignment(GSETextAlignment.CENTER);
		trackNameField.setBorderWidth(0);
		trackNameField.setVerticalMargin(0);
		trackNameField.setHorizontalMargin(0);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
	}
	
	@Override
	protected void onBoundsChanged() {
		if (trackNameField.isFocused() && editingTrackUUID != null)
			updateNameFieldBounds();
	}
	
	@Override
	public void onAdded(GSIElement parent) {
		super.onAdded(parent);

		modelView.addModelViewListener(this);
		timeline.addTimelineListener(this);
	}
	
	@Override
	public void onRemoved(GSIElement parent) {
		super.onRemoved(parent);

		modelView.removeModelViewListener(this);
		timeline.removeTimelineListener(this);
	}
	
	@Override
	public boolean isEditingText() {
		return trackNameField.isEditingText();
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, TRACK_HEADER_COLOR);
		renderer.drawVLine(width - 1, 0, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		
		renderTrackLabels(renderer);

		super.render(renderer);
	}
	
	protected void renderTrackLabels(GSIRenderer2D renderer) {
		renderer.pushClip(0, 0, width, height);
		
		for (Map.Entry<UUID, GSTrack> trackEntry : timeline.getTrackEntries()) {
			UUID trackUUID = trackEntry.getKey();
			GSTrack track = trackEntry.getValue();
			
			int y = modelView.getTrackY(trackUUID);
			if (y + modelView.getTrackHeight() > 0 && y < height)
				renderTrackLabel(renderer, track, trackUUID, y);
			
			y += modelView.getTrackHeight();
		}

		renderer.popClip();
	}
	
	private void renderTrackLabel(GSIRenderer2D renderer, GSTrack track, UUID trackUUID, int y) {
		int th = modelView.getTrackHeight();
		
		if (track.getTrackUUID().equals(hoveredTrackUUID))
			renderer.fillRect(0, y, width, th, TRACK_HOVER_COLOR);
		
		if (!track.getTrackUUID().equals(editingTrackUUID)) {
			String name = renderer.trimString(track.getInfo().getName(), width);
			int xt = (width - (int)Math.ceil(renderer.getStringWidth(name))) / 2;
			int yt = y + (modelView.getTrackHeight() - renderer.getFontHeight() + 1) / 2;
			renderer.drawString(name, xt, yt, getTrackColor(track));
		}

		renderer.fillRect(0, y + th, width, modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	private int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	@Override
	public GSECursorType getCursor() {
		return hoveredTrackUUID != null ? trackNameField.getCursor() : super.getCursor();
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		currentMouseY = event.getY();

		updateHoveredTrack();
	}
	
	private void updateHoveredTrack() {
		hoveredTrackUUID = modelView.getTrackUUIDFromView(currentMouseY);

		if (!trackNameField.isFocused())
			setCurrentEditingTrack(hoveredTrackUUID, false);
	}

	@Override
	public void mousePressed(GSMouseEvent event) {
		if (!Objects.equals(hoveredTrackUUID, editingTrackUUID)) {
			updateNameFieldInfo();
			setCurrentEditingTrack(hoveredTrackUUID, true);
			
			if (editingTrackUUID != null)
				trackNameField.dispatchMouseEvent(event, this);

			event.consume();
		}
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (trackNameField.isFocused()) {
			switch (event.getKeyCode()) {
			case GSKeyEvent.KEY_ESCAPE:
				setCurrentEditingTrack(null, false);
				event.consume();
				break;
			case GSKeyEvent.KEY_ENTER:
				updateNameFieldInfo();
				setCurrentEditingTrack(null, false);
				event.consume();
				break;
			case GSKeyEvent.KEY_TAB:
				editNextTrack(event, true, event.isModifierHeld(GSEvent.MODIFIER_SHIFT));
				break;
			case GSKeyEvent.KEY_DOWN:
				editNextTrack(event, false, false);
				break;
			case GSKeyEvent.KEY_UP:
				editNextTrack(event, false, true);
				break;
			}
		}
	}
	
	private void editNextTrack(GSKeyEvent event, boolean select, boolean descending) {
		if (trackNameField.isFocused() && editingTrackUUID != null) {
			UUID nextTrackUUID = modelView.getNextTrackUUID(editingTrackUUID, descending);
			
			updateNameFieldInfo();
			setCurrentEditingTrack(nextTrackUUID, true);

			if (select && nextTrackUUID != null)
				selectAllNameFieldText();
			
			event.consume();
		}
	}

	private void setCurrentEditingTrack(UUID trackUUID, boolean autoFocus) {
		if (!Objects.equals(editingTrackUUID, trackUUID)) {
			editingTrackUUID = trackUUID;

			if (editingTrackUUID != null) {
				resetNameFieldText();

				if (!trackNameField.isAdded())
					add(trackNameField);
				
				updateNameFieldBounds();
			}else if (trackNameField.isAdded()) {
				remove(trackNameField);
			}
		}
		
		if (trackNameField.isAdded() && autoFocus)
			trackNameField.requestFocus();
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
			trackNameField.setText(editingTrack.getInfo().getName());
			trackNameField.setEditableTextColor(getTrackColor(editingTrack));
		}
	}
	
	private void selectAllNameFieldText() {
		GSITextModel textModel = trackNameField.getTextModel();
		GSITextCaret caret = trackNameField.getCaret();
		
		caret.setCaretDot(textModel.getLength());
		caret.setCaretMark(0);
	}
	
	private void updateNameFieldInfo() {
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
	public void trackRemoved(GSTrack track) {
		updateHoveredTrack();
		updateNameFieldBounds();
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
		updateHoveredTrack();
		updateNameFieldBounds();
	}
}
