package com.g4mesoft.captureplayback.gui;

import java.time.Instant;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistoryListener;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSEAnchor;
import com.g4mesoft.panel.GSEFill;
import com.g4mesoft.panel.GSGridLayoutManager;
import com.g4mesoft.panel.GSMargin;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.field.GSTextLabel;
import com.g4mesoft.panel.scroll.GSScrollPanel;
import com.g4mesoft.panel.table.GSBasicTableModel;
import com.g4mesoft.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.panel.table.GSITableModel;
import com.g4mesoft.panel.table.GSTablePanel;
import com.g4mesoft.renderer.GSTexture;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class GSCapturePlaybackPanel extends GSParentPanel implements GSIAssetHistoryListener {

	/* Icon sheet used for Capture & Playback UI elements. */
	private static final Identifier ICONS_IDENTIFIER = new Identifier("g4mespeed", "captureplayback/textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);
	
	/* Titles for the elements shown in the asset history table */
	private static final Text NAME_TITLE          = new TranslatableText("gui.tab.capture-playback.name");
	private static final Text ASSET_UUID_TITLE    = new TranslatableText("gui.tab.capture-playback.assetUUID");
	private static final Text OWNER_UUID_TITLE    = new TranslatableText("gui.tab.capture-playback.ownerUUID");
	private static final Text MODIFIED_TITLE      = new TranslatableText("gui.tab.capture-playback.modified");
	private static final Text CREATED_TITLE       = new TranslatableText("gui.tab.capture-playback.created");
	private static final Text TYPE_TITLE          = new TranslatableText("gui.tab.capture-playback.type");
	/* Indices pointing to the column of each of the titles */
	private static final int NAME_COLUMN_INDEX;
	private static final int ASSET_UUID_COLUMN_INDEX;
	private static final int OWNER_UUID_COLUMN_INDEX;
	private static final int MODIFIED_COLUMN_INDEX;
	private static final int CREATED_COLUMN_INDEX;
	private static final int TYPE_COLUMN_INDEX;
	private static final Text[] TABLE_TITLES;
	/* Text shown in place of each of the asset types */
	private static final Text COMPOSITION_TEXT = new TranslatableText("gui.tab.capture-playback.type.composition");
	private static final Text SEQUENCE_TEXT    = new TranslatableText("gui.tab.capture-playback.type.sequence");

	/* Content titles */
	private static final Text ASSET_HISTORY_TITLE = new TranslatableText("gui.tab.capture-playback.historyTitle");
	private static final Text PLAYLIST_TITLE = new TranslatableText("gui.tab.capture-playback.playlistTitle");
	private static final GSMargin TITLE_MARGIN = new GSMargin(10);
	private static final GSMargin TABLE_MARGIN = new GSMargin(0, 10);

	/* Compute title column indices */
	static {
		int titleCount = 0;
		// Table title order is specified by order of these.
		NAME_COLUMN_INDEX       = titleCount++;
		ASSET_UUID_COLUMN_INDEX = titleCount++;
		OWNER_UUID_COLUMN_INDEX = titleCount++;
		MODIFIED_COLUMN_INDEX   = titleCount++;
		CREATED_COLUMN_INDEX    = titleCount++;
		TYPE_COLUMN_INDEX       = titleCount++;
		// Compute array with table titles
		TABLE_TITLES = new Text[titleCount];
		TABLE_TITLES[NAME_COLUMN_INDEX]       = NAME_TITLE;
		TABLE_TITLES[ASSET_UUID_COLUMN_INDEX] = ASSET_UUID_TITLE;
		TABLE_TITLES[OWNER_UUID_COLUMN_INDEX] = OWNER_UUID_TITLE;
		TABLE_TITLES[MODIFIED_COLUMN_INDEX]   = MODIFIED_TITLE;
		TABLE_TITLES[CREATED_COLUMN_INDEX]    = CREATED_TITLE;
		TABLE_TITLES[TYPE_COLUMN_INDEX]       = TYPE_TITLE;
	}
	
	private final GSCapturePlaybackClientModule module;
	
	private GSTablePanel table;

	public GSCapturePlaybackPanel(GSCapturePlaybackClientModule module) {
		this.module = module;
		
		table = new GSTablePanel(createTableModel());

		table.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_SUBSEQUENT);
		table.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		table.setPreferredRowCount(10);

		initLayout();
	}

	private GSITableModel createTableModel() {
		GSIAssetHistory history = module.getAssetHistory();
		
		GSITableModel model = new GSBasicTableModel(TABLE_TITLES.length, history.size());
		// Prepare table headers
		for (int c = 0; c < TABLE_TITLES.length; c++)
			model.getColumn(c).setHeaderValue(TABLE_TITLES[c]);
		model.setRowHeaderHidden(true);

		model.getColumn(MODIFIED_COLUMN_INDEX).setMaximumSize(new GSDimension(100, 0));
		model.getColumn(CREATED_COLUMN_INDEX).setMaximumSize(new GSDimension(100, 0));
		model.getColumn(TYPE_COLUMN_INDEX).setMaximumSize(new GSDimension(70, 0));
		
		int r = 0;
		for (GSAssetInfo info : history) {
			model.setCellValue(NAME_COLUMN_INDEX, r, info.getAssetName());
			model.setCellValue(ASSET_UUID_COLUMN_INDEX, r, info.getAssetUUID().toString());
			model.setCellValue(OWNER_UUID_COLUMN_INDEX, r, info.getOwnerUUID().toString());
			model.setCellValue(CREATED_COLUMN_INDEX, r, Instant.ofEpochMilli(info.getCreatedTimestamp()));
			model.setCellValue(MODIFIED_COLUMN_INDEX, r, Instant.ofEpochMilli(info.getLastModifiedTimestamp()));
			model.setCellValue(TYPE_COLUMN_INDEX, r, assetTypeToText(info.getType()));
			r++;
		}
		
		return model;
	}
	
	private Text assetTypeToText(GSEAssetType type) {
		switch (type) {
		case COMPOSITION:
			return COMPOSITION_TEXT;
		case SEQUENCE:
			return SEQUENCE_TEXT;
		}
		throw new IllegalStateException("Unknown asset type: " + type);
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		int gridY = 0;
		
		GSTextLabel historyTitle = new GSTextLabel(ASSET_HISTORY_TITLE);
		historyTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.MARGIN, TITLE_MARGIN)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST);
		add(historyTitle);
		
		GSScrollPanel scrollPanel = new GSScrollPanel(table);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.MARGIN, TABLE_MARGIN)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		add(scrollPanel);
		
		GSTextLabel playlistTitle = new GSTextLabel(PLAYLIST_TITLE);
		playlistTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.MARGIN, TITLE_MARGIN)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.NORTHWEST);
		add(playlistTitle);
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
