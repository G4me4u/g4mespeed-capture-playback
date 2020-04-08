package com.g4mesoft.planner.gui;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSScreen;
import com.g4mesoft.planner.gui.timeline.GSTimelineGUI;
import com.g4mesoft.planner.module.GSPlannerModule;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.timeline.GSTrack;
import com.g4mesoft.planner.timeline.GSTrackInfo;
import com.g4mesoft.planner.util.GSColorUtil;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class GSEditTimelineGUI extends GSScreen implements GSITrackProvider {

	/* Constants for generating a new track */
	private static final String NEW_TRACK_NAME = "New Track";
	private static final int MAX_COLOR_TRIES = 5;

	private final GSTimeline timeline;
	private final GSTimelineGUI timelineGUI;

	public GSEditTimelineGUI(GSTimeline timeline, GSPlannerModule plannerModule) {
		this.timeline = timeline;

		timelineGUI = new GSTimelineGUI(timeline, this, plannerModule);
		timelineGUI.setEditable(true);
	}

	@Override
	public GSTrackInfo createNewTrackInfo() {
		return new GSTrackInfo(NEW_TRACK_NAME, getNewTrackPos(), getUniqueColor(MAX_COLOR_TRIES));
	}

	private BlockPos getNewTrackPos() {
		if (minecraft != null && minecraft.crosshairTarget != null && minecraft.crosshairTarget.getType() == HitResult.Type.BLOCK)
			return ((BlockHitResult) minecraft.crosshairTarget).getBlockPos();
		return new BlockPos(0, 0, 0);
	}

	private int getUniqueColor(int maxTries) {
		int tries = 0;

		int color = 0x000000;
		while (tries < maxTries) {
			color = (int) (Math.random() * 0xFFFFFF);

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
	public void init() {
		super.init();

		timelineGUI.initBounds(minecraft, 0, 0, width, height);
		addPanel(timelineGUI);

		setFocused(timelineGUI);
	}

	@Override
	@GSCoreOverride
	public void render(int mouseX, int mouseY, float partialTicks) {
		renderBackground();

		super.render(mouseX, mouseY, partialTicks);
	}

	@Override
	@GSCoreOverride
	public void mouseMoved(double mouseX, double mouseY) {
		timelineGUI.mouseMoved(mouseX, mouseY);
	}

	@Override
	@GSCoreOverride
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
