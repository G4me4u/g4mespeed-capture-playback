package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;

import com.g4mesoft.hotkey.GSKeyCode;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.minecraft.client.util.InputUtil;

public class GSHotkeyPlaylistData implements GSIPlaylistData {

	public static final GSHotkeyPlaylistData UNKNOWN_KEY = new GSHotkeyPlaylistData(GSKeyCode.UNKNOWN_KEY);
	
	private final GSKeyCode keyCode;
	
	public GSHotkeyPlaylistData(GSKeyCode keyCode) {
		if (keyCode == null)
			throw new IllegalArgumentException("keyCode is null!");
		this.keyCode = keyCode;
	}
	
	public GSKeyCode getKeyCode() {
		return keyCode;
	}
	
	@Override
	public int hashCode() {
		return keyCode.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSHotkeyPlaylistData) {
			GSHotkeyPlaylistData other = (GSHotkeyPlaylistData)obj;
			return keyCode.equals(other.keyCode);
		}
		return false;
	}

	public static GSHotkeyPlaylistData read(GSDecodeBuffer buf) throws IOException {
		int count = buf.readInt();
		InputUtil.Key[] keys = new InputUtil.Key[count];
		for (int i = 0; i < count; i++) {
			String keyId = buf.readString();
			keys[i] = InputUtil.fromTranslationKey(keyId);
		}
		return new GSHotkeyPlaylistData(GSKeyCode.fromKeys(keys));
	}

	public static void write(GSEncodeBuffer buf, GSHotkeyPlaylistData data) throws IOException {
		GSKeyCode keyCode = data.keyCode;
		buf.writeInt(keyCode.getKeyCount());
		for (int i = 0; i < keyCode.getKeyCount(); i++)
			buf.writeString(keyCode.get(i).getTranslationKey());
	}
}
