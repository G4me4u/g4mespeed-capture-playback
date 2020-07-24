package com.g4mesoft.captureplayback.gui.timeline;

import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.gui.renderer.GSIRenderer2D;

import net.minecraft.util.math.BlockPos;

public class GSTimelineInfoPanelGUI extends GSPanel {

	private static final int INFO_TEXT_COLOR = 0xFFFFFFFF;
	
	private final GSTimeline timeline;
	
	private String infoText;
	
	public GSTimelineInfoPanelGUI(GSTimeline timeline) {
		this.timeline = timeline;
	
		infoText = null;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		renderer.fillRect(0, 0, width, height, GSTimelineTrackHeaderGUI.TRACK_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
		
		if (infoText != null) {
			int ty = (height - renderer.getFontHeight() + 1) / 2;
			renderer.drawCenteredString(infoText, width / 2, ty, INFO_TEXT_COLOR);
		}
	}
	
	public void setHoveredTrackUUID(UUID hoveredTrackUUID) {
		GSTrack hoveredTrack = timeline.getTrack(hoveredTrackUUID);
		
		if (hoveredTrack != null) {
			BlockPos pos = hoveredTrack.getInfo().getPos();
			infoText = formatTrackPosition(pos);
		} else {
			infoText = null;
		}
	}
	
	private String formatTrackPosition(BlockPos pos) {
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
	}
}
