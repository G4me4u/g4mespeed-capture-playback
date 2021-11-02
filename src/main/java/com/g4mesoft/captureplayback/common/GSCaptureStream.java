package com.g4mesoft.captureplayback.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

import net.minecraft.util.math.BlockPos;

public abstract class GSCaptureStream implements GSICaptureStream {

	private final GSBlockRegion blockRegion;
	private final Map<BlockPos, List<GSChannelCapture>> posToCaptures;
	
	private boolean closed;
	private long captureTime;
	private long latestEventTime;
	
	public GSCaptureStream(GSBlockRegion blockRegion) {
		this.blockRegion = blockRegion;
		posToCaptures = new HashMap<>();
		
		closed = false;
		captureTime = latestEventTime = 0L;
	}
	
	protected void addChannelCapture(GSChannel channel, long offset) {
		for (BlockPos position : channel.getInfo().getPositions()) {
			List<GSChannelCapture> captures = posToCaptures.get(position);
			if (captures == null) {
				// Assume 2 or less channels in the same position
				captures = new ArrayList<>(2);
				posToCaptures.put(position, captures);
			}
			
			captures.add(new GSChannelCapture(channel, offset));
		}
	}
	
	@Override
	public void write(GSISignalFrame frame) {
		if (!isClosed()) {
			boolean triggered = false;
			
			if (frame.hasNext()) {
				latestEventTime = captureTime;
				
				do {
					GSSignalEvent event = frame.next();
	
					List<GSChannelCapture> captures = posToCaptures.get(event.getPos());
					if (captures != null) {
						triggered = true;
						
						for (GSChannelCapture capture : captures)
							capture.onEvent(captureTime, event);
					}
				} while (frame.hasNext());
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
			for (List<GSChannelCapture> captures : posToCaptures.values()) {
				for (GSChannelCapture capture : captures)
					capture.onClose(latestEventTime);
			}
		}
		
		closed = true;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
	
	private static class GSChannelCapture {

		private final GSChannel channel;
		private final long offset;

		private GSSignalTime risingTime;
		private boolean risingShadow;
		
		public GSChannelCapture(GSChannel channel, long offset) {
			this.channel = channel;
			this.offset = offset;
			
			risingTime = null;
		}
		
		private void onEvent(long captureTime, GSSignalEvent event) {
			captureTime -= offset;

			if (captureTime < 0L) {
				// We have still not gotten to the correct capture time.
				return;
			}
			
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
			addHalfEntry(time, time, type);
		}

		private void addHalfEntry(GSSignalTime startTime, GSSignalTime endTime, GSEChannelEntryType type) {
			GSChannelEntry entry = channel.tryAddEntry(startTime, endTime);
			if (entry != null)
				entry.setType(type);
		}
		
		private void onClose(long latestTime) {
			if (risingTime != null && !risingShadow) {
				GSSignalTime endTime;
				if (risingTime.getGametick() == latestTime) {
					endTime = risingTime;
				} else {
					endTime = new GSSignalTime(latestTime, 0);
				}
				addHalfEntry(risingTime, endTime, GSEChannelEntryType.EVENT_START);
				risingTime = null;
				risingShadow = false;
			}
		}
	}
}
