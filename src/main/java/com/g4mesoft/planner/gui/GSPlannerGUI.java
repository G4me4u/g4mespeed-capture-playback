package com.g4mesoft.planner.gui;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSParentGUI;
import com.g4mesoft.planner.module.GSPlannerModule;
import com.g4mesoft.planner.timeline.GSTrack;
import com.g4mesoft.planner.timeline.GSTrackInfo;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.util.GSColorUtil;

import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class GSPlannerGUI extends GSParentGUI implements GSITrackProvider {

	private static final int TOP_MARGIN = 5;
	private static final int TABLE_TOP_MARGIN = 5;
	
	private static final int POSITION_GUI_HEIGHT = 16;
	
	/* Constants for generating a new timeline */
	private static final String NEW_TRACK_NAME = "New Timeline";
	private static final int MAX_COLOR_TRIES = 5;
	
	private final GSPositionGUI positionGUI;
	private final GSTimelineGUI timelineGUI;
	private final GSTimeline timeline;
	
	public GSPlannerGUI(GSPlannerModule plannerModule) {
		super(NarratorManager.EMPTY);
	
		positionGUI = new GSPositionGUI();
		timeline = new GSTimeline();
		timelineGUI = new GSTimelineGUI(timeline, this, plannerModule);
	}
	
	@Override
	public GSTrackInfo createNewTrackInfo() {
		return new GSTrackInfo(NEW_TRACK_NAME, getNewTrackPos(), getUniqueColor(MAX_COLOR_TRIES));
	}
	
	private BlockPos getNewTrackPos() {
		if (minecraft != null && minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK)
			return ((BlockHitResult)minecraft.hitResult).getBlockPos();
		return new BlockPos(0, 0, 0);
	}
	
	private int getUniqueColor(int maxTries) {
		int tries = 0;

		int color = 0x000000;
		while (tries < maxTries) {
			color = (int)(Math.random() * 0xFFFFFF);
			
			if (isColorUnique(color))
				break;
		}
		
		return color;
	}
	
	private boolean isColorUnique(int color) {
		for (GSTrack track : timeline.getTracks()) {
			if (GSColorUtil.isRGBSimilar(track.getInfo().getColor(), color))
				return false;
		}
		
		return true;
	}
	
	@Override
	@GSCoreOverride
	public void tick() {
		super.tick();
		
		positionGUI.tick();
	}
	
	@Override
	@GSCoreOverride
	public void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);
		
		positionGUI.render(mouseX, mouseY, partialTicks);
		timelineGUI.render(mouseX, mouseY, partialTicks);
	}
	
	@Override
	@GSCoreOverride
	public void init() {
		super.init();
		
		int y = TOP_MARGIN;
		positionGUI.initBounds(minecraft, 0, y, width / 2, POSITION_GUI_HEIGHT);
		y += POSITION_GUI_HEIGHT + TABLE_TOP_MARGIN;
		timelineGUI.initBounds(minecraft, 0, y, width, height - y);

		children.add(positionGUI);
		children.add(timelineGUI);
	}
}
