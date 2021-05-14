package com.g4mesoft.captureplayback.access;

import java.util.List;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

import net.minecraft.util.math.BlockPos;

public interface GSIServerWorldAccess {

	public void handleCaptureEvent(GSESignalEdge edge, BlockPos pos);

	public List<GSIPlaybackStream> getPlaybackStreams();
	
	public void addPlaybackStream(GSIPlaybackStream playbackStream);

	public List<GSICaptureStream> getCaptureStreams();
	
	public void addCaptureStream(GSICaptureStream captureStream);

	public boolean isPlaybackPosition(BlockPos pos);

	public boolean isCapturePosition(BlockPos pos);

	public boolean isPlaybackPowering(BlockPos pos);
	
}
