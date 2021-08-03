package com.g4mesoft.captureplayback.session;

public class GSSessionField<T> {

	protected GSSessionFieldType<T> type;
	protected T value;
	
	public GSSessionField(GSSessionFieldType<T> type) {
		this.type = type;
	}
	
	public void onAdded(GSSession session) {
	}

	public T get() {
		return value;
	}
	
	public void set(T value) {
		this.value = value;
	}
}
