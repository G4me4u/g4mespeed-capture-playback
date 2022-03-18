package com.g4mesoft.captureplayback.stream.handler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface GSISignalEventContext {

	/* Propagates a change event to surrounding blocks. */
	public static final int PROPAGATE_CHANGE      = 0b00000001; //  1;
	/* Notifies listeners and clients who need to react when the block changes */
	public static final int NOTIFY_LISTENERS      = 0b00000010; //  2;
	/* Used in conjunction with NOTIFY_LISTENERS to suppress the render pass on clients. */
	public static final int NO_REDRAW             = 0b00000100; //  4;
	/* Forces a synchronous redraw on clients. */
	public static final int REDRAW_ON_MAIN_THREAD = 0b00001000; //  8;
	/* Bypass virtual blockstate changes and forces the passed state to be stored as-is. */
	public static final int FORCE_STATE           = 0b00010000; // 16;
	/* Prevents the previous block (container) from dropping items when destroyed. */
	public static final int SKIP_DROPS            = 0b00100000; // 32;
	/* Signals that this is a mechanical update, usually caused by pistons moving blocks. */
	public static final int MECHANICAL_UPDATE     = 0b01000000; // 64;
	
	public boolean dispatchBlockEvent(BlockPos pos, Block block, int type, int data);

	public void dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir);

	public boolean setState(BlockPos pos, BlockState state, int flags);
	
}
