package com.g4mesoft.captureplayback.sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

import net.minecraft.util.math.BlockPos;

class GSSequenceCaptureStream implements GSICaptureStream {
	
	private final GSBlockRegion blockRegion;
	private final Map<BlockPos, List<GSChannelCapture>> posToCaptures;
	
	private boolean closed;
	private long captureTime;
	
	public GSSequenceCaptureStream(GSSequence sequence) {
		this.blockRegion = sequence.getBlockRegion();
		posToCaptures = new HashMap<>();
		
		for (GSChannel channel : sequence.getChannels()) {
			// TODO: fix issues when capturing a single channel in multiple positions
			for (BlockPos position : channel.getInfo().getPositions()) {
				List<GSChannelCapture> captures = posToCaptures.get(position);
				if (captures == null) {
					// Assume 2 or less channels in the same position
					captures = new ArrayList<>(2);
					posToCaptures.put(position, captures);
				}
				
				captures.add(new GSChannelCapture(channel));
			}
		}
		
		closed = posToCaptures.isEmpty();
		captureTime = 0L;
	}

	@Override
	public void write(GSISignalFrame frame) {
		if (!isClosed()) {
			boolean triggered = false;
			
			while (frame.hasNext()) {
				GSSignalEvent event = frame.next();

				List<GSChannelCapture> captures = posToCaptures.get(event.getPos());
				if (captures != null) {
					triggered = true;
					
					for (GSChannelCapture capture : captures)
						capture.onEvent(captureTime, event);
				}
			}
			
			// Start capture when we receive the first event.
			if (captureTime != 0L || triggered)
				captureTime++;
		}
	}
	
	@Override
	public GSBlockRegion getBlockRegion() {
		return blockRegion;
	}

	@Override
	public void close() {
		if (!isClosed()) {
			for (List<GSChannelCapture> captures : posToCaptures.values())
				captures.forEach(GSChannelCapture::onClose);
		}
		
		closed = true;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
	
	private static class GSChannelCapture {

		private final GSChannel channel;

		private GSSignalTime risingTime;
		private boolean risingShadow;
		
		public GSChannelCapture(GSChannel channel) {
			this.channel = channel;
			
			risingTime = null;
		}
		
		private void onEvent(long captureTime, GSSignalEvent event) {
			GSSignalTime time = new GSSignalTime(captureTime, event.getMicrotick());
			
			if (event.getEdge() == GSESignalEdge.RISING_EDGE) {
				if (risingTime != null && !risingShadow) {
					// In case we already have a rising edge
					addHalfEntry(risingTime, GSEChannelEntryType.EVENT_START);
				}
				
				risingTime = time;
				risingShadow = event.isShadow();
			} else { // event.getEdge() == GSESignalEdge.FALLING_EDGE
				if (risingTime == null) {
					// In case the signal did not have a rising edge
					addHalfEntry(time, GSEChannelEntryType.EVENT_END);
				} else {
					// Ensure that we actually have an event
					if (!risingShadow || !event.isShadow()) {
						GSChannelEntry entry = channel.tryAddEntry(risingTime, time);
						// Handle shadow events
						if (entry != null && (risingShadow || event.isShadow())) {
							entry.setType(risingShadow ? GSEChannelEntryType.EVENT_END :
							                             GSEChannelEntryType.EVENT_START);
						}
					}
	
					// Invalidate the current rising time
					risingTime = null;
					risingShadow = false;
				}
			}
		}

		private void addHalfEntry(GSSignalTime time, GSEChannelEntryType type) {
			GSChannelEntry entry = channel.tryAddEntry(time, time);
			if (entry != null)
				entry.setType(type);
		}
		
		private void onClose() {
			if (risingTime != null && !risingShadow) {
				addHalfEntry(risingTime, GSEChannelEntryType.EVENT_START);
				risingTime = null;
				risingShadow = false;
			}
		}
	}
}
