package com.g4mesoft.captureplayback.panel.playlist;

import java.util.UUID;

import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSETriggerType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistListener;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSPanelContext;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.field.GSTextField;
import com.g4mesoft.ui.renderer.GSIRenderer2D;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;

public class GSPlaylistPanel extends GSParentPanel implements GSIPlaylistListener {

	private static final int TITLE_MARGIN = 5;
	private static final GSMargin ENTRY_MARGIN = new GSMargin(5);
	
	/* Visible for GSTriggerPanel and GSPlaylistEntryPanel */
	static final Text[] TRIGGER_NAMES;
	static final Text[] ENTRY_NAMES;
	
	static {
		GSETriggerType[] triggerTypes = GSETriggerType.values();
		TRIGGER_NAMES = new Text[triggerTypes.length];
		for (GSETriggerType type : triggerTypes)
			TRIGGER_NAMES[type.getIndex()] = GSTextUtil.translatable("panel.playlist.trigger." + type.getName());
		GSEPlaylistEntryType[] entryTypes = GSEPlaylistEntryType.values();
		ENTRY_NAMES = new Text[entryTypes.length];
		for (GSEPlaylistEntryType type : entryTypes)
			ENTRY_NAMES[type.getIndex()] = GSTextUtil.translatable("panel.playlist.entry." + type.getName());
	}
	
	private final GSPlaylist playlist;
	
	private final GSTextField nameField;
	private boolean changingName;
	
	private final GSTriggerPanel triggerPanel;
	private final GSPanel entriesPanel;
	
	public GSPlaylistPanel(GSPlaylist playlist) {
		this.playlist = playlist;
	
		nameField = new GSTextField();
		nameField.setBackgroundColor(0x00000000);
		nameField.setTextAlignment(GSETextAlignment.CENTER);
		nameField.setBorderWidth(0);
		nameField.setVerticalMargin(TITLE_MARGIN);
		nameField.setHorizontalMargin(0);
		changingName = false;

		triggerPanel = new GSTriggerPanel(playlist.getTrigger());
		entriesPanel = new GSParentPanel();
		
		initLayout();
		initEntriesPanel();
		initEventListeners();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		nameField.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.CENTER);
		add(nameField);
		
		triggerPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL);
		
		entriesPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(entriesPanel);
	}
	
	private void initEntriesPanel() {
		entriesPanel.removeAll();
		entriesPanel.setLayoutManager(new GSGridLayoutManager());
		
		int gy = 0;
		for (GSPlaylistEntry entry : playlist.getEntries()) {
			GSPanel panel = new GSPlaylistEntryPanel(entry);
			panel.getLayout()
				.set(GSGridLayoutManager.GRID_Y, gy++)
				.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
				.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
				.set(GSGridLayoutManager.MARGIN, ENTRY_MARGIN);
			entriesPanel.add(panel);
		}
	}

	private void initEventListeners() {
		nameField.addActionListener(() -> {
			onNameChanged(nameField.getText());
		});
	}
	
	private void onNameChanged(String newName) {
		changingName = true;
		try {
			playlist.setName(newName);
		} finally {
			changingName = false;
		}
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		// TODO: change the color...
		renderer.fillRect(0, 0, width, height, 0xFFFF00FF);
		
		super.render(renderer);
	}
	
	@Override
	public void playlistNameChanged(String oldName) {
		if (!changingName)
			nameField.setText(playlist.getName());
	}

	@Override
	public void triggerChanged(GSETriggerType oldType, GSIPlaylistData oldData) {
		// Note: handled elsewhere
	}

	@Override
	public void entryAdded(GSPlaylistEntry entry, UUID prevUUID) {
		GSPanelContext.schedule(this::initEntriesPanel);
	}

	@Override
	public void entryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
		GSPanelContext.schedule(this::initEntriesPanel);
	}

	@Override
	public void entryMoved(GSPlaylistEntry entry, UUID newPrevUUID, UUID oldPrevUUID) {
		GSPanelContext.schedule(this::initEntriesPanel);
	}
	
	@Override
	public void entryChanged(GSPlaylistEntry entry, GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
		// Note: handled elsewhere
	}
}
