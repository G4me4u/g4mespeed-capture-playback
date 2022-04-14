package com.g4mesoft.captureplayback.panel.composition;

import java.util.Iterator;

import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSPanelUtil;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.util.GSColorUtil;
import com.g4mesoft.util.GSMathUtil;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Util;

public class GSCompositionPanel extends GSPanel implements GSIMouseListener, GSIKeyListener,
                                                           GSIFocusEventListener, GSIScrollable,
                                                           GSIModelViewListener {

	public static final int BACKGROUND_COLOR    = 0xFF333333;
	public static final int TRACK_SPACING_COLOR = 0xFF222222;
	
	private static final int ENTRY_TITLE_HEIGHT = 10;
	private static final int ENTRY_TITLE_LEFT_MARGIN = 2;
	
	private static final int ENTRY_TITLE_BG_ALPHA = 0xE0;
	private static final int ENTRY_PREVIEW_BG_ALPHA = 0x80;
	
	private static final double NORMAL_ZOOM_SPEED = 1.1;
	private static final double SLOW_ZOOM_SPEED = 1.05;
	
	private static final int DOTTED_LINE_LENGTH = 3;
	private static final int DOTTED_LINE_SPACING = 3;
	private static final int TIME_INDICATOR_COLOR = 0xFF222222;
	
//	private static final int SELECTION_BACKGROUND_COLOR = 0x80222222;
//	private static final int SELECTION_BORDER_COLOR = 0x80111111;
//	private static final int SELECTION_BACKGROUND_COLOR = 0x80888888;
//	private static final int SELECTION_BORDER_COLOR = 0x80AAAAAA;
	private static final int SELECTION_BACKGROUND_COLOR = 0x40EEEEEE;
	private static final int SELECTION_BORDER_COLOR = 0x40FFFFFF;

	private static final long SLOW_INCREMENTAL_DELTA = 1L;
	private static final long FAST_INCREMENTAL_DELTA = 5L;
	
	private static final long DOUBLE_CLICK_TIME = 500L;
	private static final long ADD_DELETE_ENTRY_CLICK_TIME = 250L;
	
	/* Offset on each side of a zero tick relative to game-tick width */
	private static final double RELATIVE_ZT_MARGIN = 0.35;
	
	private final GSComposition composition;
	private final GSCompositionModelView modelView;
	
	private final GSITrackEntrySelectionModel selectionModel;
	
	private int draggingMouseX;
	private boolean draggingEntry;
	
	private double selectionStartGametick;
	private int selectionStartY;
	private int selectionEndX;
	private int selectionEndY;
	private boolean selectingEntries;
	
	private GSTrackEntry clickedEntry;
	private GSTrack clickedTrack;
	private long leftClickTime;
	private int leftClickCount;
	
	private boolean editable;
	
	public GSCompositionPanel(GSComposition composition, GSCompositionModelView modelView) {
		this.composition = composition;
		this.modelView = modelView;
		
		selectionModel = new GSTrackEntrySelectionModel(composition);
		
		// Editable by default;
		editable = true;
		
		addMouseEventListener(this);
		addKeyEventListener(this);
		addFocusEventListener(this);
	}
	
	@Override
	protected void onShown() {
		super.onShown();

		resetMouseActions();

		modelView.addModelViewListener(this);
	}

	@Override
	protected void onHidden() {
		super.onHidden();

		modelView.removeModelViewListener(this);
	}
	
	private void resetMouseActions() {
		draggingEntry = false;
		
		selectingEntries = false;
		
		leftClickTime = Util.getMeasuringTimeMs();
		leftClickCount = 0;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		GSRectangle bounds = renderer.getClipBounds().intersection(0, 0, width, height);
		
		renderer.build(GSIRenderer2D.QUADS, VertexFormats.POSITION_COLOR);
		renderBackground(renderer, bounds);
		renderTimeIndicators(renderer, bounds);
		renderer.finish();
		
		for (GSTrack track : composition.getTracks())
			renderTrack(renderer, track, bounds);

		renderSelection(renderer, bounds);
	}
	
	private void renderBackground(GSIRenderer2D renderer, GSRectangle bounds) {
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, BACKGROUND_COLOR);
	}
	
	private void renderTimeIndicators(GSIRenderer2D renderer, GSRectangle bounds) {
		long interval = modelView.getTimeIndicatorInterval();
		long subInterval = interval / GSCompositionModelView.TIME_SUB_INDICATOR_COUNT;
		int sy = bounds.y - (bounds.y % (DOTTED_LINE_LENGTH + DOTTED_LINE_SPACING));
		
		long gt = modelView.getTimeIndicatorFromX(bounds.x);
		int x = modelView.getGametickX(gt);
		while (x - bounds.x < bounds.width) {
			renderer.drawVLine(x, bounds.y, bounds.y + bounds.height, TIME_INDICATOR_COLOR);
			
			if (subInterval != 0) {
				for (long j = subInterval; j < interval; j += subInterval) {
					int sx = modelView.getGametickX(gt + j);
					renderer.drawDottedVLine(sx, sy, bounds.y + bounds.height,
							DOTTED_LINE_LENGTH, DOTTED_LINE_SPACING, TIME_INDICATOR_COLOR);
				}
			}
			
			gt += interval;
			x = modelView.getGametickX(gt);
		}
	}
	
	private void renderTrack(GSIRenderer2D renderer, GSTrack track, GSRectangle bounds) {
		int th = modelView.getTrackHeight();
		int sy = modelView.getTrackY(track);
		
		if (sy + th >= bounds.y && sy - bounds.y < bounds.height) {
			for (GSTrackEntry entry : track.getEntries())
				renderEntry(renderer, entry, track.getColor(), bounds);
			
			renderer.fillRect(bounds.x, sy + th, bounds.width, modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
		}
	}
	
	private void renderEntry(GSIRenderer2D renderer, GSTrackEntry entry, int color, GSRectangle bounds) {
		if (selectionModel.isSelected(entry))
			color = 0xFF00FF00;

		GSTrack track = entry.getParent();
		GSRectangle entryBounds = modelView.modelToView(entry);
		
		if (track != null && entryBounds != null && entryBounds.intersects(bounds)) {
			int titleColor = GSIRenderer.brightenColor(color);
			int titleBgColor = GSColorUtil.withAlpha(GSIRenderer.darkenColor(color), ENTRY_TITLE_BG_ALPHA);

			String title = renderer.trimString(track.getSequence().getName(), entryBounds.width - 4);
			int tx = entryBounds.x + ENTRY_TITLE_LEFT_MARGIN;
			int ty = entryBounds.y + (ENTRY_TITLE_HEIGHT - renderer.getTextAscent()) / 2;
			
			renderer.fillRect(entryBounds.x, entryBounds.y, entryBounds.width, ENTRY_TITLE_HEIGHT, titleBgColor);
			renderer.drawTextNoStyle(title, tx, ty, titleColor, false);

			entryBounds.y += ENTRY_TITLE_HEIGHT;
			entryBounds.height -= ENTRY_TITLE_HEIGHT;
			
			renderSequencePreview(renderer, entry, entryBounds, color);
		}
	}
	
	private void renderSequencePreview(GSIRenderer2D renderer, GSTrackEntry entry, GSRectangle bounds, int color) {
		int darkColor = GSIRenderer.darkenColor(color);
		int previewBgColor = GSColorUtil.withAlpha(GSIRenderer.darkenColor(darkColor), ENTRY_PREVIEW_BG_ALPHA);
		
		renderer.build(GSIRenderer2D.QUADS, VertexFormats.POSITION_COLOR);
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, previewBgColor);

		// The width of a zero tick (use double game-tick width to have a uniform width).
		int ztw = (int)Math.round(modelView.getGametickWidth() * (1.0 - 2.0 * RELATIVE_ZT_MARGIN));
		
		GSTrack track = entry.getParent();
		if (track != null) {
			int entryColor = GSColorUtil.withAlpha(darkColor, ENTRY_TITLE_BG_ALPHA);
			
			Iterator<GSChannel> channelItr = track.getSequence().getChannels().iterator();
			for (int yo = 1; yo < bounds.height && channelItr.hasNext(); yo += 2) {
				GSChannel channel = channelItr.next();
				
				for (GSChannelEntry channelEntry : channel.getEntries()){
					long gt0 = channelEntry.getStartTime().getGametick() + entry.getOffset();
					long gt1 = channelEntry.getEndTime().getGametick() + entry.getOffset();
	
					int x0, x1;
					if (gt0 == gt1) {
						// Handle zero ticks differently (since they would otherwise not be rendered).
						x0 = modelView.getGametickX(gt0) + (modelView.getGametickWidth(gt0) - ztw) / 2;
						x1 = Math.min(x0 + ztw, modelView.getGametickX(gt0 + 1L));
					} else {
						// Sequence entries are shown in the middle of each tick.
						x0 = modelView.getGametickX(gt0) + modelView.getGametickWidth(gt0) / 2;
						x1 = modelView.getGametickX(gt1) + modelView.getGametickWidth(gt1) / 2;
					}
					
					if (x1 - x0 <= 0) {
						// We should always render the entries.
						if (x1 - bounds.x > bounds.width) {
							x0 = bounds.x + bounds.width - 1;
						} else if (x0 < bounds.x) {
							x0 = bounds.x;
						}
						
						 x1 = x0 + 1;
					}
					
					renderer.drawHLine(x0, x1, bounds.y + yo, entryColor);
				}
			}
		}
		
		renderer.finish();
	}
	
	private void renderSelection(GSIRenderer2D renderer, GSRectangle bounds) {
		if (selectingEntries) {
			int mx0 = modelView.getGametickExactX(selectionStartGametick);
			int my0 = selectionStartY;
			
			int sx0 = Math.min(mx0, selectionEndX);
			int sy0 = Math.min(my0, selectionEndY);
			int sx1 = Math.max(mx0, selectionEndX);
			int sy1 = Math.max(my0, selectionEndY);
			
			renderer.fillRect(sx0, sy0, sx1 - sx0, sy1 - sy0, SELECTION_BACKGROUND_COLOR);
			renderer.drawRect(sx0, sy0, sx1 - sx0, sy1 - sy0, SELECTION_BORDER_COLOR);
		}
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		return new GSDimension(modelView.getMinimumWidth(), modelView.getMinimumHeight());
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (!event.isConsumed() && !selectingEntries && !draggingEntry) {
			if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
				long now = Util.getMeasuringTimeMs();
				long dt = now - leftClickTime;
				leftClickTime = now;

				GSTrackEntry oldClickedEntry = clickedEntry;
				clickedTrack = modelView.getTrackFromY(event.getY());
				clickedEntry = modelView.getEntryAt(event.getX(), event.getY());

				if (clickedEntry == oldClickedEntry && dt <= DOUBLE_CLICK_TIME) {
					leftClickCount++;
				} else {
					leftClickCount = 1;
				}
				
				boolean additiveSelection = Screen.hasControlDown();
				if (additiveSelection || clickedEntry == null) {
					selectingEntries = true;
					selectionStartGametick = modelView.getGametickExactFromX(event.getX());
					selectionStartY = event.getY();
					selectionEndX = event.getX();
					selectionEndY = event.getY();
					updateSelection(additiveSelection);
					event.consume();
				} else if (leftClickCount == 2) {
					GSTrack track = clickedEntry.getParent();
					if (track != null) {
						// TODO: implement as uneditable
						editTrackSequence(track);
						event.consume();
					}
				} else {
					if (!selectionModel.isSelected(clickedEntry)) {
						selectionModel.unselectAll();
						selectionModel.select(clickedEntry);
					}
					
					if (editable) {
						draggingMouseX = event.getX();
						draggingEntry = true;
					}

					event.consume();
				}
			}
		}
	}
	
	private void editTrackSequence(GSTrack track) {
		GSCapturePlaybackClientModule module = GSCapturePlaybackExtension.getInstance().getClientModule();
		module.requestSession(GSESessionType.SEQUENCE, GSESessionRequestType.REQUEST_START, track.getTrackUUID());
	}

	@Override
	public void mouseReleased(GSMouseEvent event) {
		draggingEntry = selectingEntries = false;
		
		if (!event.isConsumed() && editable) {
			long deltaMs = Util.getMeasuringTimeMs() - leftClickTime;
			if (leftClickCount == 2 && deltaMs <= ADD_DELETE_ENTRY_CLICK_TIME) {
				GSTrack track = modelView.getTrackFromY(event.getY());
				if (track != null && track == clickedTrack) {
					// Ensure that the user has not clicked on an entry
					if (clickedEntry == null) {
						addEntryAt(event.getX(), event.getY());
						leftClickCount = 0;
						event.consume();
					} else if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
						deleteEntry(clickedEntry);
						leftClickCount = 0;
						event.consume();
					}
				}
			}
		}
	}
	
	private void addEntryAt(int x, int y) {
		long offset = modelView.getGametickFromX(x);
		GSTrack track = modelView.getTrackFromY(y);
		
		if (offset >= 0L && track != null) {
			GSTrackEntry entry = track.addEntry(offset);
			
			if (entry != null) {
				selectionModel.unselectAll();
				selectionModel.select(entry);
			}
		}
	}

	@Override
	public void mouseDragged(GSMouseEvent event) {
		if (!event.isConsumed() && editable && draggingEntry) {
			long gt0 = modelView.getGametickFromX(draggingMouseX);
			long gt1 = modelView.getGametickFromX(event.getX());
			
			if (moveSelectedEntries(gt1 - gt0)) {
				// Cancel potential editing of sequences
				leftClickCount = 0;
				
				draggingMouseX = event.getX();
				event.consume();
			}
		} else if (selectingEntries) {
			selectionEndX = event.getX();
			selectionEndY = event.getY();
			updateSelection(Screen.hasControlDown());
			event.consume();
		}
	}
	
	private boolean moveSelectedEntries(long dgt) {
		if (dgt < 0L) {
			// Limit selection delta such that entries do not
			// get an offset less than zero.
			for (GSTrackEntry entry : selectionModel) {
				if (entry.getOffset() + dgt < 0)
					dgt = -entry.getOffset();
			}
		}
		
		if (dgt != 0L) {
			for (GSTrackEntry entry : selectionModel)
				entry.setOffset(entry.getOffset() + dgt);

			return true;
		}
		
		return false;
	}
	
	private void updateSelection(boolean additiveSelection) {
		if (!additiveSelection) {
			// Clear selected entries in selection model
			selectionModel.unselectAll();
		}
		
		long gt0 = modelView.getGametickFromExact(selectionStartGametick);
		long gt1 = modelView.getGametickFromX(selectionEndX);

		if (gt0 > gt1) {
			long tmp = gt0;
			gt0 = gt1;
			gt1 = tmp;
		}

		int ch = modelView.getMinimumHeight();

		if ((selectionEndY < 0 && selectionStartY < 0) ||
				(selectionEndY >= ch && selectionStartY >= ch)) {
			// We should not select anything, since selection is entirely
			// outside of the content area bounds.
			return;
		}
		
		int my0 = GSMathUtil.clamp(selectionStartY, 0, ch - 1);
		int my1 = GSMathUtil.clamp(selectionEndY, 0, ch - 1);

		GSTrack t0 = modelView.getTrackFromY(my0);
		GSTrack t1 = modelView.getTrackFromY(my1);

		if (t0 == null || t1 == null)
			return;
		
		if (modelView.isTrackAfter(t0, t1)) {
			GSTrack tmp = t0;
			t0 = t1;
			t1 = tmp;
		}
		
		// Update the selected entries
		GSTrack track = t0;
		while (track != null && !modelView.isTrackAfter(track, t1)) {
			long duration = modelView.getSequenceDuration(track.getSequence());
			
			for (GSTrackEntry entry : track.getEntries()) {
				if (entry.getOffset() <= gt1 && entry.getOffset() + duration > gt0)
					selectionModel.select(entry);
			}
			
			track = modelView.getNextTrack(track, false, false);
		}
	}
	
	@Override
	public void mouseScrolled(GSMouseEvent event) {
		if (!event.isConsumed() && Screen.hasControlDown() && isValid()) {
			double zoomSpeed = Screen.hasAltDown() ? SLOW_ZOOM_SPEED : NORMAL_ZOOM_SPEED;
			double scroll = Screen.hasShiftDown() ? event.getScrollX() : event.getScrollY();
			zoomToCenter(Math.pow(zoomSpeed, scroll), event.getX());
			event.consume();
		}
	}
	
	private void zoomToCenter(double multiplier, int x) {
		int scrollX = GSPanelUtil.getScrollX(this);
		double gt = modelView.getGametickExactFromX(x);
		modelView.multiplyZoom(multiplier);
		// Update xOffset such that the x-coordinate is
		// in the same gametick as prior to the zoom.
		int deltaX = modelView.getGametickExactX(gt) - x;
		GSPanelUtil.setScrollX(this, scrollX + deltaX);
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (!event.isConsumed() && editable) {
			boolean modifier;
			
			switch (event.getKeyCode()) {
			case GSKeyEvent.KEY_DELETE:
				deleteSelection();
				event.consume();
				break;
			case GSKeyEvent.KEY_LEFT:
				modifier = event.isModifierHeld(GSEvent.MODIFIER_CONTROL);
				if (moveSelectedEntries(-getIncrementalMoveDelta(modifier)))
					event.consume();
				break;
			case GSKeyEvent.KEY_RIGHT:
				modifier = event.isModifierHeld(GSEvent.MODIFIER_CONTROL);
				if (moveSelectedEntries(getIncrementalMoveDelta(modifier)))
					event.consume();
				break;
			}
		}
	}
	
	private long getIncrementalMoveDelta(boolean moveModifier) {
		return moveModifier ? FAST_INCREMENTAL_DELTA : SLOW_INCREMENTAL_DELTA;
	}
	
	private void deleteSelection() {
		for (GSTrackEntry entry : selectionModel)
			deleteEntry(entry);
		selectionModel.unselectAll();
	}
	
	private void deleteEntry(GSTrackEntry entry) {
		GSTrack track = entry.getParent();
		if (track != null)
			track.removeEntry(entry.getEntryUUID());
	}

	@Override
	public void focusLost(GSFocusEvent event) {
		resetMouseActions();
	}
	
	@Override
	public boolean isScrollableWidthFilled() {
		return true;
	}
	
	@Override
	public boolean isScrollableHeightFilled() {
		return true;
	}

	@Override
	public void modelViewChanged() {
		invalidate();
	}
}
