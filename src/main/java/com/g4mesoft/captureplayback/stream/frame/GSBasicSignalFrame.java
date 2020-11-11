package com.g4mesoft.captureplayback.stream.frame;

import java.util.List;
import java.util.NoSuchElementException;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

public class GSBasicSignalFrame implements GSISignalFrame {

	private final List<GSSignalEvent> events;
	
	private int position;
	private int markedPosition;
	
	/**
	 * @param events - A <i>sorted</i> list of signal events that should be
	 *                 handled within the same tick.
	 */
	public GSBasicSignalFrame(List<GSSignalEvent> events) {
		if (events == null)
			throw new IllegalArgumentException("events is null!");
		
		this.events = events;
	
		position = 0;
		markedPosition = -1;
	}
	
	@Override
	public GSSignalEvent peek() throws NoSuchElementException {
		if (!hasNext())
			throw new NoSuchElementException();
		return events.get(position);
	}

	@Override
	public GSSignalEvent next() throws NoSuchElementException {
		GSSignalEvent event = peek();
		position++;
		return event;
	}
	
	@Override
	public int remaining() {
		return events.size() - position;
	}

	@Override
	public boolean hasNext() {
		return (remaining() > 0);
	}
	
	@Override
	public void mark() {
		markedPosition = position;
	}

	@Override
	public void reset() {
		if (markedPosition == -1)
			throw new IllegalStateException("No mark has been set!");
		
		position = markedPosition;
		invalidateMark();
	}
	
	@Override
	public void invalidateMark() {
		markedPosition = -1;
	}
}
