package com.g4mesoft.captureplayback.panel.sequence;

import java.util.Set;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.field.GSColorPickerField;
import com.g4mesoft.ui.panel.field.GSTextField;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.panel.table.GSEHeaderResizePolicy;
import com.g4mesoft.ui.panel.table.GSITableModel;
import com.g4mesoft.ui.panel.table.GSTablePanel;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class GSChannelEditorPanel extends GSEditorPanel {

	private static final int FIELD_MARGIN = 2;
	
	private static final Text TITLE_TEXT     = GSTextUtil.translatable("panel.edit.channel.title");
	private static final Text NAME_TEXT      = GSTextUtil.translatable("panel.edit.channel.name");
	private static final Text COLOR_TEXT     = GSTextUtil.translatable("panel.edit.channel.color");
	private static final Text POSITIONS_TEXT = GSTextUtil.translatable("panel.edit.channel.positions");
	
	private final GSChannel channel;
	private GSChannelInfo info;
	
	private final GSTextField nameField;
	private final GSColorPickerField colorPickerField;

	public GSChannelEditorPanel(GSChannel channel) {
		this.channel = channel;
		this.info = channel.getInfo();
	
		nameField = new GSTextField(info.getName());
		colorPickerField = new GSColorPickerField(info.getColor());
		
		setTitle(TITLE_TEXT);
		
		initLayout(getContentPanel());
	}

	private void initLayout(GSPanel contentPanel) {
		contentPanel.setLayoutManager(new GSGridLayoutManager());

		int gridY = 0;
		GSTextLabel nameLabel = new GSTextLabel(NAME_TEXT);
		nameLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(nameLabel);
		nameField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		contentPanel.add(nameField);
		GSTextLabel colorLabel = new GSTextLabel(COLOR_TEXT);
		colorLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.TOP_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(colorLabel);
		colorPickerField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.TOP_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		contentPanel.add(colorPickerField);
		GSTextLabel positionsLabel = new GSTextLabel(POSITIONS_TEXT);
		positionsLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.TOP_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		contentPanel.add(positionsLabel);
		// Prepare Table of positions
		Set<BlockPos> positions = info.getPositions();
		GSTablePanel positionsTable = new GSTablePanel(1, positions.size());
		positionsTable.setColumnHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_ALL);
		positionsTable.setRowHeaderResizePolicy(GSEHeaderResizePolicy.RESIZE_OFF);
		// Build table model
		GSITableModel positionsModel = positionsTable.getModel();
		int positionIndex = 0;
		for (BlockPos pos : positions) {
			String label = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
			positionsModel.setCellValue(0, positionIndex++, label);
		}
		// Add table to scroll panel
		GSScrollPanel positionScrollPanel = new GSScrollPanel(positionsTable);
		positionScrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH)
			.set(GSGridLayoutManager.TOP_MARGIN, FIELD_MARGIN);
		contentPanel.add(positionScrollPanel);
	}
	
	@Override
	protected void apply() {
		// Query field values
		info = info.withName(nameField.getText());
		info = info.withColor(colorPickerField.getColor());
		
		channel.setInfo(info);
	}
}
