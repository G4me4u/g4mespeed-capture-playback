package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;
import com.google.common.base.Predicates;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/* Visible for GSSession */
class GSSessionFields implements Iterable<GSSessionFieldPair<?>> {

	private GSSession session;
	
	private final Map<GSSessionFieldType<?>, GSSessionField<?>> fields;

	private GSSessionFields() {
		this(null);
	}
	
	public GSSessionFields(GSSession session) {
		this.session = session;
		
		fields = new HashMap<>();
	}
	
	public <T> boolean contains(GSSessionFieldType<T> field) {
		return fields.containsKey(field);
	}

	/* Visible for GSSession */
	<T> boolean add(GSSessionFieldType<T> type) {
		GSSessionField<T> field = getField(type);
		
		if (field == null) {
			field = type.create();
			fields.put(type, field);
			field.onAdded(session);
			return true;
		}
		
		return false;
	}

	/* Visible for GSSession */
	<T> void forceSet(GSSessionFieldPair<T> pair) {
		GSSessionField<T> field = getField(pair.getType());
		if (field != null)
			field.set(pair.getValue());
	}
	
	public <T> boolean set(GSSessionFieldType<T> type, T value) {
		if (!type.isNullable() && value == null)
			throw new IllegalArgumentException("value is null");
		
		GSSessionField<T> field = getField(type);
		if (field != null && !Objects.equals(field.get(), value)) {
			if (type.isAssignableOnce() && field.get() != null)
				throw new IllegalStateException("value already assigned");
			
			field.set(value);
			return true;
		}
		
		return false;
	}
	
	public <T> T get(GSSessionFieldType<T> type) {
		GSSessionField<T> field = getField(type);
		return (field == null) ? null : field.get();
	}

	/* Visible for GSSession */
	<T> GSSessionField<T> getField(GSSessionFieldType<T> type) {
		@SuppressWarnings("unchecked")
		GSSessionField<T> field = (GSSessionField<T>)fields.get(type);
		return field;
	}
	
	public int getSize() {
		return fields.size();
	}
	
	@Override
	public Iterator<GSSessionFieldPair<?>> iterator() {
		return new GSSessionFieldIterator();
	}

	public static GSSessionFields read(GSDecodeBuffer buf) throws IOException {
		GSSessionFields fields = new GSSessionFields();
		
		int fieldCount = buf.readInt();
		while (fieldCount-- != 0) {
			int sizeInBytes = buf.readInt();
			int location = buf.getLocation();
			
			GSSessionFieldType<?> type;
			try {
				type = GSSession.readFieldType(buf);
			} catch (IOException e) {
				// Skip the remaining field bytes
				buf.skipBytes(sizeInBytes - (buf.getLocation() - location));
				continue;
			}
			
			GSSessionFieldPair<?> pair = new GSSessionFieldPair<>(type, GSSession.readField(buf, type));
			if (fields.contains(pair.getType()))
				throw new IOException("Duplicate field type");
			
			fields.add(pair.getType());
			fields.forceSet(pair);
		}
		
		return fields;
	}

	public static void write(GSEncodeBuffer buf, GSSessionFields fields) throws IOException {
		write(buf, fields, Predicates.alwaysTrue());
	}
	
	public static void write(GSEncodeBuffer buf, GSSessionFields fields, Predicate<GSSessionFieldType<?>> fieldFilter) throws IOException {
		ByteBuf fieldBuffer = Unpooled.buffer();

		List<GSSessionFieldPair<?>> pairsToWrite = new ArrayList<>(fields.getSize());
		for (GSSessionFieldPair<?> pair : fields) {
			if (fieldFilter.test(pair.getType()))
				pairsToWrite.add(pair);
		}
		
		buf.writeInt(pairsToWrite.size());
		for (GSSessionFieldPair<?> pair : pairsToWrite) {
			GSSession.writeFieldPair(GSEncodeBuffer.wrap(fieldBuffer), pair);
			buf.writeInt(fieldBuffer.readableBytes());
			buf.writeBytes(fieldBuffer);
		}
		
		fieldBuffer.release();
	}

	public class GSSessionFieldIterator implements Iterator<GSSessionFieldPair<?>> {
		
		private Iterator<Map.Entry<GSSessionFieldType<?>, GSSessionField<?>>> fieldItr;
		
		public GSSessionFieldIterator() {
			fieldItr = fields.entrySet().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return fieldItr.hasNext();
		}

		public GSSessionFieldPair<?> next() {
			if (!fieldItr.hasNext())
				throw new NoSuchElementException();

			Map.Entry<GSSessionFieldType<?>, GSSessionField<?>> entry = fieldItr.next();
			return new GSSessionFieldPair<>(entry.getKey(), entry.getValue().get());
		}
		
		@Override
	    public void remove() {
			fieldItr.remove();
		}
	}
}
