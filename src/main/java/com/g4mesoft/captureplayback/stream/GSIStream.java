package com.g4mesoft.captureplayback.stream;

public interface GSIStream {

	public GSBlockRegion getBlockRegion();

	public void close();
	
	public boolean isClosed();

}
