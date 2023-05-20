package com.g4mesoft.captureplayback.gui;

import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.ICONS_SHEET;
import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.translatable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSDecodedAssetFile;
import com.g4mesoft.captureplayback.common.asset.GSEAssetNamespace;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistoryListener;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCache;
import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSIcon;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSTexturedIcon;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.dialog.GSFileDialog;
import com.g4mesoft.ui.panel.dialog.GSFileExtensionFilter;
import com.g4mesoft.ui.panel.dialog.GSIFileNameFilter;
import com.g4mesoft.ui.panel.field.GSTextField;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.panel.table.GSBasicTableModel;
import com.g4mesoft.ui.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.ui.panel.table.GSEHeaderSelectionPolicy;
import com.g4mesoft.ui.panel.table.GSIHeaderSelectionModel;
import com.g4mesoft.ui.panel.table.GSITableColumn;
import com.g4mesoft.ui.panel.table.GSITableModel;
import com.g4mesoft.ui.panel.table.GSTablePanel;
import com.g4mesoft.util.GSFileUtil;

import net.minecraft.text.Text;

public class GSAssetHistoryPanel extends GSParentPanel implements GSIAssetHistoryListener {

	private static final Text ASSET_HISTORY_TITLE = translatable("historyTitle");
	/* Indices pointing to the column of each of the titles */
	private static final int NAME_COLUMN_INDEX;
	private static final int OWNER_UUID_COLUMN_INDEX;
	private static final int MODIFIED_COLUMN_INDEX;
	private static final int CREATED_COLUMN_INDEX;
	private static final int NAMESPACE_COLUMN_INDEX;
	private static final int HANDLE_COLUMN_INDEX;
	private static final int TYPE_COLUMN_INDEX;
	/* Titles for the elements shown in the history table */
	private static final Text[] TABLE_TITLES;
	/* Minimum sizes of each of the columns in the history table */
	private static final int[] TABLE_WIDTHS;
	/* Text shown in place of each of the asset types */
	static final Text[] TYPE_TEXTS;
	/* Text shown in place of each of the asset namespaces */
	static final Text[] NAMESPACE_TEXTS;
	
	private static final GSIcon SEARCH_ICON          = new GSTexturedIcon(ICONS_SHEET.getRegion(27,  0, 11, 11));
	private static final GSIcon HOVERED_SEARCH_ICON  = new GSTexturedIcon(ICONS_SHEET.getRegion(27, 11, 11, 11));
	private static final GSIcon DISABLED_SEARCH_ICON = new GSTexturedIcon(ICONS_SHEET.getRegion(27, 22, 11, 11));
	
	private static final Text IMPORT_TEXT    = translatable("import");
	private static final Text EXPORT_TEXT    = translatable("export");
	private static final Text NEW_TEXT       = translatable("new");
	private static final Text EDIT_TEXT      = translatable("edit");
	private static final Text DUPLICATE_TEXT = translatable("duplicate");
	private static final Text DELETE_TEXT    = translatable("delete");
	
	private static final GSIFileNameFilter GSA_FILE_NAME_FILTER =
			new GSFileExtensionFilter(new String[] { "gsa" });
	
	private static final int SEARCH_FIELD_PREFERRED_WIDTH = 200;
	private static final int BUTTON_PREFERRED_WIDTH       = 75;
	
	/* Compute title column indices */
	static {
		int titleCount = 0;
		// Table title order is specified by order of these.
		NAME_COLUMN_INDEX       = titleCount++;
		TYPE_COLUMN_INDEX       = titleCount++;
		OWNER_UUID_COLUMN_INDEX = titleCount++;
		HANDLE_COLUMN_INDEX     = titleCount++;
		MODIFIED_COLUMN_INDEX   = titleCount++;
		CREATED_COLUMN_INDEX    = titleCount++;
		NAMESPACE_COLUMN_INDEX  = titleCount++;
		// Compute array with table titles
		TABLE_TITLES = new Text[titleCount];
		TABLE_TITLES[NAME_COLUMN_INDEX]       = translatable("name");
		TABLE_TITLES[OWNER_UUID_COLUMN_INDEX] = translatable("ownerUUID");
		TABLE_TITLES[MODIFIED_COLUMN_INDEX]   = translatable("modified");
		TABLE_TITLES[CREATED_COLUMN_INDEX]    = translatable("created");
		TABLE_TITLES[NAMESPACE_COLUMN_INDEX]  = translatable("namespace");
		TABLE_TITLES[HANDLE_COLUMN_INDEX]     = translatable("handle");
		TABLE_TITLES[TYPE_COLUMN_INDEX]       = translatable("type");
		// Compute array with minimum table widths
		TABLE_WIDTHS = new int[titleCount];
		TABLE_WIDTHS[NAME_COLUMN_INDEX]       = 100;
		TABLE_WIDTHS[OWNER_UUID_COLUMN_INDEX] = 100;
		TABLE_WIDTHS[MODIFIED_COLUMN_INDEX]   = 110;
		TABLE_WIDTHS[CREATED_COLUMN_INDEX]    = 110;
		TABLE_WIDTHS[NAMESPACE_COLUMN_INDEX]  = 60;
		TABLE_WIDTHS[HANDLE_COLUMN_INDEX]     = 140;
		TABLE_WIDTHS[TYPE_COLUMN_INDEX]       = 70;
		// Asset type value text
		GSEAssetType[] assetTypes = GSEAssetType.values();
		TYPE_TEXTS = new Text[assetTypes.length];
		for (GSEAssetType type : assetTypes)
			TYPE_TEXTS[type.getIndex()] = translatable("type." + type.getName());
		// Asset namespaces value text
		GSEAssetNamespace[] namespaces = GSEAssetNamespace.values();
		NAMESPACE_TEXTS = new Text[namespaces.length];
		for (GSEAssetNamespace namespace : namespaces)
			NAMESPACE_TEXTS[namespace.getIndex()] = translatable("namespace." + namespace.getName());
	}
	
	private final GSClientAssetManager assetManager;
	private final GSAssetPermissionPanel assetPermPanel;
	private final GSIAssetHistory history;
	private final GSPlayerCache playerCache;

	private final GSTablePanel table;
	
	private final GSTextField searchField;
	private final GSButton searchButton;
	private final GSButton importButton;
	private final GSButton exportButton;

	private final GSButton newButton;
	private final GSButton editButton;
	private final GSButton duplicateButton;
	private final GSButton deleteButton;
	
	private GSAssetHandle selectedHandle;
	
	public GSAssetHistoryPanel(GSClientAssetManager assetManager, GSAssetPermissionPanel assetPermPanel) {
		this.assetManager = assetManager;
		history = assetManager.getAssetHistory();
		playerCache = assetManager.getPlayerCache();
		this.assetPermPanel = assetPermPanel;
		
		table = new GSTablePanel(createTableModel(null));
		table.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_SUBSEQUENT);
		table.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		table.setColumnSelectionPolicy(GSEHeaderSelectionPolicy.DISABLED);
		table.setRowSelectionPolicy(GSEHeaderSelectionPolicy.SINGLE_SELECTION);
		table.setBorderWidth(0, 1);
		table.setPreferredRowCount(10);
		table.setMinimumRowHeight(16);

		searchField = new GSTextField();
		searchField.setFocusLostOnConfirm(false);
		searchButton = new GSButton(SEARCH_ICON);
		searchButton.setHoveredIcon(HOVERED_SEARCH_ICON);
		searchButton.setDisabledIcon(DISABLED_SEARCH_ICON);
		importButton = new GSButton(IMPORT_TEXT);
		exportButton = new GSButton(EXPORT_TEXT);
		
		newButton = new GSButton(NEW_TEXT);
		editButton = new GSButton(EDIT_TEXT);
		duplicateButton = new GSButton(DUPLICATE_TEXT);
		deleteButton = new GSButton(DELETE_TEXT);
		
		// Must be done after all buttons are instantiated.
		selectedHandleChanged(null);
		
		initLayout();
		initEventListeners();
	}

	private GSITableModel createTableModel(String pattern) {
		Collection<GSAssetInfo> filtered;
		if (pattern == null || pattern.isEmpty()) {
			filtered = history.asCollection();
		} else {
			filtered = new GSFilteredSet<>(new GSAssetSearchFilter(pattern));
			filtered.addAll(history.asCollection());
		}
		
		GSITableModel model = new GSBasicTableModel(TABLE_TITLES.length, filtered.size());
		// Prepare table headers
		for (int c = 0; c < TABLE_TITLES.length; c++) {
			GSITableColumn column = model.getColumn(c);
			column.setHeaderValue(TABLE_TITLES[c]);
			column.setTextAlignment(GSETextAlignment.LEFT);
			column.setMinimumWidth(TABLE_WIDTHS[c]);
			// Name column takes remainder of width
			if (c != NAME_COLUMN_INDEX)
				column.setMaximumWidth(TABLE_WIDTHS[c]);
		}
		model.setRowHeaderHidden(true);

		int r = 0;
		for (GSAssetInfo info : filtered) {
			GSEAssetNamespace namespace = info.getHandle().getNamespace();
			model.setCellValue(NAME_COLUMN_INDEX, r, info.getAssetName());
			model.setCellValue(OWNER_UUID_COLUMN_INDEX, r, playerCache.getNameText(info.getOwnerUUID()));
			model.setCellValue(CREATED_COLUMN_INDEX, r, Instant.ofEpochMilli(info.getCreatedTimestamp()));
			model.setCellValue(MODIFIED_COLUMN_INDEX, r, Instant.ofEpochMilli(info.getLastModifiedTimestamp()));
			model.setCellValue(NAMESPACE_COLUMN_INDEX, r, NAMESPACE_TEXTS[namespace.getIndex()]);
			model.setCellValue(HANDLE_COLUMN_INDEX, r, info.getHandle().toString());
			model.setCellValue(TYPE_COLUMN_INDEX, r, TYPE_TEXTS[info.getType().getIndex()]);
			r++;
		}
		
		return model;
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		// Available Assets
		//
		// ----------  ----------                 --------------------
		// | Import |  | Export |                 | Search...     | O |
		// ----------  ----------                 --------------------
		//
		// -----------------------------------------------------------
		// | NAME | OWNER |  etc.                                    |
		// -----------------------------------------------------------
		// |                                                         |
		// |                          ...                            |
		// |                                                         |
		// -----------------------------------------------------------
		//
		// -------  --------  -------------                 ----------
		// | New |  | Edit |  | Duplicate |                 | Delete |
		// -------  --------  -------------                 ----------
		
		GSTextLabel historyTitle = new GSTextLabel(ASSET_HISTORY_TITLE);
		historyTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, 10)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST);
		add(historyTitle);
		
		GSParentPanel topButtonPanel = new GSParentPanel(new GSGridLayoutManager());
		importButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		topButtonPanel.add(importButton);
		exportButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.LEFT_MARGIN, 5)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		topButtonPanel.add(exportButton);
		searchField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.VERTICAL)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(PREFERRED_WIDTH, SEARCH_FIELD_PREFERRED_WIDTH);
		topButtonPanel.add(searchField);
		searchButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 3)
			.set(GSGridLayoutManager.FILL, GSEFill.VERTICAL)
			.set(PREFERRED_WIDTH, importButton.getProperty(PREFERRED_HEIGHT));
		topButtonPanel.add(searchButton);
		topButtonPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, 10)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		add(topButtonPanel);
		
		GSScrollPanel scrollPanel = new GSScrollPanel(table);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, 10)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(scrollPanel);
		
		GSParentPanel bottomButtonPanel = new GSParentPanel(new GSGridLayoutManager());
		newButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		bottomButtonPanel.add(newButton);
		editButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.LEFT_MARGIN, 5)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		bottomButtonPanel.add(editButton);
		duplicateButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 2)
			.set(GSGridLayoutManager.LEFT_MARGIN, 5)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		bottomButtonPanel.add(duplicateButton);
		deleteButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 3)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.LEFT_MARGIN, 5)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(PREFERRED_WIDTH, BUTTON_PREFERRED_WIDTH);
		bottomButtonPanel.add(deleteButton);
		bottomButtonPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 3)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		add(bottomButtonPanel);
	}
	
	private void initEventListeners() {
		importButton.addActionListener(() -> {
			GSFileDialog dialog = GSFileDialog.showOpenDialog(null);
			dialog.setFileNameFilter(GSA_FILE_NAME_FILTER);
			dialog.addActionListener(() -> {
				if (!dialog.isCanceled())
					importAsset(dialog.getSelectedPath());
			});
		});
		exportButton.addActionListener(() -> {
			GSFileDialog dialog = GSFileDialog.showSaveDialog(null);
			dialog.setFileNameFilter(GSA_FILE_NAME_FILTER);
			dialog.addActionListener(() -> {
				if (!dialog.isCanceled())
					exportAsset(dialog.getSelectedPath());
			});
		});
		newButton.addActionListener(() -> {
			GSCreateAssetPanel.show(null, assetManager);
		});
		editButton.addActionListener(this::editSelection);
		duplicateButton.addActionListener(() -> {
			GSAssetInfo info = history.getFromHandle(selectedHandle);
			if (info != null)
				GSCreateAssetPanel.show(null, assetManager, info);
		});
		deleteButton.addActionListener(() -> {
			GSAssetInfo info = history.getFromHandle(selectedHandle);
			if (info != null)
				assetManager.deleteAsset(info.getAssetUUID());
		});
		searchField.addChangeListener(this::updateTableModel);
		searchButton.addActionListener(this::updateTableModel);
		table.addActionListener(this::editSelection);
		table.getRowSelectionModel().addListener(this::onSelectionChanged);
	}
	
	private void importAsset(Path path) {
		GSDecodedAssetFile assetFile = null;
		try {
			assetFile = GSFileUtil.readFile(path.toFile(), GSDecodedAssetFile::read);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to import asset", e);
		}
		if (assetFile != null) {
			GSCreateAssetPanel.show(null, assetManager, assetFile);
		} else {
			// TODO: show failure popup
		}
	}

	private void exportAsset(Path path) {
		GSAssetInfo info = history.getFromHandle(selectedHandle);
		if (info != null) {
			assetManager.requestAsset(info.getAssetUUID(), (assetFile) -> {
				if (assetFile == null) {
					// TODO: show failure popup (access denied)
				} else {
					try {
						GSFileUtil.writeFile(path.toFile(), assetFile, GSDecodedAssetFile::write);
					} catch (IOException e) {
						CapturePlaybackMod.GSCP_LOGGER.warn("Unable to export asset ({})",
								assetFile.getAsset().getUUID(), e);
					}
				}
			});
		} else {
			// TODO: Show failure popup
		}
	}
	
	private void editSelection() {
		GSAssetInfo info = history.getFromHandle(selectedHandle);
		if (info != null)
			assetManager.requestSession(GSESessionRequestType.REQUEST_START, info.getAssetUUID());
	}

	@Override
	protected void onShown() {
		super.onShown();

		history.addListener(this);
		// Assume history has changed
		onHistoryChanged(null);

		table.requestFocus();
	}

	@Override
	protected void onHidden() {
		super.onHidden();

		history.removeListener(this);
	}
	
	@Override
	public void onHistoryChanged(UUID assetUUID) {
		updateTableModel();
	}
	
	private void updateTableModel() {
		GSAssetHandle oldSelectedHandle = selectedHandle;
		String pattern = searchField.getText();
		pattern = StringUtils.normalizeSpace(pattern);
		table.setModel(createTableModel(pattern));
		// Either select the previously selected element, or select
		// the first row if it no longer exists.
		setSelectedHandle(oldSelectedHandle);
		if (selectedHandle == null && table.getModel().getRowCount() > 0)
			table.setSelectedRows(0, 0);
	}
	
	public void onSelectionChanged() {
		GSITableModel model = table.getModel();
		GSAssetHandle handleToSelect = null;
		int selectedRow = table.getRowSelectionModel().getIntervalMin();
		if (selectedRow >= 0 && selectedRow < model.getRowCount()) {
			// The table is filtered and out of order. Find the
			// corresponding asset in history by its handle.
			String str = (String)model.getCellValue(HANDLE_COLUMN_INDEX, selectedRow);
			try {
				handleToSelect = GSAssetHandle.fromString(str);
			} catch (IllegalArgumentException ignore) {
				// Parsing exception. Should never happen.
			}
		}
		selectedHandleChanged(handleToSelect);
	}

	private void selectedHandleChanged(GSAssetHandle handle) {
		selectedHandle = handle;
		// Disable buttons if there is no selection
		GSAssetInfo info = (handle != null) ?
				history.getFromHandle(handle) : null;
		
		boolean enableAccessButtons = (info != null &&
				assetManager.hasPermission(info.getAssetUUID()));
		exportButton.setEnabled(enableAccessButtons);
		editButton.setEnabled(enableAccessButtons);
		duplicateButton.setEnabled(enableAccessButtons);

		boolean enableDeleteButton = (info != null &&
				assetManager.hasExtendedPermission(info.getAssetUUID()));
		deleteButton.setEnabled(enableDeleteButton);
		
		assetPermPanel.setInfo(info);
	}
	
	private void setSelectedHandle(GSAssetHandle handle) {
		int rowToSelect = GSIHeaderSelectionModel.INVALID_SELECTION;
		if (handle != null) {
			String str = handle.toString();
			// Go through rows and check for handle
			GSITableModel model = table.getModel();
			for (int r = 0; r < model.getRowCount(); r++) {
				if (str.equals(model.getCellValue(HANDLE_COLUMN_INDEX, r))) {
					rowToSelect = r;
					break;
				}
			}
		}
		table.setSelectedRows(rowToSelect, rowToSelect);
		// The change event will update selectedHandle appropriately.
		//selectedHandleChanged(handle);
	}
	
	private static class GSAssetSearchFilter extends GSSearchFilter<GSAssetInfo> {

		public GSAssetSearchFilter(String pattern) {
			super(pattern);
		}

		@Override
		protected int matchCost(GSAssetInfo info) {
			if (pattern.equals(info.getType().getName())) {
				// Indication that the user searches for
				// that specific type.
				return 0;
			}
			
			int cost = Integer.MAX_VALUE;
			cost = Math.min(cost, minimumMatchCost(info.getAssetName()));
			cost = Math.min(cost, minimumMatchCost(info.getHandle().toString()));
			return cost;
		}
	}
}
