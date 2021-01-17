package com.g4mesoft.captureplayback.stream;

import net.minecraft.util.math.BlockPos;

public final class GSBlockRegion {

	private final int x0;
	private final int y0;
	private final int z0;

	private final int x1;
	private final int y1;
	private final int z1;
	
	public GSBlockRegion(BlockPos p0, BlockPos p1) {
		this(p0.getX(), p0.getY(), p0.getZ(), p1.getX(), p1.getY(), p1.getZ());
	}
	
	public GSBlockRegion(int x0, int y0, int z0, int x1, int y1, int z1) {
		this.x0 = Math.min(x0, x1);
		this.y0 = Math.min(y0, y1);
		this.z0 = Math.min(z0, z1);

		this.x1 = Math.max(x0, x1);
		this.y1 = Math.max(y0, y1);
		this.z1 = Math.max(z0, z1);
	}
	
	public boolean collides(GSBlockRegion other) {
		return (other.x1 >= x0 && other.x0 <= x1 && 
				other.y1 >= y0 && other.y0 <= y1 && 
				other.z1 >= z0 && other.z0 <= z1);
	}

	public boolean contains(BlockPos pos) {
		return contains(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public boolean contains(int x, int y, int z) {
		return (x >= x0 && x <= x1 && 
		        y >= y0 && y <= y1 && 
		        z >= z0 && z <= z1);
	}
	
	public GSBlockRegion merge(GSBlockRegion other) {
		int x0 = Math.min(this.x0, other.x0);
		int y0 = Math.min(this.y0, other.y0);
		int z0 = Math.min(this.z0, other.z0);
		
		int x1 = Math.max(this.x1, other.x1);
		int y1 = Math.max(this.y1, other.y1);
		int z1 = Math.max(this.z1, other.z1);
	
		return new GSBlockRegion(x0, y0, z0, x1, y1, z1);
	}
	
	public GSBlockRegion expand(int x, int y, int z) {
		int x0 = Math.min(this.x0, x);
		int y0 = Math.min(this.y0, y);
		int z0 = Math.min(this.z0, z);
		
		int x1 = Math.max(this.x1, x);
		int y1 = Math.max(this.y1, y);
		int z1 = Math.max(this.z1, z);
	
		return new GSBlockRegion(x0, y0, z0, x1, y1, z1);
	}
	
	public int getX0() {
		return x0;
	}
	
	public int getY0() {
		return y0;
	}
	
	public int getZ0() {
		return z0;
	}
	
	public int getX1() {
		return x1;
	}
	
	public int getY1() {
		return y1;
	}
	
	public int getZ1() {
		return z1;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		hash = hash * 31 + x0;
		hash = hash * 31 + y0;
		hash = hash * 31 + z0;

		hash = hash * 31 + x1;
		hash = hash * 31 + y1;
		hash = hash * 31 + z1;
		
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSBlockRegion))
			return false;
		
		GSBlockRegion region = (GSBlockRegion)other;
		if (region.x0 != x0 || region.y0 != y0 || region.z0 != z0)
			return false;
		if (region.x1 != x1 || region.y1 != y1 || region.z1 != z1)
			return false;
		
		return true;
	}
}
