package com.g4mesoft.captureplayback.panel.sequence;

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
