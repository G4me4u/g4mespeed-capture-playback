package com.g4mesoft.captureplayback.panel.playlist;

import com.g4mesoft.captureplayback.playlist.GSAbstractPlaylistEntry;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistEntryType;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.dropdown.GSDropdownList;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;

public abstract class GSAbstractPlaylistEntryPanel<T extends GSIPlaylistEntryType> extends GSParentPanel {

	private static final Text TYPE_TEXT = GSTextUtil.translatable("panel.playlist.type");

	private static final GSMargin FIELD_MARGIN = new GSMargin(5);
	private static final int LABEL_WIDTH = 100;
	
	private final GSAbstractPlaylistEntry<T> entry;
	
	private final GSDropdownList<T> typeField;
	
	public GSAbstractPlaylistEntryPanel(GSAbstractPlaylistEntry<T> entry, T[] types) {
		this.entry = entry;
		
		typeField = new GSDropdownList<>(types);
		typeField.setEmptySelectionAllowed(false);
		
		initLayout();
		initEventListeners();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		GSTextLabel typeLabel = new GSTextLabel(TYPE_TEXT);
		typeLabel.setTextAlignment(GSETextAlignment.LEFT);
		typeLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.MARGIN, FIELD_MARGIN)
			.set(PREFERRED_WIDTH, LABEL_WIDTH);
		add(typeLabel);
		
		typeField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.MARGIN, FIELD_MARGIN);
		add(typeField);
		
		// TODO: actually add the type depending on the chosen value...
	}
	
	private void initEventListeners() {
		
	}
}
