package com.g4mesoft.captureplayback.playlist.delta;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.playlist.GSETriggerType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistDataRegistry;
import com.g4mesoft.captureplayback.playlist.GSTrigger;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTriggerDelta implements GSIDelta<GSPlaylist> {

	protected GSETriggerType newType;
	protected GSIPlaylistData newData;
	protected GSETriggerType oldType;
	protected GSIPlaylistData oldData;

	public GSTriggerDelta() {
	}

	public GSTriggerDelta(GSTrigger trigger, GSETriggerType oldType, GSIPlaylistData oldData) {
		this(trigger.getType(), trigger.getData(), oldType, oldData);
	}
	
	public GSTriggerDelta(GSETriggerType newType, GSIPlaylistData newData, GSETriggerType oldType, GSIPlaylistData oldData) {
		this.newType = newType;
		this.newData = newData;
		this.oldType = oldType;
		this.oldData = oldData;
	}
	
	private void checkTrigger(GSTrigger trigger, GSETriggerType type, GSIPlaylistData data) throws GSDeltaException {
		if (trigger.getType() != type)
			throw new GSDeltaException("Trigger does not have the expected type: " + type);
		if (!trigger.getData().equals(data))
			throw new GSDeltaException("Trigger does not have the expected data");
	}
	
	@Override
	public void apply(GSPlaylist model) throws GSDeltaException {
		GSTrigger trigger = model.getTrigger();
		checkTrigger(trigger, oldType, oldData);
		try {
			trigger.set(newType, newData);
		} catch (Throwable t) {
			// In case data does not match type.
			throw new GSDeltaException(t);
		}
	}

	@Override
	public void unapply(GSPlaylist model) throws GSDeltaException {
		GSTrigger trigger = model.getTrigger();
		checkTrigger(model.getTrigger(), newType, newData);
		try {
			trigger.set(oldType, oldData);
		} catch (Throwable t) {
			throw new GSDeltaException(t);
		}
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		newType = GSETriggerType.fromIndex(buf.readUnsignedByte());
		if (newType == null)
			throw new IOException("Unknown trigger type");
		newData = GSPlaylistDataRegistry.readData(buf);
		oldType = GSETriggerType.fromIndex(buf.readUnsignedByte());
		if (oldType == null)
			throw new IOException("Unknown trigger type");
		oldData = GSPlaylistDataRegistry.readData(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUnsignedByte((short)newType.getIndex());
		GSPlaylistDataRegistry.writeData(buf, newData);
		buf.writeUnsignedByte((short)oldType.getIndex());
		GSPlaylistDataRegistry.writeData(buf, oldData);
	}
}
