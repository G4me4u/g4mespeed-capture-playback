package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public final class GSChannelInfo {

	private static final int MAX_POSITION_COUNT = 0xFFFF;
	
	private final String name;
	private final int color;
	private final Set<BlockPos> positions;

	public GSChannelInfo(String name, int color, BlockPos position) {
		this(name, color, Collections.singleton(position), true);
	}

	public GSChannelInfo(String name, int color, Set<BlockPos> positions) {
		this(name, color, positions, false);
	}
	
	public GSChannelInfo(String name, int color, Set<BlockPos> positions, boolean internal) {
		if (positions.isEmpty() || positions.size() > MAX_POSITION_COUNT)
			throw new IllegalArgumentException("Insufficient or too many positions");
		
		this.name = name;
		this.color = color | 0xFF000000;
		if (internal) {
			this.positions = positions;
		} else {
			this.positions = new LinkedHashSet<>();
			for (BlockPos position : positions)
				this.positions.add(position.toImmutable());
		}
	}

	public String getName() {
		return name;
	}

	public int getColor() {
		return color;
	}

	public Set<BlockPos> getPositions() {
		return Collections.unmodifiableSet(positions);
	}

	public GSChannelInfo withName(String name) {
		return new GSChannelInfo(name, color, positions, true);
	}


	public GSChannelInfo withColor(int color) {
		return new GSChannelInfo(name, color, positions, true);
	}

	public GSChannelInfo withPositions(Set<BlockPos> positions) {
		return new GSChannelInfo(name, color, positions, false);
	}

	public GSChannelInfo addPosition(BlockPos position) {
		Set<BlockPos> positions = new LinkedHashSet<>(this.positions);
		positions.add(position.toImmutable());
		return new GSChannelInfo(name, color, positions, true);
	}
	
	public boolean equals(GSChannelInfo other) {
		if (other == null)
			return false;
		if (!Objects.equals(name, other.name))
			return false;
		if (color != other.color)
			return false;
		if (!positions.equals(other.positions))
			return false;

		return true;
	}
	
	public boolean equals(Object other) {
		if (other instanceof GSChannelInfo)
			return equals((GSChannelInfo)other);
		return false;
	}
	
	public static GSChannelInfo read(PacketByteBuf buf) throws IOException {
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		int color = buf.readUnsignedMedium();

		int positionCount = buf.readUnsignedShort();
		if (positionCount == 0 || positionCount > MAX_POSITION_COUNT)
			throw new IOException("Insufficient or too many positions");
		Set<BlockPos> positions = new LinkedHashSet<>(positionCount);
		while (positionCount-- != 0)
			positions.add(buf.readBlockPos());
		
		return new GSChannelInfo(name, color, positions, true);
	}

	public static void write(PacketByteBuf buf, GSChannelInfo info) throws IOException {
		buf.writeString(info.name);
		buf.writeMedium(info.color);
		buf.writeShort(info.positions.size());
		for (BlockPos position : info.positions)
			buf.writeBlockPos(position);
	}
}
