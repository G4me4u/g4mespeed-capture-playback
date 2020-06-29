package com.g4mesoft.captureplayback.stream;

public interface GSIWritableStream<T> extends GSIStream {

	public void write(T value);
	
}
