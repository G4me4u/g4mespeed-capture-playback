package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.panel.GSEContentOpacity;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceUndoRedoHistory;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSSession {

	private static final GSISessionFieldCodec<Float>                     FLOAT_CODEC               = new GSFloatSessionFieldCodec();
	private static final GSISessionFieldCodec<GSEContentOpacity>         OPACITY_CODEC             = new GSOpacitySessionFieldCodec();
	private static final GSISessionFieldCodec<GSComposition>             COMPOSITION_CODEC         = new GSBasicSessionFieldCodec<>(GSComposition::read, GSComposition::write);
	private static final GSISessionFieldCodec<GSSequence>                SEQUENCE_CODEC            = new GSBasicSessionFieldCodec<>(GSSequence::read, GSSequence::write);
	private static final GSISessionFieldCodec<UUID>                      UUID_CODEC                = new GSBasicSessionFieldCodec<>(PacketByteBuf::readUuid, PacketByteBuf::writeUuid);
	private static final GSISessionFieldCodec<GSSequenceUndoRedoHistory> S_UNDO_REDO_HISTORY_CODEC = new GSBasicSessionFieldCodec<>(GSSequenceUndoRedoHistory::read, GSSequenceUndoRedoHistory::write);
	
	public static final GSSessionFieldType<Float>                     X_OFFSET;
	public static final GSSessionFieldType<Float>                     Y_OFFSET;
	public static final GSSessionFieldType<GSEContentOpacity>         OPACITY;
	
	public static final GSSessionFieldType<GSComposition>             C_COMPOSITION;
	
	public static final GSSessionFieldType<GSSequence>                S_SEQUENCE;
	public static final GSSessionFieldType<UUID>                      S_SELECTED_CHANNEL;
	public static final GSSessionFieldType<GSSequenceUndoRedoHistory> S_UNDO_REDO_HISTORY;

	private static final Map<String, GSSessionFieldType<?>> nameToType;
	private static final Map<GSESessionType, Set<GSSessionFieldType<?>>> sessionFieldTypes;
	
	static {
		nameToType = new HashMap<>();
		sessionFieldTypes = new HashMap<>();
		
		GSSessionFieldTypeBuilder<?> builder = new GSSessionFieldTypeBuilder<>(nameToType, sessionFieldTypes);
		
		X_OFFSET = builder.<Float>as().name("xOffset").def(0.0f).codec(FLOAT_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
		Y_OFFSET = builder.<Float>as().name("yOffset").def(0.0f).codec(FLOAT_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
		OPACITY  = builder.<GSEContentOpacity>as().name("opacity").def(GSEContentOpacity.FULLY_OPAQUE).codec(OPACITY_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
	
		C_COMPOSITION = builder.<GSComposition>as().name("composition").constr(GSCompositionSessionField::new).nullable().codec(COMPOSITION_CODEC).noCache().noSync().session(GSESessionType.COMPOSITION).build();

		S_SEQUENCE = builder.<GSSequence>as().name("sequence").constr(GSSequenceSessionField::new).nullable().codec(SEQUENCE_CODEC).noCache().noSync().session(GSESessionType.SEQUENCE).build();
		S_SELECTED_CHANNEL = builder.<UUID>as().name("selectedChannel").nullable().codec(UUID_CODEC).session(GSESessionType.SEQUENCE).build();
		S_UNDO_REDO_HISTORY = builder.<GSSequenceUndoRedoHistory>as().name("s_urHistory").constr(GSSequenceUndoRedoHistorySessionField::new).def(GSSequenceUndoRedoHistory::new).codec(S_UNDO_REDO_HISTORY_CODEC).session(GSESessionType.SEQUENCE).build();
	}
	
	private final GSESessionType type;
	
	private final GSSessionFields fields;
	private List<GSISessionListener> listeners;
	
	private Set<GSSessionFieldType<?>> fieldsToSync;
	
	public GSSession(GSESessionType type) {
		if (type == null)
			throw new IllegalArgumentException("type is null");
		
		this.type = type;
		
		fields = new GSSessionFields(this);
		listeners = null;
		
		fieldsToSync = new HashSet<>();
		
		addFields();
	}
	
	private void addFields() {
		Set<GSSessionFieldType<?>> types = sessionFieldTypes.get(type);

		if (types != null) {
			for (GSSessionFieldType<?> type : types)
				fields.add(type);
		}
	}
	
	public GSESessionType getType() {
		return type;
	}
	
	public <T> boolean contains(GSSessionFieldType<T> type) {
		return fields.contains(type);
	}
	
	private <T> boolean set(GSSessionFieldPair<T> pair) {
		return set(pair.getType(), pair.getValue());
	}
	
	public <T> boolean set(GSSessionFieldType<T> type, T value) {
		if (fields.set(type, value)) {
			onFieldChanged(type);
			return true;
		}
		
		return false;
	}

	private void setAll(GSSessionFields fields) {
		for (GSSessionFieldPair<?> pair : fields)
			set(pair);
	}
	
	public <T> T get(GSSessionFieldType<T> type) {
		return fields.get(type);
	}

	/* Visible for sub-classes of GSISessionDelta */
	GSSessionField<?> getField(GSSessionFieldType<?> type) {
		return fields.getField(type);
	}

	private void onFieldChanged(GSSessionFieldType<?> type) {
		if (type.isSynced())
			requestSync(type);
		dispatchFieldChanged(type);
	}
	
	public void requestSync(GSSessionFieldType<?> type) {
		fieldsToSync.add(type);
	}
	
	public void sync() {
		if (!fieldsToSync.isEmpty()) {
			GSISessionDelta[] deltas = new GSISessionDelta[fieldsToSync.size()];
			int i = 0;
			for (GSSessionFieldType<?> type : fieldsToSync) {
				GSSessionFieldPair<?> pair = new GSSessionFieldPair<>(type, get(type));
				deltas[i++] = new GSFieldSessionDelta(pair);
			}
			fieldsToSync.clear();
			
			dispatchSessionDeltas(deltas);
		}
	}
	
	public void addListener(GSISessionListener listener) {
		if (listeners == null)
			listeners = new ArrayList<>(2);
		listeners.add(listener);
	}
	
	public void removeListener(GSISessionListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}
	
	private void dispatchFieldChanged(GSSessionFieldType<?> type) {
		if (listeners != null) {
			for (GSISessionListener listener : listeners)
				listener.onFieldChanged(this, type);
		}
	}

	/* Visible for sub-classes of GSSessionField */
	void dispatchSessionDelta(GSISessionDelta delta) {
		dispatchSessionDeltas(new GSISessionDelta[] { delta });
	}
	
	private void dispatchSessionDeltas(GSISessionDelta[] deltas) {
		if (listeners != null) {
			for (GSISessionListener listener : listeners)
				listener.onSessionDeltas(this, deltas);
		}
	}
	
	public static GSSessionFieldType<?> readFieldType(PacketByteBuf buf) throws IOException {
		GSSessionFieldType<?> type = nameToType.get(buf.readString(GSBufferUtil.MAX_STRING_LENGTH));
		if (type == null)
			throw new IOException("Unknown type");
		return type;
	}

	public static void writeFieldType(PacketByteBuf buf, GSSessionFieldType<?> type) throws IOException {
		buf.writeString(type.getName());
	}

	public static <T> T readField(PacketByteBuf buf, GSSessionFieldType<T> type) throws IOException {
		return type.getCodec().decode(buf);
	}
	
	public static <T> void writeField(PacketByteBuf buf, GSSessionFieldType<T> type, T value) throws IOException {
		type.getCodec().encode(buf, value);
	}

	/** Expands to readFieldType(...) and readField(...) */
	public static <T> GSSessionFieldPair<T> readFieldPair(PacketByteBuf buf) throws IOException {
		GSSessionFieldType<?> type = readFieldType(buf);
		return new GSSessionFieldPair<>(type, readField(buf, type));
	}
	
	/** Expands to for writeFieldType(...) and writeField(...) */
	public static <T> void writeFieldPair(PacketByteBuf buf, GSSessionFieldPair<T> pair) throws IOException {
		writeFieldType(buf, pair.getType());
		writeField(buf, pair.getType(), pair.getValue());
	}
	
	public static GSSession read(PacketByteBuf buf) throws IOException {
		GSESessionType sessionType = GSESessionType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown session type");

		GSSessionFields fields = GSSessionFields.read(buf);
		
		GSSession session = new GSSession(sessionType);
		session.setAll(fields);
		return session;
	}

	public static void writeCache(PacketByteBuf buf, GSSession session) throws IOException {
		write(buf, session, true);
	}

	public static void writePacket(PacketByteBuf buf, GSSession session) throws IOException {
		write(buf, session, false);
	}

	private static void write(PacketByteBuf buf, GSSession session, boolean cache) throws IOException {
		buf.writeInt(session.getType().getIndex());
		
		if (cache) {
			GSSessionFields.write(buf, session.fields, GSSessionFieldType::isCached);
		} else {
			GSSessionFields.write(buf, session.fields);
		}
	}
}
