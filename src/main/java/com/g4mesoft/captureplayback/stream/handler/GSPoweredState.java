package com.g4mesoft.captureplayback.stream.handler;

public class GSPoweredState {

	private int count;
	
	/* Used internally for GSServerWorldMixin */
	public GSPoweredState(int initialCount) {
		count = initialCount;
	}
	
	public void increment() {
		count++;
	}

	public void decrement() {
		count--;
	}
	
	public boolean isPowered() {
		return (count > 0);
	}
}
