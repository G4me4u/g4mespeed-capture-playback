package com.g4mesoft.captureplayback.panel.composition;

import java.util.Iterator;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.util.GSColorUtil;
import com.g4mesoft.util.GSMathUtil;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexFormats;

public class GSCompositionContentPanel extends GSPanel implements GSIMouseListener, GSIKeyListener, GSIFocusEventListener {

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
	
	private final GSComposition composition;
	private final GSCompositionModelView modelView;
	
	private final GSITrackEntrySelectionModel selectionModel;
	
	private int draggingMouseX;
	private int draggingMouseY;

	private boolean draggingComposition;
	private boolean draggingModifier;
	private boolean draggingEntry;
	
	private double selectionStartGametick;
	private int selectionStartAbsoluteY;
	private int selectionEndMouseX;
	private int selectionEndMouseY;
	private boolean selectingEntries;
	
	private GSTrackEntry clickedEntry;
	private GSTrack clickedTrack;
	private long leftClickTime;
	private int leftClickCount;
	
	public GSCompositionContentPanel(GSComposition composition, GSCompositionModelView modelView) {
		this.composition = composition;
		this.modelView = modelView;
		
		selectionModel = new GSTrackEntrySelectionModel(composition);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
		addFocusEventListener(this);
	}
	
	@Override
	protected void onShown() {
		super.onShown();

		resetMouseActions();
	}
	
	private void resetMouseActions() {
		draggingComposition = false;
		draggingModifier = false;
		draggingEntry = false;
		
		selectingEntries = false;
		
		leftClickTime = System.currentTimeMillis() - DOUBLE_CLICK_TIME;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.pushClip(0, 0, width, height);
		
		renderer.build(GSIRenderer2D.QUADS, VertexFormats.POSITION_COLOR);
		renderBackground(renderer);
		renderTimeIndicators(renderer);
		renderer.finish();
		
		for (GSTrack track : composition.getTracks())
			renderTrack(renderer, track);

		renderSelection(renderer);
		
		renderer.popClip();
	}
	
	private void renderBackground(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
	}
	
	private void renderTimeIndicators(GSIRenderer2D renderer) {
		long interval = modelView.getTimeIndicatorInterval();
		int offset = modelView.getYOffset() % (DOTTED_LINE_LENGTH + DOTTED_LINE_SPACING);
		long subInterval = interval / GSCompositionModelView.TIME_SUB_INDICATOR_COUNT;
		
		long gt = modelView.getTimeIndicatorStart();
		for (int x = 0; x < width; gt += interval) {
			x = modelView.getGametickX(gt);
			
			renderer.drawVLine(x, 0, height, TIME_INDICATOR_COLOR);
			
			if (subInterval != 0) {
				for (long j = subInterval; j < interval; j += subInterval) {
					int sx = modelView.getGametickX(gt + j);
					renderer.drawDottedVLine(sx, offset, height, DOTTED_LINE_LENGTH,
							DOTTED_LINE_SPACING, TIME_INDICATOR_COLOR);
				}
			}
		}
	}
	
	private void renderTrack(GSIRenderer2D renderer, GSTrack track) {
		for (GSTrackEntry entry : track.getEntries())
			renderEntry(renderer, entry, track.getColor());
		
		int sy = modelView.getTrackY(track) + modelView.getTrackHeight();
		renderer.fillRect(0, sy, width, modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	private void renderEntry(GSIRenderer2D renderer, GSTrackEntry entry, int color) {
		if (selectionModel.isSelected(entry))
			color = 0xFF00FF00;

		GSTrack track = entry.getParent();
		GSRectangle bounds = modelView.viewToModel(entry);
		
		if (track != null && bounds != null) {
			int titleColor = GSIRenderer.brightenColor(color);
			int titleBgColor = GSColorUtil.withAlpha(GSIRenderer.darkenColor(color), ENTRY_TITLE_BG_ALPHA);

			String title = renderer.trimString(track.getSequence().getName(), bounds.width - 4);
			int tx = bounds.x + ENTRY_TITLE_LEFT_MARGIN;
			int ty = bounds.y + (ENTRY_TITLE_HEIGHT - renderer.getTextAscent()) / 2;
			
			renderer.fillRect(bounds.x, bounds.y, bounds.width, ENTRY_TITLE_HEIGHT, titleBgColor);
			renderer.drawText(title, tx, ty, titleColor, false);

			bounds.y += ENTRY_TITLE_HEIGHT;
			bounds.height -= ENTRY_TITLE_HEIGHT;
			
			renderSequencePreview(renderer, entry, bounds, color);
		}
	}
	
	private void renderSequencePreview(GSIRenderer2D renderer, GSTrackEntry entry, GSRectangle bounds, int color) {
		int darkColor = GSIRenderer.darkenColor(color);
		int previewBgColor = GSColorUtil.withAlpha(GSIRenderer.darkenColor(darkColor), ENTRY_PREVIEW_BG_ALPHA);
		
		renderer.build(GSIRenderer2D.QUADS, VertexFormats.POSITION_COLOR);
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, previewBgColor);

		GSTrack track = entry.getParent();
		if (track != null) {
			int entryColor = GSColorUtil.withAlpha(darkColor, ENTRY_TITLE_BG_ALPHA);
			
			Iterator<GSChannel> channelItr = track.getSequence().getChannels().iterator();
			for (int yo = 1; yo < bounds.height && channelItr.hasNext(); yo += 2) {
				GSChannel channel = channelItr.next();
				
				for (GSChannelEntry channelEntry : channel.getEntries()){
					long gt0 = channelEntry.getStartTime().getGametick() + entry.getOffset();
					long gt1 = channelEntry.getEndTime().getGametick() + entry.getOffset();
	
					int x0 = modelView.getGametickX(gt0);
					int x1 = modelView.getGametickX(gt1);
					
					if (x0 == x1) {
						// Fix cases where zero ticks are not drawn
						if (x1 >= bounds.x + bounds.width) {
							x0--;
						} else {
							x1++;
						}
					}
					
					renderer.drawHLine(x0, x1, bounds.y + yo, entryColor);
				}
			}
		}
		
		renderer.finish();
	}
	
	private void renderSelection(GSIRenderer2D renderer) {
		if (selectingEntries) {
			int mx0 = modelView.getGametickExactX(selectionStartGametick);
			int my0 = selectionStartAbsoluteY + modelView.getYOffset();
			
			int sx0 = Math.min(mx0, selectionEndMouseX);
			int sy0 = Math.min(my0, selectionEndMouseY);
			int sx1 = Math.max(mx0, selectionEndMouseX);
			int sy1 = Math.max(my0, selectionEndMouseY);
			
			renderer.fillRect(sx0, sy0, sx1 - sx0, sy1 - sy0, SELECTION_BACKGROUND_COLOR);
			renderer.drawRect(sx0, sy0, sx1 - sx0, sy1 - sy0, SELECTION_BORDER_COLOR);
		}
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (!selectingEntries && !draggingEntry && !draggingComposition) {
			if ((draggingModifier && event.getButton() == GSMouseEvent.BUTTON_LEFT) ||
					event.getButton() == GSMouseEvent.BUTTON_MIDDLE) {
				
				draggingMouseX = event.getX();
				draggingMouseY = event.getY();
				draggingComposition = true;
				event.consume();
			} else if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
				long now = System.currentTimeMillis();
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
					selectionStartAbsoluteY = event.getY() - modelView.getYOffset();
					selectionEndMouseX = event.getX();
					selectionEndMouseY = event.getY();
					updateSelection(additiveSelection);
					event.consume();
				} else if (leftClickCount == 2) {
					GSTrack track = clickedEntry.getParent();
					if (track != null) {
						editTrackSequence(track);
						event.consume();
					}
				} else {
					if (!selectionModel.isSelected(clickedEntry)) {
						selectionModel.unselectAll();
						selectionModel.select(clickedEntry);
					}
					
					draggingMouseX = event.getX();
					draggingMouseY = event.getY();
					draggingEntry = true;

					event.consume();
				}
			}
		}
	}
	
	private void editTrackSequence(GSTrack track) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().requestSequenceSession(track.getTrackUUID());
	}

	@Override
	public void mouseReleased(GSMouseEvent event) {
		draggingComposition = draggingEntry = selectingEntries = false;
		
		long deltaMs = System.currentTimeMillis() - leftClickTime;
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
		if (draggingComposition) {
			int dx = event.getX() - draggingMouseX;
			int dy = event.getY() - draggingMouseY;
			draggingMouseX = event.getX();
			draggingMouseY = event.getY();
			
			modelView.setXOffset(modelView.getXOffset() + dx);
			modelView.setYOffset(modelView.getYOffset() + dy);

			event.consume();
		} else if (draggingEntry) {
			long gt0 = modelView.getGametickFromX(draggingMouseX);
			long gt1 = modelView.getGametickFromX(event.getX());
			
			if (moveSelectedEntries(gt1 - gt0)) {
				// Cancel potential editing of sequences
				leftClickCount = 0;
				
				draggingMouseX = event.getX();
				event.consume();
			}
		} else if (selectingEntries) {
			selectionEndMouseX = event.getX();
			selectionEndMouseY = event.getY();
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
		long gt1 = modelView.getGametickFromX(selectionEndMouseX);

		if (gt0 > gt1) {
			long tmp = gt0;
			gt0 = gt1;
			gt1 = tmp;
		}

		int ch = modelView.getMinimumHeight();

		int selectionEndAbsoluteY = selectionEndMouseY - modelView.getYOffset();
		if ((selectionEndAbsoluteY < 0 && selectionStartAbsoluteY < 0) ||
				(selectionEndAbsoluteY >= ch && selectionStartAbsoluteY >= ch)) {
			// We should not select anything, since selection is entirely
			// outside of the content area bounds.
			return;
		}
		
		int my0 = GSMathUtil.clamp(selectionStartAbsoluteY, 0, ch - 1);
		int my1 = GSMathUtil.clamp(selectionEndAbsoluteY, 0, ch - 1);

		GSTrack t0 = modelView.getTrackFromAbsoluteY(my0);
		GSTrack t1 = modelView.getTrackFromAbsoluteY(my1);

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
		if (Screen.hasControlDown()) {
			double zoomSpeed = Screen.hasAltDown() ? SLOW_ZOOM_SPEED : NORMAL_ZOOM_SPEED;
			double scroll = Screen.hasShiftDown() ? event.getScrollX() : event.getScrollY();
			modelView.zoomToCenter(Math.pow(zoomSpeed, scroll), event.getX(), event.getY());
			event.consume();
		}
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		boolean modifier;
		
		switch (event.getKeyCode()) {
		case GSKeyEvent.KEY_SPACE:
			draggingModifier = true;
			event.consume();
			break;
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
	public void keyReleased(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_SPACE) {
			draggingModifier = false;
			event.consume();
		}
	}
	
	@Override
	public void focusLost(GSFocusEvent event) {
		resetMouseActions();
	}
}
