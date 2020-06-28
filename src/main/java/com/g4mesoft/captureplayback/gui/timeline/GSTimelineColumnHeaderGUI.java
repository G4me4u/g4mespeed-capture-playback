package com.g4mesoft.captureplayback.gui.timeline;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.gui.GSPanel;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;

public class GSTimelineColumnHeaderGUI extends GSPanel {

	public static final int COLUMN_HEADER_COLOR = 0x60000000;
	public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;
	public static final int DARK_HEADER_TEXT_COLOR = 0xFFB2B2B2;
	
	public static final int COLUMN_COLOR = 0x60202020;
	public static final int DARK_COLUMN_COLOR = 0x60000000;
	
	public static final int COLUMN_LINE_COLOR = 0x30B2B2B2;
	public static final int MT_COLUMN_LINE_COLOR = 0x30FEFEFE;
	
	public static final int DOTTED_LINE_LENGTH = 4;
	public static final int DOTTED_LINE_SPACING = 3;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSTimelineModelView modelView;
	
	public GSTimelineColumnHeaderGUI(GSTimeline timeline, GSExpandedColumnModel expandedColumnModel, GSTimelineModelView modelView) {
		this.expandedColumnModel = expandedColumnModel;
		this.modelView = modelView;
	}
	
	@Override
	public void renderTranslated(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(matrixStack, mouseX, mouseY, partialTicks);
	
		fill(matrixStack, 0, 0, width, height, COLUMN_HEADER_COLOR);
		
		renderColumnHeaders(matrixStack, mouseX, mouseY);

		fill(matrixStack, 0, height - 1, width, height, GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
	}
	
	private void renderColumnHeaders(MatrixStack matrixStack, int mouseX, int mouseY) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		((GSIBufferBuilderAccess)buffer).pushClip(0.0f, 0.0f, width, height);
		
		int columnStart = Math.max(0, modelView.getColumnIndexFromView(0));
		int columnEnd = modelView.getColumnIndexFromView(width - 1);

		int x0 = modelView.getColumnX(columnStart);
		for (int columnIndex = columnStart; columnIndex <= columnEnd; columnIndex++) {
			int x1 = x0 + modelView.getColumnWidth(columnIndex);
			renderColumnHeader(matrixStack, mouseX, mouseY, columnIndex, x0, x1);
			x0 = x1;
		}

		((GSIBufferBuilderAccess)buffer).popClip();
	}
	
	private void renderColumnHeader(MatrixStack matrixStack, int mouseX, int mouseY, int columnIndex, int x0, int x1) {
		boolean expanded = expandedColumnModel.isColumnExpanded(columnIndex);
		
		int y;
		int color = HEADER_TEXT_COLOR;
		if (expanded) {
			y = (height / 2 - textRenderer.fontHeight) / 2;
		} else {
			if (expandedColumnModel.hasExpandedColumn())
				color = DARK_HEADER_TEXT_COLOR;
			y = (height - textRenderer.fontHeight) / 2;
		}

		fill(matrixStack, x0, 0, x1, height, getColumnColor(columnIndex));
		
		String title = getColumnTitle(columnIndex);
		drawCenteredString(matrixStack, textRenderer, title, (x0 + x1) / 2, y, color);
		
		if (mouseX >= x0 && mouseX < x1) {
			fill(matrixStack, x0, 0, x0 + 1, height, COLUMN_LINE_COLOR);
			fill(matrixStack, x1 - 1, 0, x1, height, COLUMN_LINE_COLOR);
		}
		
		if (expanded)
			renderExpandedColumnHeader(matrixStack, mouseX, mouseY, columnIndex);
	}
	
	private void renderExpandedColumnHeader(MatrixStack matrixStack, int mouseX, int mouseY, int expandedColumnIndex) {
		int duration = modelView.getColumnDuration(expandedColumnIndex);
		int y = height * 3 / 4 - textRenderer.fontHeight / 2;
		for (int mt = 0; mt < duration; mt++) {
			int x = modelView.getMicrotickColumnX(expandedColumnIndex, mt);
			int w = modelView.getMicrotickColumnWidth(expandedColumnIndex, mt);

			String title = getMicrotickHeaderTitle(mt);
			drawCenteredString(matrixStack, textRenderer, title, x + w / 2, y, HEADER_TEXT_COLOR);
		
			if (mt != 0) {
				int ly = height / 2 + GSTimelineColumnHeaderGUI.DOTTED_LINE_SPACING / 2;
				drawVerticalDottedLine(matrixStack, x, ly, height, DOTTED_LINE_LENGTH, DOTTED_LINE_SPACING, MT_COLUMN_LINE_COLOR);
			}
		}
		
		int x0 = modelView.getColumnX(expandedColumnIndex);
		int x1 = x0 + modelView.getColumnWidth(expandedColumnIndex);
		int ys = height / 2;
		fill(matrixStack, x0, ys - 1, x1, ys, MT_COLUMN_LINE_COLOR);
	}
	
	private int getColumnColor(int columnIndex) {
		return (columnIndex & 0x1) != 0 ? DARK_COLUMN_COLOR : COLUMN_COLOR;
	}
	
	private String getColumnTitle(int columnIndex) {
		return String.format(Locale.ENGLISH, "%dgt", modelView.getColumnGametick(columnIndex));
	}

	private String getMicrotickHeaderTitle(int mt) {
		return String.format(Locale.ENGLISH, "%d", mt);
	}
	
	@Override
	public boolean onMouseClickedGS(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			int hoveredColumn = modelView.getColumnIndexFromView((int)mouseX);
			if (hoveredColumn != -1) {
				if (!Screen.hasShiftDown()) {
					expandedColumnModel.toggleExpandedColumn(hoveredColumn);
				} else {
					expandedColumnModel.includeExpandedColumn(hoveredColumn);
				}
				
				return true;
			}
		}
		
		return super.onMouseClickedGS(mouseX, mouseY, button);
	}
}
