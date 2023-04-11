package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSFieldSessionDelta implements GSIDelta<GSSession> {

	private GSSessionFieldPair<?> pair;

	public GSFieldSessionDelta() {
	}

	public <T> GSFieldSessionDelta(GSSessionFieldType<T> type, T value) {
		this(new GSSessionFieldPair<>(type, value));
	}

	public <T> GSFieldSessionDelta(GSSessionFieldPair<T> pair) {
		this.pair = pair;
	}
	
	@Override
	public void apply(GSSession session) throws GSDeltaException {
		if (session.getSide() == GSSessionSide.CLIENT_SIDE) {
			session.forceSet(pair);
		} else {
			session.set(pair);
		}
		
		session.cancelSync(pair.getType());
	}
	
	@Override
	public void unapply(GSSession session) throws GSDeltaException {
		throw new GSDeltaException("Unapply unsupported.");
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		pair = GSSession.readFieldPair(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSSession.writeFieldPair(buf, pair);
	}
}
