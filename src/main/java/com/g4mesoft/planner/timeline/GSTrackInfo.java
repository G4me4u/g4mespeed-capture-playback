package com.g4mesoft.planner.timeline;

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
}
