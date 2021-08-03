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

import com.google.common.base.Predicates;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

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

	public <T> boolean add(GSSessionFieldType<T> type) {
		@SuppressWarnings("unchecked")
		GSSessionField<T> field = (GSSessionField<T>)fields.get(type);
		
		if (field == null) {
			field = type.create();
			fields.put(type, field);
			field.onAdded(session);
			return true;
		}
		
		return false;
	}

	private <T> boolean set(GSSessionFieldPair<T> pair) {
		return set(pair.getType(), pair.getValue());
	}
	
	public <T> boolean set(GSSessionFieldType<T> type, T value) {
		@SuppressWarnings("unchecked")
		GSSessionField<T> field = (GSSessionField<T>)fields.get(type);
		
		if (field != null && !Objects.equals(field.get(), value)) {
			field.set(value);
			return true;
		}
		
		return false;
	}
	
	public <T> T get(GSSessionFieldType<T> type) {
		GSSessionField<T> field = getField(type);
		return (field == null) ? null : field.get();
	}

	public <T> GSSessionField<T> getField(GSSessionFieldType<T> type) {
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

	public static GSSessionFields read(PacketByteBuf buf) throws IOException {
		GSSessionFields fields = new GSSessionFields();
		
		int fieldCount = buf.readInt();
		while (fieldCount-- != 0) {
			int sizeInBytes = buf.readInt();
			int location = buf.readerIndex();
			
			GSSessionFieldType<?> type;
			try {
				type = GSSession.readFieldType(buf);
			} catch (IOException e) {
				// Skip the remaining field bytes
				buf.skipBytes(sizeInBytes - (buf.readerIndex() - location));
				continue;
			}
			
			GSSessionFieldPair<?> pair = new GSSessionFieldPair<>(type, GSSession.readField(buf, type));
			if (fields.contains(pair.getType()))
				throw new IOException("Duplicate field type");
			
			fields.add(pair.getType());
			fields.set(pair);
		}
		
		return fields;
	}

	public static void write(PacketByteBuf buf, GSSessionFields fields) throws IOException {
		write(buf, fields, Predicates.alwaysTrue());
	}
	
	public static void write(PacketByteBuf buf, GSSessionFields fields, Predicate<GSSessionFieldType<?>> fieldFilter) throws IOException {
		PacketByteBuf fieldBuffer = new PacketByteBuf(Unpooled.buffer());

		List<GSSessionFieldPair<?>> pairsToWrite = new ArrayList<>(fields.getSize());
		for (GSSessionFieldPair<?> pair : fields) {
			if (fieldFilter.test(pair.getType()))
				pairsToWrite.add(pair);
		}
		
		buf.writeInt(pairsToWrite.size());
		for (GSSessionFieldPair<?> pair : pairsToWrite) {
			GSSession.writeFieldPair(fieldBuffer, pair);
			buf.writeInt(fieldBuffer.writerIndex() - fieldBuffer.readerIndex());
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
