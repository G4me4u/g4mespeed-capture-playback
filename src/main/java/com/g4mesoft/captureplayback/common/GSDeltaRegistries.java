package com.g4mesoft.captureplayback.common;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackColorDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryOffsetDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackGroupDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackSequenceDelta;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelDisabledDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryTimeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryTypeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelInfoDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelMovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceNameDelta;
import com.g4mesoft.captureplayback.session.GSCompositionSessionDelta;
import com.g4mesoft.captureplayback.session.GSFieldSessionDelta;
import com.g4mesoft.captureplayback.session.GSMoveUndoRedoHistoryDelta;
import com.g4mesoft.captureplayback.session.GSSequenceSessionDelta;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSTrackUndoRedoHistoryDelta;
import com.g4mesoft.captureplayback.session.GSUndoRedoHistory;
import com.g4mesoft.captureplayback.session.GSUndoRedoHistorySessionDelta;

public final class GSDeltaRegistries {

	public static final GSDeltaRegistry<GSSequence>        SEQUENCE_DELTA_REGISTRY;
	public static final GSDeltaRegistry<GSComposition>     COMPOSITION_DELTA_REGISTRY;
	public static final GSDeltaRegistry<GSSession>         SESSION_DELTA_REGISTRY;
	public static final GSDeltaRegistry<GSUndoRedoHistory> UNDO_REDO_HISTORY_DELTA_REGISTRY;
	
	static {
		SEQUENCE_DELTA_REGISTRY = new GSDeltaRegistry<>();
		SEQUENCE_DELTA_REGISTRY.register(0, GSSequenceNameDelta.class, GSSequenceNameDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(1, GSChannelAddedDelta.class, GSChannelAddedDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(2, GSChannelRemovedDelta.class, GSChannelRemovedDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(3, GSChannelInfoDelta.class, GSChannelInfoDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(4, GSChannelDisabledDelta.class, GSChannelDisabledDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(5, GSChannelEntryAddedDelta.class, GSChannelEntryAddedDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(6, GSChannelEntryRemovedDelta.class, GSChannelEntryRemovedDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(7, GSChannelEntryTimeDelta.class, GSChannelEntryTimeDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(8, GSChannelEntryTypeDelta.class, GSChannelEntryTypeDelta::new);
		SEQUENCE_DELTA_REGISTRY.register(9, GSChannelMovedDelta.class, GSChannelMovedDelta::new);
		
		COMPOSITION_DELTA_REGISTRY = new GSDeltaRegistry<>();
		COMPOSITION_DELTA_REGISTRY.register( 0, GSCompositionNameDelta.class, GSCompositionNameDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 1, GSGroupAddedDelta.class, GSGroupAddedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 2, GSGroupRemovedDelta.class, GSGroupRemovedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 3, GSGroupNameDelta.class, GSGroupNameDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 4, GSTrackAddedDelta.class, GSTrackAddedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 5, GSTrackRemovedDelta.class, GSTrackRemovedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 6, GSTrackNameDelta.class, GSTrackNameDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 7, GSTrackColorDelta.class, GSTrackColorDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 8, GSTrackGroupDelta.class, GSTrackGroupDelta::new);
		COMPOSITION_DELTA_REGISTRY.register( 9, GSTrackSequenceDelta.class, GSTrackSequenceDelta::new);
		COMPOSITION_DELTA_REGISTRY.register(10, GSTrackEntryAddedDelta.class, GSTrackEntryAddedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register(11, GSTrackEntryRemovedDelta.class, GSTrackEntryRemovedDelta::new);
		COMPOSITION_DELTA_REGISTRY.register(12, GSTrackEntryOffsetDelta.class, GSTrackEntryOffsetDelta::new);
		
		SESSION_DELTA_REGISTRY = new GSDeltaRegistry<>();
		SESSION_DELTA_REGISTRY.register(0, GSFieldSessionDelta.class, GSFieldSessionDelta::new);
		SESSION_DELTA_REGISTRY.register(1, GSSequenceSessionDelta.class, GSSequenceSessionDelta::new);
		SESSION_DELTA_REGISTRY.register(2, GSCompositionSessionDelta.class, GSCompositionSessionDelta::new);
		SESSION_DELTA_REGISTRY.register(3, GSUndoRedoHistorySessionDelta.class, GSUndoRedoHistorySessionDelta::new);
	
		UNDO_REDO_HISTORY_DELTA_REGISTRY = new GSDeltaRegistry<>();
		UNDO_REDO_HISTORY_DELTA_REGISTRY.register(0, GSMoveUndoRedoHistoryDelta.class, GSMoveUndoRedoHistoryDelta::new);
		UNDO_REDO_HISTORY_DELTA_REGISTRY.register(1, GSTrackUndoRedoHistoryDelta.class, GSTrackUndoRedoHistoryDelta::new);
	}
	
	private GSDeltaRegistries() {
	}
}
