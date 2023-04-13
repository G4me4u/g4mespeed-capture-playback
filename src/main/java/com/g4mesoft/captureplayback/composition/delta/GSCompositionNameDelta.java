package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSCompositionNameDelta implements GSIDelta<GSComposition> {

	private String newName;
	private String oldName;

	public GSCompositionNameDelta() {
	}
	
	public GSCompositionNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSComposition composition) throws GSDeltaException {
		if (!Objects.equals(oldName, composition.getName()))
			throw new GSDeltaException("Composition does not have the expected name");
		composition.setName(newName);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setName(oldName, newName, composition);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setName(newName, oldName, composition);
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		newName = buf.readString();
		oldName = buf.readString();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
