package com.g4mesoft.captureplayback.gui.timeline;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.text.GSITextCaret;
import com.g4mesoft.gui.text.GSITextModel;
import com.g4mesoft.gui.text.GSTextAlignment;
import com.g4mesoft.gui.text.GSTextField;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

public class GSTimelineTrackHeaderGUI extends GSParentPanel implements GSITimelineListener,
                                                                       GSITimelineModelViewListener {

	public static final int TRACK_HEADER_COLOR = 0x60000000;
	
	public static final int TRACK_HOVER_COLOR = 0x30FFFFFF;
	public static final int TRACK_SPACING_COLOR = 0xFF444444;
	
	private final GSTimeline timeline;
	private final GSTimelineModelView modelView;
	
	private double currentMouseY;
	private UUID hoveredTrackUUID;

	private final GSTextField trackNameField;

	private boolean wasFocusingText;
	private UUID editingTrackUUID;
	
	public GSTimelineTrackHeaderGUI(GSTimeline timeline, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.modelView = modelView;
	
		trackNameField = new GSTextField();
		trackNameField.setBackgroundColor(0x00000000);
		trackNameField.setTextAlignment(GSTextAlignment.CENTER);
		trackNameField.setBorderWidth(0);
		trackNameField.setVerticalMargin(0);
		trackNameField.setHorizontalMargin(0);
	}
	
	@Override
	public void init() {
		super.init();
		
		if (wasFocusingText && editingTrackUUID != null) {
			setCurrentEditingTrack(editingTrackUUID, true);
		} else {
			setCurrentEditingTrack(null, false);
		}

		wasFocusingText = false;
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();

		modelView.addModelViewListener(this);
		timeline.addTimelineListener(this);
	}
	
	@Override
	protected void onRemoved() {
		wasFocusingText = trackNameField.isElementFocused();
		
		super.onRemoved();

		modelView.removeModelViewListener(this);
		timeline.removeTimelineListener(this);
	}
	
	@Override
	public boolean isEditingText() {
		return trackNameField.isEditingText();
	}
	
	@Override
	protected void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		fill(0, 0, width, height, TRACK_HEADER_COLOR);
		fill(width - 1, 0, width, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		
		renderTrackLabels(mouseX, mouseY);

		super.renderTranslated(mouseX, mouseY, partialTicks);
	}
	
	protected void renderTrackLabels(int mouseX, int mouseY) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		((GSIBufferBuilderAccess)buffer).pushClip(0, 0, width, height);
		
		for (Map.Entry<UUID, GSTrack> trackEntry : timeline.getTrackEntries()) {
			UUID trackUUID = trackEntry.getKey();
			GSTrack track = trackEntry.getValue();
			
			int y = modelView.getTrackY(trackUUID);
			if (y + modelView.getTrackHeight() > 0 && y < height)
				renderTrackLabel(track, trackUUID, y);
			
			y += modelView.getTrackHeight();
		}

		((GSIBufferBuilderAccess)buffer).popClip();
	}
	
	private void renderTrackLabel(GSTrack track, UUID trackUUID, int y) {
		int y1 = y + modelView.getTrackHeight();
		
		if (track.getTrackUUID().equals(hoveredTrackUUID))
			fill(0, y, width, y1, TRACK_HOVER_COLOR);
		
		if (!track.getTrackUUID().equals(editingTrackUUID)) {
			String name = trimText(track.getInfo().getName(), width);
			int xt = (width - font.getStringWidth(name)) / 2;
			int yt = y + (modelView.getTrackHeight() - font.fontHeight) / 2;
			drawString(font, name, xt, yt, getTrackColor(track));
		}

		fill(0, y1, width, y1 + modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	private int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	@Override
	public void onMouseMovedGS(double mouseX, double mouseY) {
		super.onMouseMovedGS(mouseX, mouseY);

		currentMouseY = mouseY;

		updateHoveredTrack();
	}
	
	private void updateHoveredTrack() {
		hoveredTrackUUID = modelView.getTrackUUIDFromView((int)currentMouseY);

		if (!trackNameField.isElementFocused())
			setCurrentEditingTrack(hoveredTrackUUID, false);
	}

	@Override
	public boolean onMouseClickedGS(double mouseX, double mouseY, int button) {
		if (trackNameField.isElementFocused()) {
			if (!Objects.equals(hoveredTrackUUID, editingTrackUUID)) {
				updateTrackNameInfo();
				setCurrentEditingTrack(hoveredTrackUUID, true);
				
				// Only return immediately if the text field was not added
				// through the call. This will ensure that the clicked event
				// will reach the newly added text field.
				if (hoveredTrackUUID == null)
					return true;
			}
		}
		
		return super.onMouseClickedGS(mouseX, mouseY, button);
	}
	
	@Override
	public boolean onKeyPressedGS(int key, int scancode, int mods) {
		if (trackNameField.isElementFocused()) {
			switch (key) {
			case GLFW.GLFW_KEY_ESCAPE:
				setCurrentEditingTrack(null, false);
				return true;
			case GLFW.GLFW_KEY_ENTER:
				updateTrackNameInfo();
				setCurrentEditingTrack(null, false);
				return true;
			case GLFW.GLFW_KEY_TAB:
				return editNextTrack((mods & GLFW.GLFW_MOD_SHIFT) != 0);
			}
		}
		
		return super.onKeyPressedGS(key, scancode, mods);
	}
	
	private boolean editNextTrack(boolean descending) {
		if (trackNameField.isElementFocused() && editingTrackUUID != null) {
			UUID nextTrackUUID = modelView.getNextTrackUUID(editingTrackUUID, descending);
			
			updateTrackNameInfo();
			setCurrentEditingTrack(nextTrackUUID, true);

			if (nextTrackUUID != null)
				selectAllText();
			
			return true;
		}
		
		return false;
	}

	private void setCurrentEditingTrack(UUID trackUUID, boolean autoFocus) {
		if (!Objects.equals(editingTrackUUID, trackUUID)) {
			editingTrackUUID = trackUUID;

			if (editingTrackUUID != null)
				resetTrackNameField();
			
			initTrackNameField();
		}
		
		if (editingTrackUUID != null && autoFocus && !trackNameField.isElementFocused()) {
			initTrackNameField();
			setFocused(trackNameField);
		}
	}
	
	private void initTrackNameField() {
		if (editingTrackUUID != null) {
			if (!trackNameField.isAdded())
				addPanel(trackNameField);
			
			int ty = modelView.getTrackY(editingTrackUUID);
			int th = modelView.getTrackHeight();
			trackNameField.initBounds(client, 0, ty, width, th);
		} else if (trackNameField.isAdded()) {
			removePanel(trackNameField);
		}
	}
	
	private void resetTrackNameField() {
		GSTrack editingTrack = timeline.getTrack(editingTrackUUID);
		if (editingTrack != null) {
			trackNameField.setText(editingTrack.getInfo().getName());
			trackNameField.setEditableTextColor(getTrackColor(editingTrack));
		}
	}
	
	private void selectAllText() {
		GSITextModel textModel = trackNameField.getTextModel();
		GSITextCaret caret = trackNameField.getCaret();
		
		caret.setCaretDot(textModel.getLength());
		caret.setCaretMark(0);
	}
	
	private void updateTrackNameInfo() {
		String name = trackNameField.getText();

		if (name.isEmpty()) {
			resetTrackNameField();
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
	}
	
	@Override
	public void modelViewChanged() {
		updateHoveredTrack();
	}
}
