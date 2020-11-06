package com.g4mesoft.captureplayback.gui.timeline;

public class GSMultiCellInfo {

	private final int columnIndex;
	private final int count;
	
	GSMultiCellInfo(int columnIndex, int count) {
		this.columnIndex = columnIndex;
		this.count = count;
	}
	
	public int getColumnIndex() {
		return columnIndex;
	}

	public int getCount() {
		return count;
	}
}
