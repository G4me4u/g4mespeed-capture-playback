package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.panel.table.GSBasicTableModel;
import com.g4mesoft.ui.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.ui.panel.table.GSITableModel;
import com.g4mesoft.ui.panel.table.GSTablePanel;

import net.minecraft.text.Text;

public class GSAssetBackupPanel extends GSParentPanel {

	private static final Text ASSET_HISTORY_TITLE = Text.translatable("gui.tab.capture-playback.assetBackup");
	/* Titles for the elements shown in the backup history table */
	private static final Text DATE_TITLE = Text.translatable("gui.tab.capture-playback.date");
	private static final Text NOTE_TITLE = Text.translatable("gui.tab.capture-playback.note");
	/* Indices pointing to the column of each of the titles */
	private static final int DATE_COLUMN_INDEX;
	private static final int NOTE_COLUMN_INDEX;
	private static final Text[] TABLE_TITLES;

	/* Compute title column indices */
	static {
		int titleCount = 0;
		// Table title order is specified by order of these.
		DATE_COLUMN_INDEX = titleCount++;
		NOTE_COLUMN_INDEX = titleCount++;
		// Compute array with table titles
		TABLE_TITLES = new Text[titleCount];
		TABLE_TITLES[DATE_COLUMN_INDEX] = DATE_TITLE;
		TABLE_TITLES[NOTE_COLUMN_INDEX] = NOTE_TITLE;
	}
	
	private final GSTablePanel table;

	public GSAssetBackupPanel() {
		table = new GSTablePanel(createTableModel());

		table.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_SUBSEQUENT);
		table.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		table.setPreferredRowCount(10);

		initLayout();
	}

	private GSITableModel createTableModel() {
		GSITableModel model = new GSBasicTableModel(TABLE_TITLES.length, 0);
		// Prepare table headers
		for (int c = 0; c < TABLE_TITLES.length; c++)
			model.getColumn(c).setHeaderValue(TABLE_TITLES[c]);
		model.setRowHeaderHidden(true);

		model.getColumn(DATE_COLUMN_INDEX)
			.setMinimumWidth(110)
			.setMinimumWidth(110);
		model.getColumn(NOTE_COLUMN_INDEX)
			.setMinimumWidth(50)
			.setMinimumWidth(50);
		
		return model;
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		GSTextLabel historyTitle = new GSTextLabel(ASSET_HISTORY_TITLE);
		historyTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, 10)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST);
		add(historyTitle);
		
		GSScrollPanel scrollPanel = new GSScrollPanel(table);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(scrollPanel);
	}
}
