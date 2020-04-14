package com.g4mesoft.captureplayback.gui;

import java.util.Locale;
import java.util.function.Predicate;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSPanel;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.BlockPos;

public class GSPositionGUI extends GSPanel {

	private static final int LABEL_COLOR = 0xFFFFFFFF;
	
	private static final String X_LABEL = "X:";
	private static final String Y_LABEL = "Y:";
	private static final String Z_LABEL = "Z:";
	
	private static final int FIELD_MARGIN = 5;
	
	private final BlockPos.Mutable pos;

	private TextFieldWidget xField;
	private TextFieldWidget yField;
	private TextFieldWidget zField;
	
	public GSPositionGUI() {
		pos = new BlockPos.Mutable();
		
		xField = yField = zField = null;
	}
	
	@Override
	public void tick() {
		super.init();

		tickField(xField);
		tickField(yField);
		tickField(zField);
	}
	
	private void tickField(TextFieldWidget textField) {
		if (textField != null)
			textField.tick();
	}
	
	@Override
	public void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);
		
		renderFieldLabel(mouseX, mouseY, partialTicks, X_LABEL, xField);
		renderFieldLabel(mouseX, mouseY, partialTicks, Y_LABEL, yField);
		renderFieldLabel(mouseX, mouseY, partialTicks, Z_LABEL, zField);
	}
	
	private void renderFieldLabel(int mouseX, int mouseY, float partialTicks, String label, TextFieldWidget textField) {
		if (textField != null) {
			int labelX = textField.x - FIELD_MARGIN - font.getStringWidth(label);
			int labelY = (height - font.fontHeight) / 2;
			drawString(font, label, labelX, labelY, LABEL_COLOR);
		}
	}
	
	@Override
	public void init() {
		super.init();

		int xLabelWidth = font.getStringWidth(X_LABEL);
		int yLabelWidth = font.getStringWidth(Y_LABEL);
		int zLabelWidth = font.getStringWidth(Z_LABEL);

		int labelWidths = xLabelWidth + yLabelWidth + zLabelWidth;
		int fieldWidth = (width - labelWidths) / 3 - FIELD_MARGIN;
		
		int x = xLabelWidth + FIELD_MARGIN;
		xField = new TextFieldWidget(font, x, 0, fieldWidth, height, "");
		x += 2 * FIELD_MARGIN + yLabelWidth + fieldWidth;
		yField = new TextFieldWidget(font, x, 0, fieldWidth, height, "");
		x += 2 * FIELD_MARGIN + zLabelWidth + fieldWidth;
		zField = new TextFieldWidget(font, x, 0, fieldWidth, height, "");
		
		xField.setText(formatCoordinate(pos.getX()));
		yField.setText(formatCoordinate(pos.getY()));
		zField.setText(formatCoordinate(pos.getZ()));
		
		Predicate<String> predicate = new CoordinateFieldPredicate();
		xField.setTextPredicate(predicate);
		yField.setTextPredicate(predicate);
		zField.setTextPredicate(predicate);
		
		addWidget(xField);
		addWidget(yField);
		addWidget(zField);
	}
	
	@Override
	@GSCoreOverride
	public void setFocused(Element element) {
		super.setFocused(element);
		
		if (xField != element)
			xField.method_1876(false);
		if (yField != element)
			yField.method_1876(false);
		if (zField != element)
			zField.method_1876(false);
	}
	
	private String formatCoordinate(int c) {
		return String.format(Locale.ENGLISH, "%d", c);
	}
	
	private static class CoordinateFieldPredicate implements Predicate<String> {

		@Override
		public boolean test(String text) {
			for (int i = 0; i < text.length(); i++) {
				if (!Character.isDigit(text.charAt(i)))
					return false;
			}
			
			try {
				Integer.parseInt(text);
			} catch (NumberFormatException e) {
				return false;
			}
			
			return true;
		}
	}
}
