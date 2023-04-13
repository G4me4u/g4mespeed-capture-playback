package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.function.Predicate;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSAssetHandle implements Comparable<GSAssetHandle> {

	/* Note: must be greater than length of Integer.toString(Integer.MAX_VALUE) */
	private static final int HANDLE_MAX_LENGTH = 20;
	
	private static final int  BASE62_MAX = 62;
	private static final int  BASE62_POW_4 = BASE62_MAX * BASE62_MAX * BASE62_MAX * BASE62_MAX;
	private static final long BASE62_POW_8 = (long)BASE62_POW_4 * BASE62_POW_4;

	private static final char[] BASE62_ALPHABET;
	private static final SecureRandom secureRandom = new SecureRandom();
	
	public static final char NAMESPACE_SEPARATOR = ':';
	
	static {
		// Generate alphabet in order 0-9, A-Z, a-z
		BASE62_ALPHABET = new char[BASE62_MAX];
		int i = 0;
		for (char c = '0'; c <= '9'; c++)
			BASE62_ALPHABET[i++] = c;
		for (char c = 'A'; c <= 'Z'; c++)
			BASE62_ALPHABET[i++] = c;
		for (char c = 'a'; c <= 'z'; c++)
			BASE62_ALPHABET[i++] = c;
		//assert i == BASE
	}
	
	private final GSEAssetNamespace namespace;
	private final String handle;
	
	private String identifierCache;

	private GSAssetHandle(GSEAssetNamespace namespace, String handle) {
		if (namespace == null)
			throw new IllegalArgumentException("namespace is null!");
		// Note: Intentional null-pointer exception
		if (handle.isEmpty())
			throw new IllegalArgumentException("Handle is empty!");
		if (handle.length() > HANDLE_MAX_LENGTH)
			throw new IllegalArgumentException("Handle length exceeds maximum!");
		if (!isBase62OrUnderscore(handle))
			throw new IllegalArgumentException("Handle contains non-base62/underscore characters!");
		this.namespace = namespace;
		this.handle = handle;
		// Cache for #toString() method
		identifierCache = null;
	}

	private static boolean isBase62OrUnderscore(String handle) {
		for (int i = 0; i < handle.length(); i++) {
			if (!isBase62OrUnderscore(handle.charAt(i)))
				return false;
		}
		return true;
	}

	private static boolean isBase62OrUnderscore(char c) {
		return isBase62(c) || c == '_';
	}
	
	private static boolean isBase62(char c) {
		if ('0' <= c && c <= '9')
			return true;
		if ('A' <= c && c <= 'Z')
			return true;
		if ('a' <= c && c <= 'z')
			return true;
		return false;
	}
	
	public static GSAssetHandle randomBase62(GSEAssetNamespace namespace, int length) {
		if (length <= 0)
			throw new IllegalArgumentException("length must be positive!");
		if (length > HANDLE_MAX_LENGTH)
			throw new IllegalArgumentException("length must be at most " + HANDLE_MAX_LENGTH);
		
		char[] result = new char[length];
		
		int i = 0;
		while (i < length) {
			long value = secureRandom.nextLong(BASE62_POW_8);
			// Convert value to the base62 alphabet.
			for (int j = 0; j < 8 && i < length; j++) {
				result[i++] = BASE62_ALPHABET[(int)(value % BASE62_MAX)];
				value /= BASE62_MAX;
			}
		}
		
		return new GSAssetHandle(namespace, new String(result));
	}
	
	public static GSAssetHandle randomBase62Unique(GSEAssetNamespace namespace, int length, Predicate<GSAssetHandle> existsPred) {
		GSAssetHandle result;
		do {
			result = randomBase62(namespace, length);
		} while (existsPred.test(result));
		
		return result;
	}
	
	public static final GSAssetHandle fromName(GSEAssetNamespace namespace, String name, String suffix) {
		StringBuilder sb = new StringBuilder(HANDLE_MAX_LENGTH);
		// Build handle from name. Symbols are replaced with the
		// underscore character ('_'), where two consecutive symbols
		// corresponds to a single underscore.
		for (int i = 0; i < name.length() && sb.length() < HANDLE_MAX_LENGTH - suffix.length(); i++) {
			char c = Character.toLowerCase(name.charAt(i));
			if (isBase62(c)) {
				sb.append(c);
			} else {
				// Replace non-base62 char by underscore.
				if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '_')
					sb.append('_');
			}
		}
		// Finally, append the suffix
		sb.append(suffix);
		return new GSAssetHandle(namespace, sb.toString());
	}
	
	public static GSAssetHandle fromNameUnique(GSEAssetNamespace namespace, String name, Predicate<GSAssetHandle> existsPred) {
		int i = 1;
		GSAssetHandle result;
		do {
			String suffix = (i > 1 || name.isEmpty()) ? Integer.toString(i) : "";
			result = fromName(namespace, name, suffix);
			i++;
			// In the (very) unlikely case of overflow
			if (i < 0)
				throw new IllegalStateException("Unable to create unique handle");
		} while (existsPred.test(result));
		
		return result;
	}
	
	public static GSAssetHandle fromString(String identifier) {
		// The format of the identifier is:
		//     <namespace>:<handle>
		// where the namespace is a single character.
		if (identifier.length() < 2 || identifier.charAt(1) != NAMESPACE_SEPARATOR)
			throw new IllegalArgumentException("Identifier does not specify a namespace!");
		// Decode the namespace
		GSEAssetNamespace namespace = GSEAssetNamespace.fromIdentifier(identifier.charAt(0));
		if (namespace == null)
			throw new IllegalArgumentException("Unknown namespace '" + identifier.charAt(0) + "'.");
		// The remainder (after separator) is the handle.
		return new GSAssetHandle(namespace, identifier.substring(2));
	}
	
	public GSEAssetNamespace getNamespace() {
		return namespace;
	}
	
	public String getHandle() {
		return handle;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + namespace.hashCode();
		hash = 31 * hash + handle.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSAssetHandle) {
			GSAssetHandle other = (GSAssetHandle)obj;
			if (namespace != other.namespace)
				return false;
			return handle.equals(other.handle);
		}
		return false;
	}
	
	@Override
	public int compareTo(GSAssetHandle other) {
		if (namespace != other.namespace)
			return namespace.getIndex() < other.namespace.getIndex() ? -1 : 1;
		return handle.compareTo(other.handle);
	}
	
	@Override
	public String toString() {
		if (identifierCache == null) {
			StringBuilder sb = new StringBuilder(2 + handle.length());
			sb.append(namespace.getIdentifier());
			sb.append(NAMESPACE_SEPARATOR);
			sb.append(handle);
			identifierCache = sb.toString();
		}
		return identifierCache;
	}
	
	public static GSAssetHandle read(GSDecodeBuffer buf) throws IOException {
		GSEAssetNamespace namespace = GSEAssetNamespace.fromIndex(buf.readUnsignedByte());
		if (namespace == null)
			throw new IllegalArgumentException("Unknown namespace");
		String handle = buf.readString(HANDLE_MAX_LENGTH);
		return new GSAssetHandle(namespace, handle);
	}
	
	public static void write(GSEncodeBuffer buf, GSAssetHandle handle) throws IOException {
		buf.writeUnsignedByte((short)handle.namespace.getIndex());
		buf.writeString(handle.handle);
	}
}
