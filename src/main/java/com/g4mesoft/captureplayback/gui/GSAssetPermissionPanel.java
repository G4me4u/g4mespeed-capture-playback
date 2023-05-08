package com.g4mesoft.captureplayback.gui;

import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.ICONS_SHEET;
import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.translatable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCache;
import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.ui.panel.GSDimension;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSIcon;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSTexturedIcon;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.cell.GSCellContext;
import com.g4mesoft.ui.panel.cell.GSICellRenderer;
import com.g4mesoft.ui.panel.cell.GSTextCellRenderer;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.panel.table.GSBasicTableModel;
import com.g4mesoft.ui.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.ui.panel.table.GSEHeaderSelectionPolicy;
import com.g4mesoft.ui.panel.table.GSIHeaderSelectionModel;
import com.g4mesoft.ui.panel.table.GSITableColumn;
import com.g4mesoft.ui.panel.table.GSITableModel;
import com.g4mesoft.ui.panel.table.GSTablePanel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class GSAssetPermissionPanel extends GSParentPanel {

	private static final Text TITLE_TEXT = translatable("permissionTitle");
	private static final Text OWNER_TEXT = translatable("owner");
	private static final Text CREATED_BY_TEXT = translatable("createdBy");
	private static final Text COLLABORATORS_TEXT = translatable("collaborators");
	private static final Text NAME_TEXT = translatable("name");
	
	private static final GSIcon ADD_ICON          = new GSTexturedIcon(ICONS_SHEET.getRegion(38,  0, 10, 10));
	private static final GSIcon HOVERED_ADD_ICON  = new GSTexturedIcon(ICONS_SHEET.getRegion(38, 10, 10, 10));
	private static final GSIcon DISABLED_ADD_ICON = new GSTexturedIcon(ICONS_SHEET.getRegion(38, 20, 10, 10));

	private static final GSIcon REMOVE_ICON          = new GSTexturedIcon(ICONS_SHEET.getRegion(48,  0, 10, 10));
	private static final GSIcon HOVERED_REMOVE_ICON  = new GSTexturedIcon(ICONS_SHEET.getRegion(48, 10, 10, 10));
	private static final GSIcon DISABLED_REMOVE_ICON = new GSTexturedIcon(ICONS_SHEET.getRegion(48, 20, 10, 10));
	
	private static final int FIELD_MARGIN = 10;
	private static final int FIELD_WIDTH = 150;

	private final GSClientAssetManager assetManager;
	private final GSPlayerCache playerCache;
	
	private final GSTextLabel ownerField;
	private final GSTextLabel createdByField;
	private final GSTablePanel collabTable;
	private final GSButton addCollabButton;
	private final GSButton removeCollabButton;
	
	private GSAssetInfo info;
	private UUID selectedCollab;
	
	public GSAssetPermissionPanel(GSClientAssetManager assetManager) {
		this.assetManager = assetManager;
		playerCache = assetManager.getPlayerCache();
		
		ownerField = new GSTextLabel(LiteralText.EMPTY);
		ownerField.setTextAlignment(GSETextAlignment.LEFT);
		createdByField = new GSTextLabel(LiteralText.EMPTY);
		createdByField.setTextAlignment(GSETextAlignment.LEFT);
		
		collabTable = new GSTablePanel();
		collabTable.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_ALL);
		collabTable.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		collabTable.setColumnSelectionPolicy(GSEHeaderSelectionPolicy.DISABLED);
		collabTable.setRowSelectionPolicy(GSEHeaderSelectionPolicy.SINGLE_SELECTION);
		collabTable.setCellRenderer(GSCollabEntry.class, GSCollabEntryCellRenderer.INSTANCE);
		collabTable.setBorderWidth(0, 1);
		collabTable.setPreferredRowCount(10);
		collabTable.setMinimumRowHeight(16);
		
		addCollabButton = new GSButton(ADD_ICON);
		addCollabButton.setHoveredIcon(HOVERED_ADD_ICON);
		addCollabButton.setDisabledIcon(DISABLED_ADD_ICON);
		
		removeCollabButton = new GSButton(REMOVE_ICON);
		removeCollabButton.setHoveredIcon(HOVERED_REMOVE_ICON);
		removeCollabButton.setDisabledIcon(DISABLED_REMOVE_ICON);

		onCollabSelectionChanged(null);
		onInfoChanged();

		// Updated by #onCollabSelectionChanged(...)
		//updateButtonEnabled();
		
		initLayout();
		initEventListeners();
	}

	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		int gridY = 0;
		GSTextLabel title = new GSTextLabel(TITLE_TEXT);
		title.getLayout()
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, 10)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST);
		add(title);
		
		GSTextLabel ownerLabel = new GSTextLabel(OWNER_TEXT);
		ownerLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		add(ownerLabel);
		ownerField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, FIELD_WIDTH);
		add(ownerField);
		
		GSTextLabel createdByLabel = new GSTextLabel(CREATED_BY_TEXT);
		createdByLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		add(createdByLabel);
		createdByField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, FIELD_WIDTH);
		add(createdByField);
		
		GSTextLabel collaboratorsLabel = new GSTextLabel(COLLABORATORS_TEXT);
		collaboratorsLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		add(collaboratorsLabel);
		GSScrollPanel scrollPanel = new GSScrollPanel(collabTable);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH)
			// Hack: add 1 extra margin for bottom-alignment
			//       with asset history.
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN + 1)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, FIELD_WIDTH);
		add(scrollPanel);
		
		GSPanel buttonPanel = new GSParentPanel(new GSGridLayoutManager());
		buttonPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		add(buttonPanel);
		
		addCollabButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN)
			// Hack: ensures buttons are square.
			.set(PREFERRED_WIDTH, addCollabButton.getProperty(PREFERRED_HEIGHT));
		buttonPanel.add(addCollabButton);
		removeCollabButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			// Hack: see above.
			.set(PREFERRED_WIDTH, removeCollabButton.getProperty(PREFERRED_HEIGHT));
		buttonPanel.add(removeCollabButton);
	}
	
	private void initEventListeners() {
		collabTable.getRowSelectionModel().addListener(() -> {
			int selectedRow = collabTable.getRowSelectionModel().getIntervalMin();
			GSCollabEntry selectedCollab = null;
			if (selectedRow != GSIHeaderSelectionModel.INVALID_SELECTION) {
				selectedCollab = (GSCollabEntry)collabTable.getModel()
					.getCellValue(0, selectedRow);
			}
			onCollabSelectionChanged(selectedCollab != null ?
					selectedCollab.getPlayerUUID() : null);
		});
		addCollabButton.addActionListener(() -> {
			if (info != null) {
				GSPlayerPickerPanel playerPicker = GSPlayerPickerPanel.show(null);
				playerPicker.addActionListener(() -> {
					if (!playerPicker.isCanceled()) {
						assetManager.addCollaborator(info.getAssetUUID(),
								playerPicker.getSelectedPlayerUUID());
					}
				});
			}
		});
		removeCollabButton.addActionListener(() -> {
			if (info != null && selectedCollab != null) {
				assetManager.removeCollaborator(
						info.getAssetUUID(), selectedCollab);
			}
		});
	}

	private void onCollabSelectionChanged(UUID selectedCollab) {
		this.selectedCollab = selectedCollab;
		updateButtonEnabled();
	}
	
	private void updateButtonEnabled() {
		boolean enableButtons = (info != null &&
				assetManager.hasExtendedPermission(info.getAssetUUID()));
		addCollabButton.setEnabled(enableButtons);
		removeCollabButton.setEnabled(enableButtons && selectedCollab != null);
	}
	
	public void setInfo(GSAssetInfo info) {
		this.info = info;
		onInfoChanged();
	}
	
	private void onInfoChanged() {
		Text ownerText, createdByText;
		if (info == null) {
			ownerText = createdByText = LiteralText.EMPTY;
		} else {
			ownerText = playerCache.getNameText(info.getOwnerUUID());
			createdByText = playerCache.getNameText(info.getCreatedByUUID());
		}
		ownerField.setText(ownerText);
		createdByField.setText(createdByText);
		collabTable.setModel(createCollabTableModel());
		updateButtonEnabled();
	}
	
	private GSITableModel createCollabTableModel() {
		Set<UUID> collabUUIDs = (info != null) ?
				info.getCollaboratorUUIDs() : Collections.emptySet();
		
		GSITableModel model = new GSBasicTableModel(1, collabUUIDs.size());
		
		GSITableColumn nameColumn = model.getColumn(0);
		nameColumn.setHeaderValue(NAME_TEXT);
		nameColumn.setTextAlignment(GSETextAlignment.LEFT);
		
		int r = 0;
		for (UUID collabUUID : collabUUIDs) {
			GSCollabEntry entry = new GSCollabEntry(
					collabUUID, playerCache.getNameText(collabUUID));
			model.setCellValue(0, r++, entry);
		}
		
		return model;
	}
	
	private static class GSCollabEntry {
		
		private final UUID playerUUID;
		private final Text name;
		
		public GSCollabEntry(UUID playerUUID, Text name) {
			this.playerUUID = playerUUID;
			this.name = name;
		}
		
		public UUID getPlayerUUID() {
			return playerUUID;
		}
		
		public Text getName() {
			return name;
		}
	}
	
	private static class GSCollabEntryCellRenderer implements GSICellRenderer<GSCollabEntry> {

		public static final GSCollabEntryCellRenderer INSTANCE = new GSCollabEntryCellRenderer();
		
		private GSCollabEntryCellRenderer() {
		}
		
		@Override
		public void render(GSIRenderer2D renderer, GSCollabEntry value, GSCellContext context) {
			GSTextCellRenderer.INSTANCE.render(renderer, value.getName(), context);
		}

		@Override
		public GSDimension getMinimumSize(GSCollabEntry value) {
			return GSTextCellRenderer.INSTANCE.getMinimumSize(value.getName());
		}
	}
}
