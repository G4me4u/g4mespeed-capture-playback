package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public final class GSChannelInfo {

	private final String name;
	private final BlockPos pos;
	private final int color;
	
	public GSChannelInfo(String name, BlockPos pos, int color) {
		this.name = name;
		this.pos = pos.toImmutable();
		this.color = color | 0xFF000000;
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

	public GSChannelInfo withName(String name) {
		return new GSChannelInfo(name, pos, color);
	}

	public GSChannelInfo withPos(BlockPos pos) {
		return new GSChannelInfo(name, pos, color);
	}

	public GSChannelInfo withColor(int color) {
		return new GSChannelInfo(name, pos, color);
	}
	
	public boolean equals(GSChannelInfo other) {
		if (other == null)
			return false;
		if (color != other.color)
			return false;
		if (!pos.equals(other.pos))
			return false;
		
		return Objects.equals(name, other.name);
	}
	
	public boolean equals(Object other) {
		if (other instanceof GSChannelInfo)
			return equals((GSChannelInfo)other);
		return false;
	}
	
	public static GSChannelInfo read(PacketByteBuf buf) throws IOException {
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		BlockPos pos = buf.readBlockPos();
		// Skip reserved byte
		buf.readByte();
		int color = buf.readUnsignedMedium();
		
		return new GSChannelInfo(name, pos, color);
	}

	public static void write(PacketByteBuf buf, GSChannelInfo info) throws IOException {
		buf.writeString(info.name);
		buf.writeBlockPos(info.pos);
		buf.writeByte(0x00);
		buf.writeMedium(info.color);
	}
}
