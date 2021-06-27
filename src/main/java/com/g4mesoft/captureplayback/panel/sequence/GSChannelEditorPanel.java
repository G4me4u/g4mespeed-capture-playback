package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.panel.GSLocation;
import com.g4mesoft.panel.field.GSColorPickerField;

public class GSChannelEditorPanel extends GSEditorPanel {

	private final GSChannel channel;
	private GSChannelInfo info;
	
	private final GSColorPickerField colorPickerField;
	
	public GSChannelEditorPanel(GSChannel channel) {
		this.channel = channel;
		this.info = channel.getInfo();
	
		colorPickerField = new GSColorPickerField(info.getColor());
		colorPickerField.addActionListener(() -> {
			info = info.withColor(colorPickerField.getColor());
		});
		
		getContentPanel().add(colorPickerField);
	}
	
	@Override
	protected void layout() {
		super.layout();
		
		colorPickerField.setBounds(new GSLocation(10, 10), colorPickerField.getPreferredSize());
	}

	@Override
	protected void apply() {
		channel.setInfo(info);
	}
}
