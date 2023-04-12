package com.g4mesoft.captureplayback.stream;

import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

public class GSDelayedPlaybackStream implements GSIPlaybackStream {

	private final GSIPlaybackStream stream;
	private int delay;
	
	public GSDelayedPlaybackStream(GSIPlaybackStream stream, int delay) {
		if (stream == null)
			throw new IllegalArgumentException("stream is null");
		this.stream = stream;
		this.delay = delay;
	}

	@Override
	public GSISignalFrame read() {
		if (delay > 0) {
			delay--;
			return GSISignalFrame.EMPTY;
		}
		return stream.read();
	}

	@Override
	public void addCloseListener(GSIStreamCloseListener listener) {
		stream.addCloseListener(listener);
	}

	@Override
	public void removeCloseListener(GSIStreamCloseListener listener) {
		stream.removeCloseListener(listener);
	}

	@Override
	public GSBlockRegion getBlockRegion() {
		return stream.getBlockRegion();
	}

	@Override
	public void close() {
		delay = 0;
		stream.close();
	}

	@Override
	public boolean isClosed() {
		return stream.isClosed();
	}

	@Override
	public boolean isForceClosed() {
		return stream.isForceClosed();
	}
}
