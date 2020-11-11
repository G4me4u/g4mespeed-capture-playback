package com.g4mesoft.captureplayback.stream.frame;

import java.util.NoSuchElementException;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

public final class GSEmptySignalFrame implements GSISignalFrame {

	static final GSEmptySignalFrame INSTANCE = new GSEmptySignalFrame();
	
	private GSEmptySignalFrame() {
	}
	
	@Override
	public GSSignalEvent peek() throws NoSuchElementException {
		throw new NoSuchElementException();
	}

	@Override
	public GSSignalEvent next() throws NoSuchElementException {
		throw new NoSuchElementException();
	}

	@Override
	public int remaining() {
		return 0;
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public void mark() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void invalidateMark() {
	}
}
