package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.panel.GSEContentOpacity;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSSession {

	private static final GSISessionFieldCodec<Integer>           INTEGER_CODEC           = new GSIntegerSessionFieldCodec();
	private static final GSISessionFieldCodec<Float>             FLOAT_CODEC             = new GSFloatSessionFieldCodec();
	private static final GSISessionFieldCodec<Double>            DOUBLE_CODEC            = new GSDoubleSessionFieldCodec();
	private static final GSISessionFieldCodec<GSEContentOpacity> OPACITY_CODEC           = new GSOpacitySessionFieldCodec();
	private static final GSISessionFieldCodec<GSComposition>     COMPOSITION_CODEC       = new GSBasicSessionFieldCodec<>(GSComposition::read, GSComposition::write);
	private static final GSISessionFieldCodec<GSSequence>        SEQUENCE_CODEC          = new GSBasicSessionFieldCodec<>(GSSequence::read, GSSequence::write);
	private static final GSISessionFieldCodec<UUID>              UUID_CODEC              = new GSBasicSessionFieldCodec<>(PacketByteBuf::readUuid, PacketByteBuf::writeUuid);
	private static final GSISessionFieldCodec<GSUndoRedoHistory> UNDO_REDO_HISTORY_CODEC = new GSBasicSessionFieldCodec<>(GSUndoRedoHistory::read, GSUndoRedoHistory::write);
	
	public static final GSSessionFieldType<Float>             X_OFFSET;
	public static final GSSessionFieldType<Float>             Y_OFFSET;
	public static final GSSessionFieldType<GSEContentOpacity> OPACITY;

	public static final GSSessionFieldType<GSUndoRedoHistory> UNDO_REDO_HISTORY;

	public static final GSSessionFieldType<GSComposition>     COMPOSITION;
	public static final GSSessionFieldType<Double>            GAMETICK_WIDTH;
	
	public static final GSSessionFieldType<GSSequence>        SEQUENCE;
	public static final GSSessionFieldType<UUID>              SELECTED_CHANNEL;
	public static final GSSessionFieldType<Integer>           MIN_EXPANDED_COLUMN;
	public static final GSSessionFieldType<Integer>           MAX_EXPANDED_COLUMN;

	private static final Map<String, GSSessionFieldType<?>> nameToType;
	private static final Map<GSESessionType, Set<GSSessionFieldType<?>>> sessionFieldTypes;
	
	static {
		nameToType = new HashMap<>();
		sessionFieldTypes = new EnumMap<>(GSESessionType.class);
		
		GSSessionFieldTypeBuilder<?> builder = new GSSessionFieldTypeBuilder<>(nameToType, sessionFieldTypes);
		
		X_OFFSET = builder.<Float>cast().name("xOffset").def(0.0f).codec(FLOAT_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
		Y_OFFSET = builder.<Float>cast().name("yOffset").def(0.0f).codec(FLOAT_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
		OPACITY  = builder.<GSEContentOpacity>cast().name("opacity").def(GSEContentOpacity.FULLY_OPAQUE).codec(OPACITY_CODEC).session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();

		UNDO_REDO_HISTORY = builder.<GSUndoRedoHistory>cast().name("undoRedoHistory").constr(GSUndoRedoHistorySessionField::new).def(GSUndoRedoHistory::new).assignOnce().codec(UNDO_REDO_HISTORY_CODEC).noSync().session(GSESessionType.COMPOSITION).session(GSESessionType.SEQUENCE).build();
	
		COMPOSITION = builder.<GSComposition>cast().name("composition").constr(GSCompositionSessionField::new).nullable().assignOnce().codec(COMPOSITION_CODEC).noCache().noSync().session(GSESessionType.COMPOSITION).build();
		GAMETICK_WIDTH = builder.<Double>cast().name("gametickWidth").def(8.0).codec(DOUBLE_CODEC).session(GSESessionType.COMPOSITION).build();
		
		SEQUENCE = builder.<GSSequence>cast().name("sequence").constr(GSSequenceSessionField::new).nullable().assignOnce().codec(SEQUENCE_CODEC).noCache().noSync().session(GSESessionType.SEQUENCE).build();
		SELECTED_CHANNEL = builder.<UUID>cast().name("selectedChannel").nullable().codec(UUID_CODEC).session(GSESessionType.SEQUENCE).build();
		MIN_EXPANDED_COLUMN = builder.<Integer>cast().name("minExpandedColumn").def(-1).codec(INTEGER_CODEC).session(GSESessionType.SEQUENCE).build();
		MAX_EXPANDED_COLUMN = builder.<Integer>cast().name("maxExpandedColumn").def(-1).codec(INTEGER_CODEC).session(GSESessionType.SEQUENCE).build();
	}
	
	private final GSESessionType type;
	private GSSessionSide side;
	
	private final GSSessionFields fields;
	private List<GSISessionListener> listeners;
	
	private Set<GSSessionFieldType<?>> fieldsToSync;

	public GSSession(GSESessionType type) {
		if (type == null)
			throw new IllegalArgumentException("type is null");
		
		this.type = type;
		side = GSSessionSide.UNKNOWN;
		
		this.fields = new GSSessionFields(this);
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
	
	public GSSessionSide getSide() {
		return side;
	}

	public void setSide(GSSessionSide side) {
		if (side == null)
			throw new IllegalArgumentException("side is null");
		this.side = side;
	}
	
	public <T> boolean contains(GSSessionFieldType<T> type) {
		return fields.contains(type);
	}

	/* Visible for GSFieldSessionDelta */
	<T> void forceSet(GSSessionFieldPair<T> pair) {
		fields.forceSet(pair);
		onFieldChanged(pair.getType(), true);
	}
	
	/* Visible for GSFieldSessionDelta */
	<T> boolean set(GSSessionFieldPair<T> pair) {
		return set(pair.getType(), pair.getValue());
	}
	
	public <T> boolean set(GSSessionFieldType<T> type, T value) {
		if (fields.set(type, value)) {
			onFieldChanged(type, true);
			return true;
		}
		
		return false;
	}

	public <T> T get(GSSessionFieldType<T> type) {
		return fields.get(type);
	}

	/* Visible for sub-classes of GSISessionDelta */
	GSSessionField<?> getField(GSSessionFieldType<?> type) {
		return fields.getField(type);
	}
	
	private void onFieldChanged(GSSessionFieldType<?> type, boolean shouldRequestSync) {
		if (shouldRequestSync)
			requestSync(type);
		dispatchFieldChanged(type);
	}
	
	public void requestSync(GSSessionFieldType<?> type) {
		if (type.isSynced())
			fieldsToSync.add(type);
	}
	
	public void cancelSync(GSSessionFieldType<?> type) {
		fieldsToSync.remove(type);
	}
	
	public void sync() {
		if (!fieldsToSync.isEmpty()) {
			@SuppressWarnings("unchecked")
			GSIDelta<GSSession>[] deltas = new GSIDelta[fieldsToSync.size()];
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
	void dispatchSessionDelta(GSIDelta<GSSession> delta) {
		@SuppressWarnings("unchecked")
		GSIDelta<GSSession>[] deltas = new GSIDelta[] { delta };
		dispatchSessionDeltas(deltas);
	}
	
	private void dispatchSessionDeltas(GSIDelta<GSSession>[] deltas) {
		if (listeners != null) {
			for (GSISessionListener listener : listeners)
				listener.onSessionDeltas(this, deltas);
		}
	}
	
	public void applySessionDeltas(GSIDelta<GSSession>[] deltas) {
		for (GSIDelta<GSSession> delta : deltas) {
			try {
				delta.apply(this);
			} catch (GSDeltaException ignore) {
			}
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
		
		GSSession session = new GSSession(sessionType);
		
		GSSessionFields fields = GSSessionFields.read(buf);
		for (GSSessionFieldPair<?> pair : fields)
			session.forceSet(pair);
		
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
