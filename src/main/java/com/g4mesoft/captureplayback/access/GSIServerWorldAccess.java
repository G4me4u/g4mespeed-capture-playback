package com.g4mesoft.captureplayback.access;

import java.util.Collection;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface GSIServerWorldAccess {

	public void gcp_handleCaptureEvent(GSESignalEdge edge, BlockPos pos);

	public boolean gcp_hasPlaybackStream(UUID assetUUID);
	
	public void gcp_addPlaybackStream(UUID assetUUID, GSIPlaybackStream playbackStream);

	public GSIPlaybackStream gcp_getPlaybackStream(UUID assetUUID);

	public Collection<GSIPlaybackStream> gcp_getPlaybackStreams();

	public boolean gcp_hasCaptureStream(UUID assetUUID);

	public void gcp_addCaptureStream(UUID assetUUID, GSICaptureStream captureStream);

	public GSICaptureStream gcp_getCaptureStream(UUID assetUUID);

	public Collection<GSICaptureStream> gcp_getCaptureStreams();

	public boolean gcp_isPlaybackPosition(BlockPos pos);

	public boolean gcp_isCapturePosition(BlockPos pos);

	public boolean gcp_isPlaybackPowering(BlockPos pos);
	
	/* signal event context */
	
	public boolean gcp_dispatchBlockEvent(BlockPos pos, Block block, int type, int data);

	public void gcp_dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir);

	public boolean gcp_setState(BlockPos pos, BlockState state, int flags);
	
}
