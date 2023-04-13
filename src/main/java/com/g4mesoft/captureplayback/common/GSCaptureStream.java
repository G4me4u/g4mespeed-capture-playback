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

public abstract class GSCaptureStream extends GSAbstractStream implements GSICaptureStream {

	private final GSBlockRegion blockRegion;
	private final Map<BlockPos, List<GSChannelCapture>> posToCaptures;
	
	private long captureTime;
	private long latestEventTime;
	
	private boolean closed;
	
	public GSCaptureStream(GSBlockRegion blockRegion) {
		this.blockRegion = blockRegion;
		posToCaptures = new HashMap<>();
		
		captureTime = latestEventTime = 0L;
		
		closed = false;
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
	public final void close() {
		if (!closed) {
			closed = true;
			for (List<GSChannelCapture> captures : posToCaptures.values()) {
				for (GSChannelCapture capture : captures)
					capture.onClose(latestEventTime);
			}
			dispatchCloseEvent();
		}
	}
	
	@Override
	public final boolean isClosed() {
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
					addEntry(risingTime, risingTime, GSEChannelEntryType.EVENT_START);
				}
				
				risingTime = time;
				risingShadow = event.isShadow();
			} else { // event.getEdge() == GSESignalEdge.FALLING_EDGE
				if (risingTime == null) {
					// In case the signal did not have a rising edge
					addEntry(time, time, GSEChannelEntryType.EVENT_END);
				} else {
					// Ensure that we actually have an event
					if (!risingShadow || !event.isShadow()) {
						// Handle shadow events
						GSEChannelEntryType type;
						if (risingShadow || event.isShadow()) {
							type = risingShadow ? GSEChannelEntryType.EVENT_END :
							                      GSEChannelEntryType.EVENT_START;
						} else {
							type = GSEChannelEntryType.EVENT_BOTH;
						}
						
						addEntry(risingTime, time, type);
					}
	
					// Invalidate the current rising time
					risingTime = null;
					risingShadow = false;
				}
			}
		}

		private void addEntry(GSSignalTime startTime, GSSignalTime endTime, GSEChannelEntryType type) {
			GSChannelEntry entry = channel.tryAddEntry(startTime, endTime);
			if (entry != null && type != GSEChannelEntryType.EVENT_BOTH)
				entry.setType(type);
		}
		
		private void onClose(long latestTime) {
			latestTime -= offset;

			if (latestTime < 0L)
				return;
			
			if (risingTime != null && !risingShadow) {
				GSSignalTime endTime;
				if (risingTime.getGametick() == latestTime) {
					endTime = risingTime;
				} else {
					endTime = new GSSignalTime(latestTime, 0);
				}
				
				addEntry(risingTime, endTime, GSEChannelEntryType.EVENT_START);
				
				risingTime = null;
				risingShadow = false;
			}
		}
	}
}
