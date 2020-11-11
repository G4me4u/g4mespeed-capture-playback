package com.g4mesoft.captureplayback.stream.frame;

import java.util.NoSuchElementException;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

public interface GSISignalFrame {

	public static final GSISignalFrame EMPTY = GSEmptySignalFrame.INSTANCE;
	
	public GSSignalEvent peek() throws NoSuchElementException;

	public GSSignalEvent next() throws NoSuchElementException;
	
	public int remaining();

	public boolean hasNext();
	
	public void mark();

	public void reset();
	
	public void invalidateMark();
	
}
