package com.g4mesoft.captureplayback.gui.timeline;

import java.util.ArrayList;
import java.util.List;

public class GSExpandedColumnModel {

	private final List<GSIExpandedColumnModelListener> listeners;

	private int minColumnIndex;
	private int maxColumnIndex;
	
	public GSExpandedColumnModel() {
		listeners = new ArrayList<GSIExpandedColumnModelListener>();

		clearExpandedColumns();
	}
	
	public void addModelListener(GSIExpandedColumnModelListener listener) {
		listeners.add(listener);
	}

	public void removeModelListener(GSIExpandedColumnModelListener listener) {
		listeners.remove(listener);
	}
	
	public void clearExpandedColumns() {
		setExpandedColumn(-1);
	}
	
	public void setExpandedColumn(int columnIndex) {
		setExpandedColumnRange(columnIndex, columnIndex);
	}

	public void setExpandedColumnRange(int minColumnIndex, int maxColumnIndex) {
		if (maxColumnIndex < minColumnIndex)
			maxColumnIndex = minColumnIndex = -1;
			
		if (minColumnIndex != this.minColumnIndex || maxColumnIndex != this.maxColumnIndex) {
			this.minColumnIndex = minColumnIndex;
			this.maxColumnIndex = maxColumnIndex;

			dispatchExpandedColumnChangedEvent();
		}
	}
	
	public void toggleExpandedColumn(int columnIndex) {
		if (hasExpandedColumn()) {
			if (columnIndex == minColumnIndex) {
				setExpandedColumnRange(minColumnIndex + 1, maxColumnIndex);
			} else if (columnIndex == maxColumnIndex) {
				setExpandedColumnRange(minColumnIndex, maxColumnIndex - 1);
			} else if (isColumnExpanded(columnIndex)) {
				clearExpandedColumns();
			} else {
				setExpandedColumn(columnIndex);
			}
		} else {
			setExpandedColumn(columnIndex);
		}
	}

	public void includeExpandedColumn(int columnIndex) {
		if (hasExpandedColumn()) {
			if (columnIndex < minColumnIndex) {
				setExpandedColumnRange(columnIndex, maxColumnIndex);
			} else if (columnIndex > maxColumnIndex) {
				setExpandedColumnRange(minColumnIndex, columnIndex);
			}
		} else {
			setExpandedColumn(columnIndex);
		}
	}
	
	private void dispatchExpandedColumnChangedEvent() {
		for (GSIExpandedColumnModelListener listener : listeners)
			listener.onExpandedColumnChanged(minColumnIndex, maxColumnIndex);
	}

	public boolean isColumnExpanded(int columnIndex) {
		if (!hasExpandedColumn())
			return false;
		
		// Check if column index is between range
		if (columnIndex < minColumnIndex)
			return false;
		if (columnIndex > maxColumnIndex)
			return false;
	
		return true;
	}

	public boolean hasExpandedColumn() {
		return (minColumnIndex != -1);
	}

	public boolean isSingleExpandedColumn() {
		return (hasExpandedColumn() && minColumnIndex == maxColumnIndex);
	}

	public boolean isMultiExpandedColumn() {
		return (hasExpandedColumn() && minColumnIndex != maxColumnIndex);
	}
	
	public int getMinColumnIndex() {
		return minColumnIndex;
	}

	public int getMaxColumnIndex() {
		return maxColumnIndex;
	}
}
