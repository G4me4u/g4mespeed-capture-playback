package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public final class GSTrackInfo {

	private final String name;
	private final BlockPos pos;
	private final int color;
	
	public GSTrackInfo(String name, BlockPos pos, int color) {
		this.name = name;
		this.pos = pos.toImmutable();
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public BlockPos getPos() {
		return pos;
	}
	
	public int getColor() {
		return color;
	}

	public GSTrackInfo withName(String name) {
		return new GSTrackInfo(name, pos, color);
	}

	public GSTrackInfo withPos(BlockPos pos) {
		return new GSTrackInfo(name, pos, color);
	}

	public GSTrackInfo withColor(int color) {
		return new GSTrackInfo(name, pos, color);
	}
	
	public boolean equals(GSTrackInfo other) {
		if (other == null)
			return false;
		if (color != other.color)
			return false;
		if (!pos.equals(other.pos))
			return false;
		
		return Objects.equals(name, other.name);
	}
	
	public boolean equals(Object other) {
		if (other instanceof GSTrackInfo)
			return equals((GSTrackInfo)other);
		return false;
	}
	
	public static GSTrackInfo read(PacketByteBuf buf) throws IOException {
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		BlockPos pos = buf.readBlockPos();
		int color = buf.readInt();
		
		return new GSTrackInfo(name, pos, color);
	}

	public static void write(PacketByteBuf buf, GSTrackInfo info) throws IOException {
		buf.writeString(info.name);
		buf.writeBlockPos(info.pos);
		buf.writeInt(info.color);
	}
}
