package com.g4mesoft.planner.module;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSKeyBinding;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.planner.gui.GSPlannerGUI;
import com.g4mesoft.planner.timeline.GSTimeline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class GSPlannerModule implements GSIModule {

	private static final String GUI_TAB_TITLE = "gui.tab.planner";
	
	public static final String KEY_CATEGORY = "Planner";
	
	@Environment(EnvType.CLIENT)
	private GSKeyBinding collapseTabKey;
	@Environment(EnvType.CLIENT)
	private GSKeyBinding expandedHoveredTabKey;
	@Environment(EnvType.CLIENT)
	private GSTimeline activeTimeline;
	
	@Override
	public void init(GSIModuleManager manager) {
		// TODO: change this to a bank of timelines. This will be the same on
		// both server and client. The server will send all timelines when the
		// client connects. This ensures that the two are synced. The client can
		// then later override the timelines on the server. Timelines should be
		// per world, but it should also be allowed to copy them outside each world
		// to ensure easy access to them in singleplayer etc.
		manager.runOnClient(managerClient -> activeTimeline = new GSTimeline());
	}
	
	@Override
	public void initGUI(GSTabbedGUI tabbedGUI) {
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSPlannerGUI(this));
	}
	
	@Override
	public void registerHotkeys(GSKeyManager keyManager) {
		collapseTabKey = keyManager.registerKey("collapseTab", KEY_CATEGORY, GLFW.GLFW_KEY_T);
		expandedHoveredTabKey = keyManager.registerKey("expandedHoveredTab", KEY_CATEGORY, GLFW.GLFW_KEY_T);
	}
	
	@Environment(EnvType.CLIENT)
	public GSKeyBinding getCollapseTabKey() {
		return collapseTabKey;
	}

	@Environment(EnvType.CLIENT)
	public GSKeyBinding getExpandHoveredTabKey() {
		return expandedHoveredTabKey;
	}
	
	@Environment(EnvType.CLIENT)
	public GSTimeline getActiveTimeline() {
		return activeTimeline;
	}
}
