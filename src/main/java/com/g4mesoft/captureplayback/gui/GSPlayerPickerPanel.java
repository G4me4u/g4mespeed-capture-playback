package com.g4mesoft.captureplayback.gui;

import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.translatable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.g4mesoft.ui.panel.GSDimension;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSEIconAlignment;
import com.g4mesoft.ui.panel.GSEPopupPlacement;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSIActionListener;
import com.g4mesoft.ui.panel.GSIcon;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSPanelUtil;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSPopup;
import com.g4mesoft.ui.panel.GSTexturedIcon;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.cell.GSCellContext;
import com.g4mesoft.ui.panel.cell.GSICellRenderer;
import com.g4mesoft.ui.panel.event.GSKeyButtonStroke;
import com.g4mesoft.ui.panel.event.GSKeyEvent;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.panel.table.GSBasicTableModel;
import com.g4mesoft.ui.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.ui.panel.table.GSEHeaderSelectionPolicy;
import com.g4mesoft.ui.panel.table.GSIHeaderSelectionModel;
import com.g4mesoft.ui.panel.table.GSITableModel;
import com.g4mesoft.ui.panel.table.GSTablePanel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;
import com.g4mesoft.ui.renderer.GSTexture;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public class GSPlayerPickerPanel extends GSParentPanel {

	private static final Text TITLE_TEXT = translatable("playerPickerTitle");
	private static final Text CHOOSE_TEXT = translatable("choose");
	private static final Text CANCEL_TEXT = translatable("cancel");
	
	private static final GSMargin OUTER_MARGIN = new GSMargin(10);
	private static final int TITLE_MARGIN = 10;
	private static final int BUTTON_MARGIN = 5;
	
	private final GSTablePanel playerTable;
	
	private final GSButton chooseButton;
	private final GSButton cancelButton;
	
	private UUID selectedPlayerUUID;
	private boolean canceled;
	
	private List<GSIActionListener> listeners;
	
	public GSPlayerPickerPanel() {
		playerTable = new GSTablePanel(createTableModel());
		playerTable.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_SUBSEQUENT);
		playerTable.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		playerTable.setColumnSelectionPolicy(GSEHeaderSelectionPolicy.DISABLED);
		playerTable.setRowSelectionPolicy(GSEHeaderSelectionPolicy.SINGLE_SELECTION);
		playerTable.setCellRenderer(PlayerListEntry.class, GSPlayerListEntryCellRenderer.INSTANCE);
		playerTable.setPreferredRowCount(6);
		playerTable.setMinimumRowHeight(16);
		
		chooseButton = new GSButton(CHOOSE_TEXT);
		chooseButton.setEnabled(false);
		cancelButton = new GSButton(CANCEL_TEXT);

		canceled = false;
		
		listeners = new ArrayList<>();
		
		initLayout();
		initEventListeners();
	}
	
	private void initLayout() {
		//         Choose Player
		//
		// ------------------------------
		// |                          |A|
		// | ICN1    G4me4u           | |
		// | ICN2    SpaceWalker      | |
		// | ICN3    Sidney           | |
		// | ...                      | |
		// |                          |V|
		// ------------------------------
		//
		// ----------          ----------
		// | Choose |          | Cancel |
		// ----------          ----------
		
		getLayout()
			.set(GSGridLayoutManager.MARGIN, OUTER_MARGIN);
		
		setLayoutManager(new GSGridLayoutManager());
	
		GSTextLabel titleLabel = new GSTextLabel(TITLE_TEXT);
		titleLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, TITLE_MARGIN);
		add(titleLabel);
		GSScrollPanel scrollPanel = new GSScrollPanel(playerTable);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(scrollPanel);
		chooseButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN);
		add(chooseButton);
		cancelButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN);
		add(cancelButton);
		
		setProperty(PREFERRED_WIDTH, 150);
	}
	
	private void initEventListeners() {
		playerTable.getRowSelectionModel().addListener(() -> {
			chooseButton.setEnabled(playerTable.hasSelection());
		});
		chooseButton.addActionListener(this::chooseAndHide);
		cancelButton.addActionListener(this::cancelAndHide);
		playerTable.addActionListener(this::chooseAndHide);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ENTER), this::chooseAndHide);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ESCAPE), this::hide);
	}
	
	private void chooseAndHide() {
		int sr = playerTable.getRowSelectionModel().getIntervalMin();
		if (sr != GSIHeaderSelectionModel.INVALID_SELECTION) {
			GSITableModel model = playerTable.getModel();
			PlayerListEntry entry = (PlayerListEntry)model.getCellValue(0, sr);
			//assert(selectedEntry != null)
			selectedPlayerUUID = entry.getProfile().getId();
			canceled = false;
			hide();
			dispatchActionPerformed();
		}
	}
	
	private void cancelAndHide() {
		selectedPlayerUUID = null;
		canceled = true;
		hide();
		dispatchActionPerformed();
	}
	
	private GSITableModel createTableModel() {
		ClientPlayNetworkHandler networkHandler =
				MinecraftClient.getInstance().getNetworkHandler();
		if (networkHandler == null) {
			// We are not currently in a world.
			return new GSBasicTableModel(0, 0);
		}
		List<PlayerListEntry> entries = new ArrayList<>(networkHandler.getPlayerList());
		Collections.sort(entries, (lhs, rhs) -> {
			String n0 = lhs.getProfile().getName();
			String n1 = rhs.getProfile().getName();
			return n0.compareToIgnoreCase(n1);
		});
		GSITableModel model = new GSBasicTableModel(1, entries.size());
		model.getColumn(0).setTextAlignment(GSETextAlignment.LEFT);
		model.setColumnHeaderHidden(true);
		model.setRowHeaderHidden(true);
		int r = 0;
		for (PlayerListEntry entry : entries)
			model.setCellValue(0, r++, entry);
		return model;
	}
	
	public void addActionListener(GSIActionListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null");
		listeners.add(listener);
	}
	
	public void removeActionListener(GSIActionListener listener) {
		listeners.remove(listener);
	}
	
	private void dispatchActionPerformed() {
		listeners.forEach(GSIActionListener::actionPerformed);
	}
	
	public UUID getSelectedPlayerUUID() {
		return selectedPlayerUUID;
	}

	public boolean isCanceled() {
		return canceled;
	}
	
	public static GSPlayerPickerPanel show(GSPanel source) {
		GSPlayerPickerPanel panel = new GSPlayerPickerPanel();
		GSPopup popup = new GSPopup(panel, true);
		popup.setHiddenOnFocusLost(false);
		popup.setSourceFocusedOnHide(source != null);
		popup.show(source, 0, 0, GSEPopupPlacement.CENTER);
		return panel;
	}
	
	public void hide() {
		GSPanel parent = getParent();
		if (parent instanceof GSPopup)
			((GSPopup)parent).hide();
	}
	
	private static class GSPlayerListEntryCellRenderer implements GSICellRenderer<PlayerListEntry> {

		private static final int ICON_SPACING = 5;
		private static final int OUTER_MARGIN = 2;
		
		private static final GSPlayerListEntryCellRenderer INSTANCE = new GSPlayerListEntryCellRenderer();
		
		private GSPlayerListEntryCellRenderer() {
		}
		
		@Override
		public void render(GSIRenderer2D renderer, PlayerListEntry value, GSCellContext context) {
			// Shrink bounds to allow outer margin.
			context.bounds.x += OUTER_MARGIN;
			context.bounds.width -= 2 * OUTER_MARGIN;
			GSPanelUtil.drawLabel(renderer, getIcon(value), ICON_SPACING, getNameAsText(value),
					context.textColor, false, GSEIconAlignment.LEFT, context.textAlignment, context.bounds);
		}

		@Override
		public GSDimension getMinimumSize(PlayerListEntry value) {
			return GSPanelUtil.labelPreferredSize(getIcon(value), getNameAsText(value), ICON_SPACING);
		}
		
		private Text getNameAsText(PlayerListEntry value) {
			Text displayText = value.getDisplayName();
			if (displayText != null)
				return displayText;
			return GSTextUtil.literal(value.getProfile().getName());
		}
		
		private GSIcon getIcon(PlayerListEntry value) {
			// See PlayerListHud#render(...) for magic constants.
			GSTexture texture = new GSTexture(value.getSkinTexture(), 64, 64);
			return new GSTexturedIcon(texture.getRegion(8, 8, 8, 8));
		}
	}
}
