package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.captureplayback.panel.playlist.GSPlaylistPanel;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;

public class GSPlaylistOverviewPanel extends GSParentPanel {

	private static final Text PLAYLIST_TITLE = GSTextUtil.translatable("gui.tab.capture-playback.playlistTitle");

	private static final int PLAYLIST_COLUMN_COUNT = 4;
	
	private final GSClientAssetManager assetManager;
	
	private final GSPanel contentPanel;
	
	public GSPlaylistOverviewPanel(GSClientAssetManager assetManager) {
		this.assetManager = assetManager;
		
		contentPanel = new GSParentPanel();
		
		initLayout();
		initContentPanel();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		GSTextLabel playlistTitle = new GSTextLabel(PLAYLIST_TITLE);
		playlistTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.MARGIN, new GSMargin(10))
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.NORTHWEST);
		add(playlistTitle);
		
		contentPanel.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH)
			.set(GSGridLayoutManager.MARGIN, new GSMargin(5));
		add(contentPanel);
	}
	
	private void initContentPanel() {
		contentPanel.removeAll();
		// TODO: use a better layout manager...
		contentPanel.setLayoutManager(new GSGridLayoutManager());

		int playlistCount = 1;
		for (int i = 0; i < playlistCount; i++) {
			// TODO: retrieve the actual playlists...
			GSPlaylist playlist = new GSPlaylist(UUID.randomUUID(), "Playlist: Hello there");
			int gx = i % PLAYLIST_COLUMN_COUNT;
			int gy = i / PLAYLIST_COLUMN_COUNT;
			
			GSPanel panel = new GSPlaylistPanel(playlist);
			panel.getLayout()
				.set(GSGridLayoutManager.GRID_X, gx)
				.set(GSGridLayoutManager.GRID_Y, gy)
				.set(GSGridLayoutManager.FILL, GSEFill.NONE)
				.set(GSGridLayoutManager.ANCHOR, GSEAnchor.NORTHWEST)
				.set(GSGridLayoutManager.MARGIN, new GSMargin(5));

			if (i == playlistCount - 1) {
				if (gy == 0) {
					// There is only a single row. In this case the
					// last element should occupy the remaining space.
					panel.getLayout()
						.set(GSGridLayoutManager.WEIGHT_X, 1.0f);
				}
				// The last row should occupy the remaining vertical space.
				panel.getLayout()
					.set(GSGridLayoutManager.WEIGHT_Y, 1.0f);
			} else if (gx == PLAYLIST_COLUMN_COUNT - 1) {
				// The last column should occupy the remaining horizontal space.
				panel.getLayout()
					.set(GSGridLayoutManager.WEIGHT_X, 1.0f);
			}
			contentPanel.add(panel);
		}
	}
}
