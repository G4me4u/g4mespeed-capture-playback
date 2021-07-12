package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaException;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaTransformer;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDeltaListener;
import com.g4mesoft.captureplayback.module.GSCompositionDeltaPacket;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.GSResetCompositionPacket;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.captureplayback.module.GSStartCompositionSessionPacket;
import com.g4mesoft.captureplayback.module.GSStartSequenceSessionPacket;
import com.g4mesoft.captureplayback.module.GSStopCompositionSessionPacket;
import com.g4mesoft.captureplayback.module.GSStopSequenceSessionPacket;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSFileUtil;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionTracker implements GSICompositionDeltaListener, GSICompositionListener {

	private static final String SEQUENCE_SESSION_DIRECTORY_NAME = "sequence";
	private static final String LATEST_SESSION_DIRECTORY_NAME = "latest";
	private static final String SESSION_EXTENSION = ".session";
	
	private final GSIServerModuleManager manager;
	private final GSComposition composition;
	
	private final File cacheDir;
	private final File sequenceCacheDir;
	
	private final Map<UUID, GSCompositionSession> compositionSessions;
	private final Map<UUID, GSSequenceSession> sequenceSessions;
	
	private final Map<UUID, Set<UUID>> trackToSequenceSession;
	
	private final GSCompositionDeltaTransformer transformer;
	
	public GSSessionTracker(GSIServerModuleManager manager, GSComposition composition, File cacheDir) {
		this.manager = manager;
		this.composition = composition;
		
		this.cacheDir = cacheDir;
		sequenceCacheDir = new File(cacheDir, SEQUENCE_SESSION_DIRECTORY_NAME);
		
		compositionSessions = new HashMap<>();
		sequenceSessions = new HashMap<>();
		
		trackToSequenceSession = new HashMap<>();
		
		transformer = new GSCompositionDeltaTransformer();
		transformer.addDeltaListener(this);
	}
	
	public void install() {
		transformer.install(composition);
		composition.addCompositionListener(this);
	}

	public void uninstall() {
		transformer.uninstall(composition);
		composition.removeCompositionListener(this);
	}

	public boolean startCompositionSession(ServerPlayerEntity player) {
		if (compositionSessions.containsKey(player.getUuid()))
			stopCompositionSession(player);
		
		GSCompositionSession session = readCompositionSession(player);
		if (session == null)
			session = new GSCompositionSession(composition.getCompositionUUID());
		
		compositionSessions.put(player.getUuid(), session);
		manager.sendPacket(new GSStartCompositionSessionPacket(session, composition), player);
		
		return true;
	}

	public void stopCompositionSession(ServerPlayerEntity player) {
		// Stop any potential sequence sessions.
		stopSequenceSession(player);
		
		GSCompositionSession session = compositionSessions.remove(player.getUuid());
		if (session != null)
			onCompositionSessionStopped(player, session);
	}
	
	private void onCompositionSessionStopped(ServerPlayerEntity player, GSCompositionSession session) {
		writeCompositionSession(player, session);
		manager.sendPacket(new GSStopCompositionSessionPacket(), player);
	}

	public boolean startSequenceSession(ServerPlayerEntity player, UUID trackUUID) {
		if (sequenceSessions.containsKey(player.getUuid()))
			stopSequenceSession(player);

		GSTrack track = composition.getTrack(trackUUID);
		if (track != null) {
			GSSequenceSession session = readSequenceSession(player, trackUUID.toString());
			if (session == null) {
				UUID sequenceUUID = track.getSequence().getSequenceUUID();
				session = new GSSequenceSession(composition.getCompositionUUID(), trackUUID, sequenceUUID);
				
				// Copy relevant fields from the latest session
				GSSequenceSession latestSession = readSequenceSession(player, LATEST_SESSION_DIRECTORY_NAME);
				if (latestSession != null)
					session.setRelevantRepeatedFields(latestSession);
			}
			sequenceSessions.put(player.getUuid(), session);

			Set<UUID> playerUUIDs = trackToSequenceSession.get(trackUUID);
			if (playerUUIDs == null) {
				playerUUIDs = new HashSet<>();
				trackToSequenceSession.put(trackUUID, playerUUIDs);
			}
			playerUUIDs.add(player.getUuid());
			
			manager.sendPacket(new GSStartSequenceSessionPacket(session), player);
		}
			
		return true;
	}

	public void stopSequenceSession(ServerPlayerEntity player) {
		GSSequenceSession session = sequenceSessions.remove(player.getUuid());
		if (session != null)
			onSequenceSessionStopped(player, session);
	}
	
	private void onSequenceSessionStopped(ServerPlayerEntity player, GSSequenceSession session) {
		UUID trackUUID = session.getTrackUUID();

		writeSequenceSession(player, trackUUID.toString(), session);
		writeSequenceSession(player, LATEST_SESSION_DIRECTORY_NAME, session);
		
		Set<UUID> playerUUIDs = trackToSequenceSession.get(trackUUID);
		if (playerUUIDs != null && playerUUIDs.remove(player.getUuid()) && playerUUIDs.isEmpty())
			trackToSequenceSession.remove(trackUUID);
		
		manager.sendPacket(new GSStopSequenceSessionPacket(), player);
	}
	
	public void stopAllSessions() {
		for (Map.Entry<UUID, GSSequenceSession> entry : sequenceSessions.entrySet()) {
			ServerPlayerEntity player = manager.getPlayer(entry.getKey());
			if (player != null)
				onSequenceSessionStopped(player, entry.getValue());
		}
		sequenceSessions.clear();
			
		for (Map.Entry<UUID, GSCompositionSession> entry : compositionSessions.entrySet()) {
			ServerPlayerEntity player = manager.getPlayer(entry.getKey());
			if (player != null)
				onCompositionSessionStopped(player, entry.getValue());
		}
		compositionSessions.clear();
	}
	
	public GSCompositionSession readCompositionSession(ServerPlayerEntity player) {
		return readSession(getCompositionSessionFile(player), buf -> {
			try {
				return GSCompositionSession.read(buf);
			} catch (IOException ignore) {
			}
			return null;
		});
	}

	public void writeCompositionSession(ServerPlayerEntity player, GSCompositionSession session) {
		writeSession(getCompositionSessionFile(player), buf -> {
			try {
				GSCompositionSession.write(buf, session);
			} catch (IOException ignore) {
			}
		});
	}
	
	public GSSequenceSession readSequenceSession(ServerPlayerEntity player, String trackIdentifier) {
		return readSession(getSequenceSessionFile(player, trackIdentifier), buf -> {
			try {
				return GSSequenceSession.read(buf);
			} catch (IOException ignore) {
			}
			return null;
		});
	}

	public void writeSequenceSession(ServerPlayerEntity player, String trackIdentifier, GSSequenceSession session) {
		writeSession(getSequenceSessionFile(player, trackIdentifier), buf -> {
			try {
				GSSequenceSession.write(buf, session);
			} catch (IOException ignore) {
			}
		});
	}
	
	private <T> T readSession(File sessionFile, Function<PacketByteBuf, T> decodeFunc) {
		T session = null;
		
		try (FileInputStream fis = new FileInputStream(sessionFile)) {
			byte[] data = IOUtils.toByteArray(fis);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(data));
			session = decodeFunc.apply(buffer);
			buffer.release();
		} catch (Throwable ignore) {
		}
		
		return session;
	}

	private void writeSession(File sessionFile, Consumer<PacketByteBuf> encodeFunc) {
		try {
			GSFileUtil.ensureFileExists(sessionFile);
		
			try (FileOutputStream fos = new FileOutputStream(sessionFile)) {
				PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
				encodeFunc.accept(buffer);
				if (buffer.hasArray()) {
					fos.write(buffer.array(), buffer.arrayOffset(), buffer.writerIndex());
				} else {
					fos.getChannel().write(buffer.nioBuffer());
				}
				buffer.release();
			}
		} catch (Throwable ignore) {
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
	
	@Override
	public void onCompositionDelta(GSICompositionDelta delta) {
		manager.sendPacketToAll(new GSCompositionDeltaPacket(composition.getCompositionUUID(), delta));
	}

	public void onDeltaReceived(ServerPlayerEntity player, GSICompositionDelta delta) {
		if (compositionSessions.containsKey(player.getUuid())) {
			try {
				transformer.setEnabled(false);
				delta.applyDelta(composition);
				
				GSIPacket deltaPacket = new GSCompositionDeltaPacket(composition.getCompositionUUID(), delta);
				for (UUID playerUUID : compositionSessions.keySet()) {
					ServerPlayerEntity otherPlayer = manager.getPlayer(playerUUID);
					if (otherPlayer != player)
						manager.sendPacket(deltaPacket, otherPlayer);
				}
			} catch (GSCompositionDeltaException ignore) {
				// The delta could not be applied. Probably because of a de-sync, or
				// because multiple users are changing the same part of the composition.
				manager.sendPacket(new GSResetCompositionPacket(composition), player);
			} finally {
				transformer.setEnabled(true);
			}
		}
	}

	public void onCompositionSessionChanged(ServerPlayerEntity player, GSCompositionSession session) {
		GSCompositionSession oldSession = compositionSessions.get(player.getUuid());
		
		if (oldSession != null) {
			// Ensure that it is the same session that is being updated.
			if (oldSession.getCompositionUUID().equals(session.getCompositionUUID()))
				compositionSessions.put(player.getUuid(), session);
		}
	}
	
	public void onSequenceSessionChanged(ServerPlayerEntity player, GSSequenceSession session) {
		GSSequenceSession oldSession = sequenceSessions.get(player.getUuid());
		
		if (oldSession != null) {
			// Ensure that it is the same session that is being updated.
			if (!oldSession.getCompositionUUID().equals(session.getCompositionUUID()))
				return;
			if (!oldSession.getTrackUUID().equals(session.getTrackUUID()))
				return;
			if (!oldSession.getSequenceUUID().equals(session.getSequenceUUID()))
				return;
			
			sequenceSessions.put(player.getUuid(), session);
		}
	}
	
	@Override
	public void trackRemoved(GSTrack track) {
		Set<UUID> playerUUIDs = trackToSequenceSession.get(track.getTrackUUID());

		if (playerUUIDs != null) {
			UUID[] playerUUIDArray = playerUUIDs.toArray(new UUID[0]);
			trackToSequenceSession.remove(track.getTrackUUID());
			
			for (UUID playerUUID : playerUUIDArray) {
				ServerPlayerEntity player = manager.getPlayer(playerUUID);
				if (player != null)
					stopSequenceSession(player);
			}
		}
	}
}
