package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;

public class GSPlaylistOverviewPanel extends GSParentPanel {

	private static final Text PLAYLIST_TITLE = GSTextUtil.translatable("gui.tab.capture-playback.playlistTitle");

	public GSPlaylistOverviewPanel() {
		initLayout();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		GSTextLabel playlistTitle = new GSTextLabel(PLAYLIST_TITLE);
		playlistTitle.getLayout()
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.MARGIN, new GSMargin(10))
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.NORTHWEST);
		add(playlistTitle);
	}
}
