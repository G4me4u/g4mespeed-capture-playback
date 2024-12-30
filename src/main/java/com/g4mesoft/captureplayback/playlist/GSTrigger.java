package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrigger extends GSAbstractPlaylistEntry<GSETriggerType> {

	public GSTrigger(GSTrigger other) {
		this(other.getType(), other.getData());
	}

	public GSTrigger(GSETriggerType type, GSIPlaylistData data) {
		super(type, data);
	}

	@Override
	protected void dispatchDataChanged(GSETriggerType oldType, GSIPlaylistData oldData) {
		GSPlaylist parent = getParent();
		if (parent != null) {
			for (GSIPlaylistListener listener : parent.getListeners())
				listener.triggerChanged(oldType, oldData);
		}
	}
	
	public static GSTrigger read(GSDecodeBuffer buf) throws IOException {
		GSETriggerType type = GSETriggerType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Unknown trigger type");
		GSIPlaylistData data = GSPlaylistDataRegistry.readData(buf);
		return new GSTrigger(type, data);
	}

	public static void write(GSEncodeBuffer buf, GSTrigger trigger) throws IOException {
		buf.writeInt(trigger.getType().getIndex());
		GSPlaylistDataRegistry.writeData(buf, trigger.getData());
	}
}
