package com.g4mesoft.captureplayback.session;

final class GSSessionFieldPair<T> {

	private final GSSessionFieldType<T> type;
	private final T value;

	@SuppressWarnings("unchecked")
	GSSessionFieldPair(GSSessionFieldType<?> type, Object value) {
		this.type = (GSSessionFieldType<T>)type;
		this.value = (T)value;
	}
	
	public GSSessionFieldType<T> getType() {
		return type;
	}
	
	public T getValue() {
		return value;
	}
}
