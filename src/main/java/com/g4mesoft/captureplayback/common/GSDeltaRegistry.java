package com.g4mesoft.captureplayback.common;

import java.io.IOException;
import java.util.function.Supplier;

import com.g4mesoft.registry.GSSupplierRegistry;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSDeltaRegistry<M> {

	private final GSSupplierRegistry<Integer, GSIDelta<M>> registry;
	
	GSDeltaRegistry() {
		registry = new GSSupplierRegistry<>();
	}
	
	public <T extends GSIDelta<M>> void register(int id, Class<T> deltaClazz, Supplier<T> deltaSupplier) {
		registry.register(id, deltaClazz, deltaSupplier);
	}
	
	public GSIDelta<M> read(GSDecodeBuffer buf) throws IOException {
		GSIDelta<M> delta = registry.createNewElement(buf.readInt());
		if (delta == null)
			throw new IOException("Invalid delta ID");
		delta.read(buf);
		return delta;
	}

	public void write(GSEncodeBuffer buf, GSIDelta<M> delta)  throws IOException {
		Integer identifier = registry.getIdentifier(delta);
		if (identifier == null)
			throw new IOException("Unknown delta");
		buf.writeInt(identifier);
		delta.write(buf);
	}
}
