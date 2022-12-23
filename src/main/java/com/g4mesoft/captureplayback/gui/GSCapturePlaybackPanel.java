package com.g4mesoft.captureplayback.gui;

import java.util.Date;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistoryListener;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSEAnchor;
import com.g4mesoft.panel.GSEFill;
import com.g4mesoft.panel.GSGridLayoutManager;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.scroll.GSScrollPanel;
import com.g4mesoft.panel.table.GSBasicTableModel;
import com.g4mesoft.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.panel.table.GSITableModel;
import com.g4mesoft.panel.table.GSTablePanel;
import com.g4mesoft.renderer.GSTexture;

import net.minecraft.util.Identifier;

public class GSCapturePlaybackPanel extends GSParentPanel implements GSIAssetHistoryListener {

	/* Icon sheet used for Capture & Playback UI elements. */
	private static final Identifier ICONS_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);
	
	private final GSCapturePlaybackClientModule module;
	
	private GSTablePanel table;

	public GSCapturePlaybackPanel(GSCapturePlaybackClientModule module) {
		this.module = module;
		
		table = new GSTablePanel(createTableModel());

		table.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_SUBSEQUENT);
		table.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		table.setPreferredRowCount(5);

		initLayout();
	}

	private GSITableModel createTableModel() {
		GSIAssetHistory history = module.getAssetHistory();
		
		int c;
		GSITableModel model = new GSBasicTableModel(5, history.size());
		c = 0;
		model.getColumn(c++).setHeaderValue("Name");
		model.getColumn(c++).setHeaderValue("Asset ID");
		model.getColumn(c++).setHeaderValue("Created");
		model.getColumn(c++).setHeaderValue("Last Modified");
		model.getColumn(c++).setHeaderValue("Type");
		model.setRowHeaderHidden(true);

		// Minimum height is captured by other column headers.
		model.getColumn(0).setMinimumSize(new GSDimension(200, 0));
		
		int r = 0;
		for (GSAssetInfo info : history) {
			c = 0;
			model.setCellValue(c++, r, info.getAssetName());
			model.setCellValue(c++, r, info.getAssetUUID().toString());
			model.setCellValue(c++, r, new Date(info.getCreatedTimestamp()));
			model.setCellValue(c++, r, new Date(info.getLastModifiedTimestamp()));
			model.setCellValue(c++, r, info.getType().toString());
			r++;
		}
		
		return model;
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		GSScrollPanel scrollPanel = new GSScrollPanel(table);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.TOP_MARGIN, 10)
			.set(GSGridLayoutManager.LEFT_MARGIN, 10)
			.set(GSGridLayoutManager.RIGHT_MARGIN, 10)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.NORTH)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		add(scrollPanel);
	}

	@Override
	protected void onShown() {
		super.onShown();

		module.getAssetHistory().addListener(this);
		// Assume history has changed
		onHistoryChanged(null);
	}

	@Override
	protected void onHidden() {
		super.onHidden();

		module.getAssetHistory().removeListener(this);
	}
	
	@Override
	public void onHistoryChanged(UUID assetUUID) {
		// Update table model to reflect changes.
		table.setModel(createTableModel());
	}
}
