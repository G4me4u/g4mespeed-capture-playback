package com.g4mesoft.planner.timeline;

import java.util.Objects;

import net.minecraft.util.math.BlockPos;

public class GSTrackInfo {

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
}
