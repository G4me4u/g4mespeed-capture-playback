package com.g4mesoft.planner.gui;

import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.planner.module.GSPlannerModule;

import net.minecraft.client.gui.widget.ButtonWidget;

public class GSPlannerGUI extends GSPanel {

	private static final int TOP_MARGIN = 5;
	private static final int POSITION_GUI_HEIGHT = 16;

	private final GSPlannerModule plannerModule;
	
	private final GSPositionGUI positionGUI;
	
	public GSPlannerGUI(GSPlannerModule plannerModule) {
		this.plannerModule = plannerModule;
		
		positionGUI = new GSPositionGUI();
	}
	
	@Override
	public void init() {
		super.init();
		
		int y = TOP_MARGIN;
		positionGUI.initBounds(client, 0, y, width / 2, POSITION_GUI_HEIGHT);
		addPanel(positionGUI);
		y += POSITION_GUI_HEIGHT + 5;

		addWidget(new ButtonWidget(width / 2 - 45, y, 90, 20, "Edit timeline", (button) -> {
			client.openScreen(new GSEditTimelineGUI(plannerModule.getActiveTimeline(), plannerModule));
		}));
	}
}
