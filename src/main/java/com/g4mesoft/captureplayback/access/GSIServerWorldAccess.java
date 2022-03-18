package com.g4mesoft.captureplayback.access;

import java.util.List;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface GSIServerWorldAccess {

	public void gcp_handleCaptureEvent(GSESignalEdge edge, BlockPos pos);

	public List<GSIPlaybackStream> gcp_getPlaybackStreams();
	
	public void gcp_addPlaybackStream(GSIPlaybackStream playbackStream);

	public List<GSICaptureStream> gcp_getCaptureStreams();
	
	public void gcp_addCaptureStream(GSICaptureStream captureStream);

	public boolean gcp_isPlaybackPosition(BlockPos pos);

	public boolean gcp_isCapturePosition(BlockPos pos);

	public boolean gcp_isPlaybackPowering(BlockPos pos);
	
	/* signal event context */
	
	public boolean gcp_dispatchBlockEvent(BlockPos pos, Block block, int type, int data);

	public void gcp_dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir);

	public boolean gcp_setState(BlockPos pos, BlockState state, int flags);
	
}
