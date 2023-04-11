package com.g4mesoft.captureplayback.common;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public interface GSIDelta<M> {

	public void apply(M model) throws GSDeltaException;
	
	public void unapply(M model) throws GSDeltaException;
	
	public void read(GSDecodeBuffer buf) throws IOException;

	public void write(GSEncodeBuffer buf) throws IOException;
	
}
