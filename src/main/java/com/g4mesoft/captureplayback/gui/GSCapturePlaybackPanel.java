package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSMargin;
import com.g4mesoft.ui.panel.GSPanelContext;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.scroll.GSIScrollable;
import com.g4mesoft.ui.renderer.GSTexture;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GSCapturePlaybackPanel extends GSParentPanel implements GSIScrollable {

	/* Icon sheet used for Capture & Playback UI elements. */
	private static final Identifier ICONS_IDENTIFIER = Identifier.of("g4mespeed-capture-playback", "textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);

	/* Helper method for getting translatable text. */
	public static Text translatable(String key) {
		return GSTextUtil.translatable("gui.tab.capture-playback." + key);
	}

	public static Text translatable(String key, Object... args) {
		return GSTextUtil.translatable("gui.tab.capture-playback." + key, args);
	}
	
	private final GSAssetPermissionPanel assetPermPanel;
	private final GSAssetHistoryPanel assetHistoryPanel;
	
	private boolean compactView;
	
	public GSCapturePlaybackPanel(GSClientAssetManager assetManager) {
		assetPermPanel = new GSAssetPermissionPanel(assetManager);
		assetHistoryPanel = new GSAssetHistoryPanel(assetManager, assetPermPanel);

		// Force scroll panel to allocate exactly its own width to
		// us (note that #isScrollableWidthFilled() returns true).
		setProperty(PREFERRED_WIDTH, 0);
		
		initLayout();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		assetHistoryPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.GRID_WIDTH, 1)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.MARGIN, new GSMargin(10))
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(assetHistoryPanel);
		assetPermPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.MARGIN, new GSMargin(10))
			.set(GSGridLayoutManager.FILL, GSEFill.VERTICAL);
		add(assetPermPanel);
		// Default is compact view disabled.
		compactView = false;
	}
	
	@Override
	protected void onResized(int oldWidth, int oldHeight) {
		super.onResized(oldWidth, oldHeight);

		// Check whether there is space for asset history and
		// asset backups next to each other. Otherwise place
		// the backup panel underneath.
		long pw = (long)assetHistoryPanel.getProperty(MINIMUM_WIDTH) +
		                assetPermPanel.getProperty(MINIMUM_WIDTH);
		boolean shouldCompactView = (pw > width);
		if (compactView != shouldCompactView) {
			if (shouldCompactView) {
				assetHistoryPanel.getLayout()
					.set(GSGridLayoutManager.GRID_WIDTH, 2);
				assetPermPanel.getLayout()
					.set(GSGridLayoutManager.GRID_X, 0)
					.set(GSGridLayoutManager.GRID_Y, 1)
					.set(GSGridLayoutManager.GRID_WIDTH, 2);
			} else {
				assetHistoryPanel.getLayout()
					.set(GSGridLayoutManager.GRID_WIDTH, 1);
				assetPermPanel.getLayout()
					.set(GSGridLayoutManager.GRID_X, 1)
					.set(GSGridLayoutManager.GRID_Y, 0)
					.set(GSGridLayoutManager.GRID_WIDTH, 1);
			}
			compactView = shouldCompactView;
			// Preferred size has changed. Invalidate later,
			// since we are not allowed to invalidate here.
			GSPanelContext.schedule(this::invalidate);
		}
	}
	
	@Override
	public boolean isScrollableWidthFilled() {
		return true;
	}
}
