package com.g4mesoft.captureplayback.panel.playlist;

import java.util.UUID;

import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSETriggerType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistListener;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.field.GSTextField;

import net.minecraft.text.Text;

public class GSPlaylistPanel extends GSParentPanel implements GSIPlaylistListener {

	private static final int TITLE_MARGIN = 5;
	
	private static final Text[] TRIGGER_NAMES;
	private static final Text[] ENTRY_NAMES;
	
	static {
		GSETriggerType[] triggerTypes = GSETriggerType.values();
		TRIGGER_NAMES = new Text[triggerTypes.length];
		for (GSETriggerType type : triggerTypes)
			TRIGGER_NAMES[type.getIndex()] = Text.translatable("panel.playlist.trigger." + type.getName());
		GSEPlaylistEntryType[] entryTypes = GSEPlaylistEntryType.values();
		ENTRY_NAMES = new Text[entryTypes.length];
		for (GSEPlaylistEntryType type : entryTypes)
			ENTRY_NAMES[type.getIndex()] = Text.translatable("panel.playlist.entry." + type.getName());
	}
	
	private final GSPlaylist playlist;
	
	private final GSTextField nameField;
	private boolean changingName;
	
	public GSPlaylistPanel(GSPlaylist playlist) {
		this.playlist = playlist;
	
		nameField = new GSTextField();
		nameField.setBackgroundColor(0x00000000);
		nameField.setTextAlignment(GSETextAlignment.CENTER);
		nameField.setBorderWidth(0);
		nameField.setVerticalMargin(TITLE_MARGIN);
		nameField.setHorizontalMargin(0);
		
		changingName = false;
		
		initLayout();
		initEventListeners();
	}
	
	private void initLayout() {
		
	}

	private void initEventListeners() {
		nameField.addActionListener(() -> {
			onNameChanged(nameField.getText());
		});
	}
	
	private void onNameChanged(String newName) {
		changingName = true;
		playlist.setName(newName);
		changingName = false;
	}
	
	@Override
	public void playlistNameChanged(String oldName) {
		if (!changingName)
			nameField.setText(playlist.getName());
	}

	@Override
	public void triggerChanged(GSETriggerType oldType, GSIPlaylistData oldData) {
	}

	@Override
	public void entryAdded(GSPlaylistEntry entry, UUID prevUUID) {
	}

	@Override
	public void entryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
	}

	@Override
	public void entryMoved(GSPlaylistEntry entry, UUID newPrevUUID, UUID oldPrevUUID) {
	}
	
	@Override
	public void entryChanged(GSPlaylistEntry entry, GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
	}
}
