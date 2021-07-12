package com.g4mesoft.captureplayback.panel.composition;

import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.panel.GSScrollableContentPanel;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionPanel extends GSScrollableContentPanel implements GSIModelViewListener,
                                                                            GSIMouseListener, GSIKeyListener {

	private static final int TRACK_HEADER_WIDTH = 90;
	private static final int COLUMN_HEADER_HEIGHT = 12;
	
	private final GSCompositionSession session;
	private final GSComposition composition;
	
	private final GSCompositionModelView modelView;
	private final GSCompositionContentPanel content;
	private final GSCompositionColumnHeaderPanel columnHeader;
	private final GSCompositionTrackHeaderPanel trackHeader;
	
	private UUID hoveredTrackUUID;
	
	public GSCompositionPanel(GSCompositionSession session, GSComposition composition) {
		this.session = session;
		this.composition = composition;
		
		modelView = new GSCompositionModelView(composition);
		modelView.addModelViewListener(this);
	
		content = new GSCompositionContentPanel(composition, modelView);
		columnHeader = new GSCompositionColumnHeaderPanel(modelView);
		trackHeader = new GSCompositionTrackHeaderPanel(composition, modelView);
	
		init();
	}

	@Override
	protected void init() {
		super.init();

		addMouseEventListener(this);
		addKeyEventListener(this);
	}
	
	@Override
	protected GSPanel getContent() {
		return content;
	}

	@Override
	protected GSPanel getColumnHeader() {
		return columnHeader;
	}

	@Override
	protected GSPanel getRowHeader() {
		return trackHeader;
	}

	@Override
	protected int getColumnHeaderHeight() {
		return COLUMN_HEADER_HEIGHT;
	}

	@Override
	protected int getRowHeaderWidth() {
		return TRACK_HEADER_WIDTH;
	}
	
	@Override
	protected void onShown() {
		super.onShown();
		
		modelView.installListeners();
		modelView.updateModelView();
		
		setXOffset(session.getXOffset());
		setYOffset(session.getYOffset());
		setOpacity(session.getOpacity());
	}
	
	@Override
	protected void onHidden() {
		super.onHidden();

		session.setXOffset(getXOffset());
		session.setYOffset(getYOffset());
		session.setOpacity(getOpacity());
		
		modelView.uninstallListeners();
	}

	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
		
		if (isVisible())
			modelView.updateModelView();
	}
	
	@Override
	public void renderTranslucent(GSIRenderer2D renderer) {
		super.renderTranslucent(renderer);

		int sw = verticalScrollBar.getWidth();
		int sh = horizontalScrollBar.getHeight();
		int cx = width - sw;
		int cy = height - sh;

		// Top left corner
		renderer.fillRect(0, 0, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, GSCompositionColumnHeaderPanel.BACKGROUND_COLOR);
		// Bottom left corner
		renderer.fillRect(0, cy, TRACK_HEADER_WIDTH, sh, GSCompositionContentPanel.BACKGROUND_COLOR);
		// Top right corner
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, GSCompositionColumnHeaderPanel.BACKGROUND_COLOR);
	}

	@Override
	protected void onXOffsetChanged(float xOffset) {
		modelView.setXOffset(Math.round(xOffset));
	}

	@Override
	protected void onYOffsetChanged(float yOffset) {
		modelView.setYOffset(Math.round(yOffset));
	}
	
	@Override
	public void modelViewChanged() {
		setXOffset(modelView.getXOffset());
		setYOffset(modelView.getYOffset());

		setContentSize(modelView.getMinimumWidth(), modelView.getMinimumHeight());
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		GSTrack hoveredTrack = modelView.getTrackFromY(event.getY());
		hoveredTrackUUID = (hoveredTrack == null) ? null : hoveredTrack.getTrackUUID();
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_T) {
			if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
				if (hoveredTrackUUID != null)
					composition.removeTrack(hoveredTrackUUID);
			} else {
				// TODO: change this a lot.
				String trackName = "Track #" + composition.getTracks().size();
				int color = 0xFF000000 | (int)(Math.random() * 0xFFFFFF);
				GSTrackGroup group = composition.addGroup("testgroup");
				composition.addTrack(trackName, color, group.getGroupUUID());
			}
		}
	}
}
