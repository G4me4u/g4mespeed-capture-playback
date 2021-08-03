package com.g4mesoft.captureplayback.session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public final class GSSessionFieldTypeBuilder<T> {

	private static final Supplier<Object> NULL_SUPPLIER = Suppliers.ofInstance(null);
	
	private final Map<String, GSSessionFieldType<?>> nameToType;
	private final Map<GSESessionType, Set<GSSessionFieldType<?>>> sessionFieldTypes;
	
	private String name;
	private Function<GSSessionFieldType<T>, GSSessionField<T>> constructor;
	private Supplier<T> defaultSupplier;
	private boolean nullable;
	private GSISessionFieldCodec<T> codec;
	private boolean cached;
	private boolean synced;
	private final Set<GSESessionType> sessionTypes;
	
	private boolean hasName;
	private boolean hasDefaultSupplier;
	private boolean hasCodec;
	
	public GSSessionFieldTypeBuilder(Map<String, GSSessionFieldType<?>> nameToType,
			Map<GSESessionType, Set<GSSessionFieldType<?>>> sessionFieldTypes) {
		
		this.nameToType = nameToType;
		this.sessionFieldTypes = sessionFieldTypes;
		
		sessionTypes = new HashSet<>();
		
		reset();
	}
	
	private void reset() {
		name = null;
		constructor = GSSessionField::new;
		@SuppressWarnings("unchecked")
		Supplier<T> nullSupplier = (Supplier<T>)NULL_SUPPLIER;
		defaultSupplier = nullSupplier;
		nullable = false;
		codec = null;
		cached = true;
		synced = true;
		sessionTypes.clear();
		
		hasName = false;
		hasDefaultSupplier = false;
		hasCodec = false;
	}
	
	public GSSessionFieldTypeBuilder<T> name(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");

		this.name = name;
		hasName = true;
		return this;
	}

	public GSSessionFieldTypeBuilder<T> constr(Function<GSSessionFieldType<T>, GSSessionField<T>> constructor) {
		if (constructor == null)
			throw new IllegalArgumentException("constructor is null");

		this.constructor = constructor;
		return this;
	}

	public GSSessionFieldTypeBuilder<T> def(T defaultValue) {
		this.defaultSupplier = Suppliers.ofInstance(defaultValue);
		hasDefaultSupplier = true;
		return this;
	}

	public GSSessionFieldTypeBuilder<T> def(Supplier<T> defaultSupplier) {
		if (defaultSupplier == null)
			throw new IllegalArgumentException("defaultSupplier is null");
		
		this.defaultSupplier = defaultSupplier;
		hasDefaultSupplier = true;
		return this;
	}
	
	public GSSessionFieldTypeBuilder<T> nullable() {
		nullable = true;
		return this;
	}

	public GSSessionFieldTypeBuilder<T> codec(GSISessionFieldCodec<T> codec) {
		this.codec = codec;
		hasCodec = true;
		return this;
	}
	
	public GSSessionFieldTypeBuilder<T> noCache() {
		cached = false;
		return this;
	}

	public GSSessionFieldTypeBuilder<T> noSync() {
		synced = false;
		return this;
	}
	
	public GSSessionFieldTypeBuilder<T> session(GSESessionType sessionType) {
		sessionTypes.add(sessionType);
		return this;
	}
	
	public GSSessionFieldType<T> build() {
		if (!hasName)
			throw new IllegalStateException("must specify a name");
		if (!nullable && !hasDefaultSupplier)
			throw new IllegalStateException("must specify a default supplier");
		if (!hasCodec)
			throw new IllegalStateException("must specify a codec");
		if (sessionTypes.isEmpty())
			throw new IllegalStateException("must specify a session type");
		
		GSSessionFieldType<T> type = new GSSessionFieldType<>(name, constructor, defaultSupplier, nullable, codec, cached, synced);
		if (nameToType.put(name, type) != null)
			throw new IllegalStateException("Duplicate type with name: " + name);
		
		for (GSESessionType sessionType : sessionTypes) {
			Set<GSSessionFieldType<?>> types = sessionFieldTypes.get(sessionType);
			if (types == null) {
				types = new HashSet<>();
				sessionFieldTypes.put(sessionType, types);
			}
			types.add(type);
		}
		
		reset();
		
		return type;
	}
	
	public <V> GSSessionFieldTypeBuilder<V> as() {
		@SuppressWarnings("unchecked")
		GSSessionFieldTypeBuilder<V> builder = (GSSessionFieldTypeBuilder<V>)this;
		return builder;
	}
}
