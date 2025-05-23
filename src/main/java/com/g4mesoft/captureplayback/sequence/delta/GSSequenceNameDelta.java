package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSSequenceNameDelta implements GSIDelta<GSSequence> {

	private String newName;
	private String oldName;

	public GSSequenceNameDelta() {
	}
	
	public GSSequenceNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSSequence sequence) throws GSDeltaException {
		if (!Objects.equals(oldName, sequence.getName()))
			throw new GSDeltaException("Sequence does not have the expected name");
		sequence.setName(newName);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		setName(oldName, newName, sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		setName(newName, oldName, sequence);
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
