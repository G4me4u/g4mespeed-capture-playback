package com.g4mesoft.captureplayback.session;

import java.util.function.Function;
import java.util.function.Supplier;

public class GSSessionFieldType<T> {

	private final String name;
	private final Function<GSSessionFieldType<T>, GSSessionField<T>> constructor;
	private final Supplier<T> defaultSupplier;
	private final boolean nullable;
	private final boolean assignOnce;
	private final GSISessionFieldCodec<T> codec;
	private final boolean cached;
	private final boolean synced;
	
	public GSSessionFieldType(String name, Function<GSSessionFieldType<T>, GSSessionField<T>> constructor, Supplier<T> defaultSupplier,
			boolean nullable, boolean assignOnce, GSISessionFieldCodec<T> codec, boolean cached, boolean synced) {
		
		if (name == null)
			throw new IllegalArgumentException("name is null");
		if (constructor == null)
			throw new IllegalArgumentException("constructor is null");
		if (codec == null)
			throw new IllegalArgumentException("codec is null");
		if (defaultSupplier == null)
			throw new IllegalArgumentException("defaultSupplier is null");
		
		this.name = name;
		this.constructor = constructor;
		this.defaultSupplier = defaultSupplier;
		this.nullable = nullable;
		this.assignOnce = assignOnce;
		this.codec = codec;
		this.cached = cached;
		this.synced = synced;
	}
	
	public String getName() {
		return name;
	}
	
	public GSSessionField<T> create() {
		GSSessionField<T> field = constructor.apply(this);
		T value = defaultSupplier.get();
		if (!nullable && value == null)
			throw new IllegalStateException("value is null");
		field.set(value);
		return field;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isAssignableOnce() {
		return assignOnce;
	}
	
	public GSISessionFieldCodec<T> getCodec() {
		return codec;
	}

	public boolean isCached() {
		return cached;
	}
	
	public boolean isSynced() {
		return synced;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		if (!GSSessionFieldType.class.equals(other.getClass()))
			return false;
		return name.equals(((GSSessionFieldType<?>)other).name);
	}
}
