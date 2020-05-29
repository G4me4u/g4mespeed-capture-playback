package com.g4mesoft.captureplayback.gui.timeline;

import java.util.Map;
import java.util.UUID;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.gui.GSPanel;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

public class GSTimelineTrackHeaderGUI extends GSPanel {

	public static final int TRACK_HEADER_COLOR = 0x60000000;
	
	public static final int TRACK_HOVER_COLOR = 0x30FFFFFF;
	public static final int TRACK_SPACING_COLOR = 0xFF444444;
	
	private final GSTimeline timeline;
	private final GSTimelineModelView modelView;
	
	public GSTimelineTrackHeaderGUI(GSTimeline timeline, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.modelView = modelView;
	}
	
	@Override
	protected void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);

		fill(0, 0, width, height, TRACK_HEADER_COLOR);
		fill(width - 1, 0, width, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		
		renderTrackLabels(mouseX, mouseY);
	}
	
	protected void renderTrackLabels(int mouseX, int mouseY) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		((GSIBufferBuilderAccess)buffer).pushClip(0, 0, width, height);
		
		UUID hoveredTrackUUID = modelView.getTrackUUIDFromView(mouseY);
		for (Map.Entry<UUID, GSTrack> trackEntry : timeline.getTrackEntries()) {
			UUID trackUUID = trackEntry.getKey();
			GSTrack track = trackEntry.getValue();
			
			int y = modelView.getTrackY(trackUUID);
			if (y + modelView.getTrackHeight() > 0 && y < height)
				renderTrackLabel(track, trackUUID, y, track.getTrackUUID().equals(hoveredTrackUUID));
			y += modelView.getTrackHeight();
		}

		((GSIBufferBuilderAccess)buffer).popClip();
	}
	
	private void renderTrackLabel(GSTrack track, UUID trackUUID, int y, boolean trackHovered) {
		int y1 = y + modelView.getTrackHeight();
		
		if (trackHovered)
			fill(0, y, width, y1, TRACK_HOVER_COLOR);
		
		String name = trimText(track.getInfo().getName(), width);
		int xt = (width - font.getStringWidth(name)) / 2;
		int yt = y + (modelView.getTrackHeight() - font.fontHeight) / 2;
		drawString(font, name, xt, yt, getTrackColor(track));

		fill(0, y1, width, y1 + modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	private int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
}
