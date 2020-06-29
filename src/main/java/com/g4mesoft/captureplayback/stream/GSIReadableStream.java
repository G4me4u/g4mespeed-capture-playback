package com.g4mesoft.captureplayback.stream;

public interface GSIReadableStream<T> extends GSIStream {

	public T read();
	
}
