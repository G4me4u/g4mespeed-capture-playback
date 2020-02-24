package com.g4mesoft.planner.module;

import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.planner.gui.GSPlannerGUI;

public class GSPlannerModule implements GSIModule {

	private static final String GUI_TAB_TITLE = "gui.tab.planner";
	
	@Override
	public void init(GSIModuleManager manager) {
		
	}
	
	@Override
	public void initGUI(GSTabbedGUI tabbedGUI) {
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSPlannerGUI());
	}
}
