package com.g4mesoft.captureplayback.stream;

public interface GSIStream {

	public void addCloseListener(GSIStreamCloseListener listener);

	public void removeCloseListener(GSIStreamCloseListener listener);
	
	public GSBlockRegion getBlockRegion();

	public void close();
	
	public boolean isClosed();

}
