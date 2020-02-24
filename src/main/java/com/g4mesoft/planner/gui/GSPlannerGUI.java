package com.g4mesoft.planner.gui;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSParentGUI;
import com.g4mesoft.planner.timeline.GSBlockEventTime;
import com.g4mesoft.planner.timeline.GSETimelineEntryType;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.timeline.GSTimelineEntry;
import com.g4mesoft.planner.timeline.GSTimelineInfo;
import com.g4mesoft.planner.timeline.GSTimelineTable;

import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.math.BlockPos.Mutable;

public class GSPlannerGUI extends GSParentGUI {

	private static final int TOP_MARGIN = 5;
	private static final int TABLE_TOP_MARGIN = 5;
	
	private static final int POSITION_GUI_HEIGHT = 16;
	
	private final GSPositionGUI positionGUI;
	private final GSTimelineTableGUI timelineTableGUI;
	
	public GSPlannerGUI() {
		super(NarratorManager.EMPTY);
	
		positionGUI = new GSPositionGUI();
		
		GSTimelineTable table = new GSTimelineTable();
		GSTimeline t1 = table.addTimeline(new GSTimelineInfo("Piston #1", new Mutable(), 0xFF22FF));
		GSTimeline t2 = table.addTimeline(new GSTimelineInfo("Piston #2", new Mutable(), 0x22FFFF));
		GSTimeline t3 = table.addTimeline(new GSTimelineInfo("Piston #3", new Mutable(), 0x2222FF));
		
		GSTimelineEntry e1 = new GSTimelineEntry(new GSBlockEventTime(0, 0), new GSBlockEventTime(4, 1));
		e1.setType(GSETimelineEntryType.EVENT_END);
		t1.tryAddEntry(e1);
		t1.tryAddEntry(new GSTimelineEntry(new GSBlockEventTime(7, 2), new GSBlockEventTime(9, 2)));

		t2.tryAddEntry(new GSTimelineEntry(new GSBlockEventTime(1, 3), new GSBlockEventTime(2, 1)));
		t2.tryAddEntry(new GSTimelineEntry(new GSBlockEventTime(5, 2), new GSBlockEventTime(5, 8)));
		t2.tryAddEntry(new GSTimelineEntry(new GSBlockEventTime(13, 2), new GSBlockEventTime(15, 1)));

		t3.tryAddEntry(new GSTimelineEntry(new GSBlockEventTime(2, 2), new GSBlockEventTime(11, 2)));
		
		timelineTableGUI = new GSTimelineTableGUI(table);
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
		timelineTableGUI.render(mouseX, mouseY, partialTicks);
	}
	
	@Override
	@GSCoreOverride
	public void init() {
		super.init();
		
		int y = TOP_MARGIN;
		positionGUI.initBounds(minecraft, 0, y, width / 2, POSITION_GUI_HEIGHT);
		y += POSITION_GUI_HEIGHT + TABLE_TOP_MARGIN;
		timelineTableGUI.initBounds(minecraft, 0, y, width, height - y);

		children.add(positionGUI);
		children.add(timelineTableGUI);
	}
}
