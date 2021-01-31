package com.g4mesoft.captureplayback.panel.sequence;

import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.util.math.BlockPos;

public class GSSequenceInfoPanel extends GSPanel {

	private static final int INFO_TEXT_COLOR = 0xFFE0E0E0;
	
	private final GSSequence sequence;
	
	private String infoText;
	
	public GSSequenceInfoPanel(GSSequence sequence) {
		this.sequence = sequence;
	
		infoText = null;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		renderer.fillRect(0, 0, width, height, GSSequenceChannelHeaderPanel.CHANNEL_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSSequenceContentPanel.CHANNEL_SPACING_COLOR);
		
		if (infoText != null) {
			int ty = (height - renderer.getTextHeight() + 1) / 2;
			renderer.drawCenteredText(infoText, width / 2, ty, INFO_TEXT_COLOR);
		}
	}
	
	public void setHoveredChannelUUID(UUID hoveredChannelUUID) {
		GSChannel hoveredChannel = sequence.getChannel(hoveredChannelUUID);
		
		if (hoveredChannel != null) {
			BlockPos pos = hoveredChannel.getInfo().getPos();
			infoText = formatChannelPosition(pos);
		} else {
			infoText = null;
		}
	}
	
	private String formatChannelPosition(BlockPos pos) {
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
	}
}
