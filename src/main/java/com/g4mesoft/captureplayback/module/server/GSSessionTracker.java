package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionSide;
import com.g4mesoft.captureplayback.session.GSSessionStartPacket;
import com.g4mesoft.captureplayback.session.GSSessionStopPacket;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.util.GSFileUtil;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionTracker implements GSICompositionListener, GSISessionListener {

	private static final String SEQUENCE_SESSION_DIRECTORY_NAME = "sequence";
	private static final String LATEST_SESSION_DIRECTORY_NAME = "latest";
	private static final String SESSION_EXTENSION = ".session";
	
	private final GSIServerModuleManager manager;
	private final GSComposition composition;
	
	private final File cacheDir;
	private final File sequenceCacheDir;
	
	private final Map<GSESessionType, Map<UUID, GSSession>> playerSessionsFromType;
	private final Map<GSSession, UUID> sessionToPlayerUUID;
	private final Map<UUID, Set<UUID>> sequenceUUIDToSession;
	
	public GSSessionTracker(GSIServerModuleManager manager, GSComposition composition, File cacheDir) {
		this.manager = manager;
		this.composition = composition;
		
		this.cacheDir = cacheDir;
		sequenceCacheDir = new File(cacheDir, SEQUENCE_SESSION_DIRECTORY_NAME);
		
		playerSessionsFromType = new EnumMap<>(GSESessionType.class);
		sessionToPlayerUUID = new IdentityHashMap<>();
		sequenceUUIDToSession = new HashMap<>();
	}
	
	/* TODO: rewrite all of this */
	
	public void install() {
		composition.addCompositionListener(this);
	}

	public void uninstall() {
		composition.removeCompositionListener(this);
	}
	
	private void addSession(ServerPlayerEntity player, GSSession session) {
		Map<UUID, GSSession> playerSessions = playerSessionsFromType.get(session.getType());
		if (playerSessions == null) {
			playerSessions = new HashMap<>();
			playerSessionsFromType.put(session.getType(), playerSessions);
		}
		playerSessions.put(player.getUuid(), session);
		session.setSide(GSSessionSide.SERVER_SIDE);
		
		if (sessionToPlayerUUID.put(session, player.getUuid()) != null)
			throw new IllegalStateException("Session is already added");
	}

	private GSSession removeSession(ServerPlayerEntity player, GSESessionType sessionType) {
		Map<UUID, GSSession> playerSessions = playerSessionsFromType.get(sessionType);
		if (playerSessions != null) {
			GSSession session = playerSessions.remove(player.getUuid());
			sessionToPlayerUUID.remove(session);
			return session;
		}
		
		return null;
	}
	
	private GSSession getSession(ServerPlayerEntity player, GSESessionType sessionType) {
		Map<UUID, GSSession> playerSessions = playerSessionsFromType.get(sessionType);
		return (playerSessions == null) ? null : playerSessions.get(player.getUuid());
	}

	public boolean startCompositionSession(ServerPlayerEntity player) {
		if (getSession(player, GSESessionType.COMPOSITION) != null)
			stopCompositionSession(player);
		
		GSSession session = readCompositionSession(player);
		if (session == null)
			session = new GSSession(GSESessionType.COMPOSITION);
		session.set(GSSession.COMPOSITION, composition);
		
		addSession(player, session);
		session.addListener(this);

		manager.sendPacket(new GSSessionStartPacket(session), player);
		
		return true;
	}

	public void stopCompositionSession(ServerPlayerEntity player) {
		// Stop any potential sequence sessions.
		stopSequenceSession(player);
		
		GSSession session = removeSession(player, GSESessionType.COMPOSITION);
		if (session != null)
			onCompositionSessionStopped(player, session);
	}
	
	private void onCompositionSessionStopped(ServerPlayerEntity player, GSSession session) {
		writeCompositionSession(player, session);
		session.removeListener(this);
		manager.sendPacket(new GSSessionStopPacket(session.getType()), player);
	}

	public boolean startSequenceSession(ServerPlayerEntity player, UUID trackUUID) {
		if (getSession(player, GSESessionType.SEQUENCE) != null)
			stopSequenceSession(player);

		GSTrack track = composition.getTrack(trackUUID);
		if (track != null) {
			GSSession session = readSequenceSession(player, trackUUID.toString());
			if (session == null) {
				session = readSequenceSession(player, LATEST_SESSION_DIRECTORY_NAME);
				if (session == null || session.getType() != GSESessionType.SEQUENCE)
					session = new GSSession(GSESessionType.SEQUENCE);
			}
			session.set(GSSession.SEQUENCE, track.getSequence());
			addSession(player, session);

			UUID sequenceUUID = track.getSequence().getSequenceUUID();
			Set<UUID> playerUUIDs = sequenceUUIDToSession.get(sequenceUUID);
			if (playerUUIDs == null) {
				playerUUIDs = new HashSet<>();
				sequenceUUIDToSession.put(sequenceUUID, playerUUIDs);
			}
			playerUUIDs.add(player.getUuid());
			session.addListener(this);

			manager.sendPacket(new GSSessionStartPacket(session), player);
		}
			
		return true;
	}

	public void stopSequenceSession(ServerPlayerEntity player) {
		GSSession session = removeSession(player, GSESessionType.SEQUENCE);
		if (session != null)
			onSequenceSessionStopped(player, session);
	}
	
	private void onSequenceSessionStopped(ServerPlayerEntity player, GSSession session) {
		UUID sequenceUUID = session.get(GSSession.SEQUENCE).getSequenceUUID();

		writeSequenceSession(player, sequenceUUID.toString(), session);
		writeSequenceSession(player, LATEST_SESSION_DIRECTORY_NAME, session);
		
		Set<UUID> playerUUIDs = sequenceUUIDToSession.get(sequenceUUID);
		if (playerUUIDs != null && playerUUIDs.remove(player.getUuid()) && playerUUIDs.isEmpty())
			sequenceUUIDToSession.remove(sequenceUUID);
		
		session.removeListener(this);

		manager.sendPacket(new GSSessionStopPacket(session.getType()), player);
	}

	public void stopAllSessions() {
		for (Map<UUID, GSSession> playerSessions : playerSessionsFromType.values()) {
			for (Map.Entry<UUID, GSSession> entry : playerSessions.entrySet()) {
				ServerPlayerEntity player = manager.getPlayer(entry.getKey());
				if (player != null) {
					switch (entry.getValue().getType()) {
					case COMPOSITION:
						onCompositionSessionStopped(player, entry.getValue());
						break;
					case SEQUENCE:
						onSequenceSessionStopped(player, entry.getValue());
						break;
					}
				}
			}
			playerSessions.clear();
		}
		playerSessionsFromType.clear();
		sessionToPlayerUUID.clear();
		
		// Should already be cleared, but clear for safety.
		sequenceUUIDToSession.clear();
	}
	
	public GSSession readCompositionSession(ServerPlayerEntity player) {
		try {
			return GSFileUtil.readFile(getCompositionSessionFile(player), GSSession::read);
		} catch (IOException ignore) {
			return null;
		}
	}

	public void writeCompositionSession(ServerPlayerEntity player, GSSession session) {
		try {
			GSFileUtil.writeFile(getCompositionSessionFile(player), session, GSSession::writeCache);
		} catch (IOException ignore) {
		}
	}
	
	public GSSession readSequenceSession(ServerPlayerEntity player, String trackIdentifier) {
		try {
			return GSFileUtil.readFile(getSequenceSessionFile(player, trackIdentifier), GSSession::read);
		} catch (IOException ignore) {
			return null;
		}
	}

	public void writeSequenceSession(ServerPlayerEntity player, String trackIdentifier, GSSession session) {
		try {
			GSFileUtil.writeFile(getSequenceSessionFile(player, trackIdentifier), session, GSSession::writeCache);
		} catch (IOException ignore) {
		}
	}
	
	private File getSequenceSessionFile(ServerPlayerEntity player, String trackIdentifier) {
		return getSessionFile(player, new File(sequenceCacheDir, trackIdentifier));
	}

	private File getCompositionSessionFile(ServerPlayerEntity player) {
		return getSessionFile(player, cacheDir);
	}
	
	private File getSessionFile(ServerPlayerEntity player, File cacheDir) {
		return new File(cacheDir, player.getUuid().toString() + SESSION_EXTENSION);
	}
	
	public GSComposition getComposition() {
		return composition;
	}
	
	public void onDeltasReceived(ServerPlayerEntity player, GSESessionType sessionType, GSIDelta<GSSession>[] deltas) {
		GSSession session = getSession(player, sessionType);
		if (session != null)
			session.applySessionDeltas(deltas);
	}

	@Override
	public void trackRemoved(GSTrack track) {
		Set<UUID> playerUUIDs = sequenceUUIDToSession.remove(track.getSequence().getSequenceUUID());

		if (playerUUIDs != null) {
			for (UUID playerUUID : playerUUIDs) {
				ServerPlayerEntity player = manager.getPlayer(playerUUID);
				if (player != null)
					stopSequenceSession(player);
			}
		}
	}
	
	@Override
	public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
		UUID playerUUID = sessionToPlayerUUID.get(session);
		
		if (playerUUID != null) {
			ServerPlayerEntity player = manager.getPlayer(playerUUID);
			if (player != null)
				manager.sendPacket(new GSSessionDeltasPacket(session.getType(), deltas), player);
		}
	}
}
