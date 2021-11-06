package com.g4mesoft.captureplayback.stream.frame;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

public class GSMergedSignalFrame implements GSISignalFrame {

	private final LinkedList<GSISignalFrame> frames;
	
	public GSMergedSignalFrame() {
		frames = new LinkedList<>();
	}
	
	public void merge(GSISignalFrame frame) {
		if (frame != EMPTY)
			frames.addLast(frame);
	}
	
	@Override
	public GSSignalEvent peek() throws NoSuchElementException {
		return getNextFrame().peek();
	}

	@Override
	public GSSignalEvent next() throws NoSuchElementException {
		return getNextFrame().next();
	}
	
	private GSISignalFrame getNextFrame() throws NoSuchElementException {
		GSISignalFrame nextFrame = null;
		GSSignalEvent nextEvent = null;
		
		Iterator<GSISignalFrame> itr = frames.iterator();
		while (itr.hasNext()) {
			GSISignalFrame frame = itr.next();
			
			if (frame.hasNext()) {
				GSSignalEvent event = frame.peek();
				
				// Replace the next frame by the smallest next event. Hence, if
				// the current next event is greater we should replace it.
				if (nextEvent == null || nextEvent.compareTo(event) > 0) {
					nextFrame = frame;
					nextEvent = event;
				}
			}
		}

		if (nextFrame == null)
			throw new NoSuchElementException();
		
		return nextFrame;
	}

	@Override
	public int remaining() {
		// The remaining amount of events might have changed since
		// the last call. We have to calculate it each time.
		int remaining = 0;
		for (GSISignalFrame frame : frames)
			remaining += frame.remaining();
		return remaining;
	}

	@Override
	public boolean hasNext() {
		return (remaining() > 0);
	}

	@Override
	public void mark() {
		for (GSISignalFrame frame : frames)
			frame.mark();
	}

	@Override
	public void reset() {
		for (GSISignalFrame frame : frames)
			frame.reset();
	}

	@Override
	public void invalidateMark() {
		for (GSISignalFrame frame : frames)
			frame.invalidateMark();
	}
}
