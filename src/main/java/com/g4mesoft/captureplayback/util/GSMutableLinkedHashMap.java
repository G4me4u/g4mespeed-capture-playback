package com.g4mesoft.captureplayback.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class GSMutableLinkedHashMap<K, V> implements Map<K, V> {

	private final Map<K, GSNode<K, V>> nodes;
	
	private GSNode<K, V> first;
	private GSNode<K, V> last;
	
	private GSKeySet keySet;
	private GSValues values;
	private GSEntrySet entrySet;
	
	public GSMutableLinkedHashMap() {
		nodes = new HashMap<K, GSNode<K, V>>();
		first = last = null;
	}
	
    public GSMutableLinkedHashMap(int initialCapacity) {
		nodes = new HashMap<K, GSNode<K, V>>(initialCapacity);
		first = last = null;
    }
	
	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return nodes.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if (value == null)
			throw new NullPointerException("value is null");
		
		for (GSNode<K, V> node = first; node != null; node = node.next) {
			if (value.equals(node.value))
				return true;
		}
		
		return false;
	}

	@Override
	public V get(Object key) {
		GSNode<K, V> node = nodes.get(key);
		return (node == null) ? null : node.value;
	}

	public V getFirst() {
		return (first == null) ? null : first.value;
	}
	
	public Map.Entry<K, V> getFirstEntry() {
		return first;
	}
	
	public V getLast() {
		return (last == null) ? null : last.value;
	}

	public Map.Entry<K, V> getLastEntry() {
		return last;
	}
	
	public V getPrevious(K key) {
		Map.Entry<K, V> entry = getPreviousEntry(key);
		return (entry == null) ? null : entry.getValue();
	}

	public Map.Entry<K, V> getPreviousEntry(K key) {
		GSNode<K, V> node = nodes.get(key);
		if (node == null)
			return last;
		return (node.prev == null) ? null : node.prev;
	}
	
	public V getNext(K key) {
		Map.Entry<K, V> entry = getNextEntry(key);
		return (entry == null) ? null : entry.getValue();
	}

	public Map.Entry<K, V> getNextEntry(K key) {
		GSNode<K, V> node = nodes.get(key);
		if (node == null)
			return first;
		return (node.next == null) ? null : node.next;
	}
	
	@Override
	public V put(K key, V value) {
		return putLast(key, value);
	}

	public V putFirst(K key, V value) {
		GSNode<K, V> node = new GSNode<K, V>(key, value);
		GSNode<K, V> prev = nodes.put(key, node);
		
		if (prev != null)
			removeNode(prev);
		
		insertNodeFirst(node);
		
		return (prev == null) ? null : prev.value;
	}

	private void insertNodeFirst(GSNode<K, V> node) {
		if (first == null) {
			first = last = node;
		} else {
			first.prev = node;
			node.next = first;
			first = node;
		}
	}
	
	public V putLast(K key, V value) {
		GSNode<K, V> node = new GSNode<K, V>(key, value);
		GSNode<K, V> prev = nodes.put(key, node);
		
		if (prev != null)
			removeNode(prev);
		
		insertNodeLast(node);
		
		return (prev == null) ? null : prev.value;
	}
	
	private void insertNodeLast(GSNode<K, V> node) {
		if (last == null) {
			first = last = node;
		} else {
			last.next = node;
			node.prev = last;
			last = node;
		}
	}
	
	@Override
	public V remove(Object key) {
		GSNode<K, V> node = nodes.remove(key);

		if (node != null) {
			removeNode(node);
			return node.value;
		}
		
		return null;
	}
	
	private void removeNode(GSNode<K, V> node) {
		GSNode<K, V> prev = node.prev;
		GSNode<K, V> next = node.next;
		
		if (prev == null) {
			first = next;
		} else {
			prev.next = next;
			node.prev = null;
		}

		if (next == null) {
			last = prev;
		} else {
			next.prev = prev;
			node.next = null;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	@Override
	public void clear() {
		nodes.clear();
		
		for (GSNode<K, V> node = first; node != null; ) {
			GSNode<K, V> tmpNext = node.next;
			node.next = node.prev = null;
			node = tmpNext;
		}
		
		first = last = null;
	}

	@Override
	public Set<K> keySet() {
		if (keySet == null)
			keySet = new GSKeySet();
		return keySet;
	}

	@Override
	public Collection<V> values() {
		if (values == null)
			values = new GSValues();
		return values;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		if (entrySet == null)
			entrySet = new GSEntrySet();
		return entrySet;
	}
	
	public Map.Entry<K, V> moveAfter(K key, K otherKey) {
		GSNode<K, V> srcNode = nodes.get(key);
		GSNode<K, V> dstNode = nodes.get(otherKey);
	
		if (srcNode == null)
			throw new NoSuchElementException();
		if (srcNode == dstNode)
			throw new IllegalArgumentException("key and otherKey are the same");

		removeNode(srcNode);
		
		if (dstNode != null) {
			srcNode.next = dstNode.next;
			srcNode.prev = dstNode;
			dstNode.next = srcNode;

			if (srcNode.next == null) {
				last = srcNode;
			} else {
				srcNode.next.prev = srcNode;
			}
		} else {
			insertNodeFirst(srcNode);
		}
		
		return srcNode;
	}

	public Map.Entry<K, V> moveBefore(K key, K otherKey) {
		GSNode<K, V> srcNode = nodes.get(key);
		GSNode<K, V> dstNode = nodes.get(otherKey);
	
		if (srcNode == null)
			throw new NoSuchElementException();
		if (srcNode == dstNode)
			throw new IllegalArgumentException("key and otherKey are the same");

		removeNode(srcNode);
		
		if (dstNode != null) {
			srcNode.prev = dstNode.prev;
			srcNode.next = dstNode;
			dstNode.prev = srcNode;

			if (srcNode.prev == null) {
				first = srcNode;
			} else {
				srcNode.prev.next = srcNode;
			}
		} else {
			insertNodeLast(srcNode);
		}
		
		return srcNode;
	}
	
	final class GSKeySet extends AbstractSet<K> {

		@Override
		public int size() {
			return GSMutableLinkedHashMap.this.size();
		}

		@Override
		public void clear() {
			GSMutableLinkedHashMap.this.clear();
		}

		@Override
		public Iterator<K> iterator() {
			return new GSKeyIterator();
		}

		@Override
		public boolean contains(Object obj) {
			return containsKey(obj);
		}

		@Override
		public boolean remove(Object obj) {
			return GSMutableLinkedHashMap.this.remove(obj) != null;
		}

		@Override
		public void forEach(Consumer<? super K> action) {
			if (action == null)
				throw new NullPointerException("action is null");
			
			for (GSNode<K, V> node = first; node != null; node = node.next)
				action.accept(node.key);
		}
	}
	
	final class GSValues extends AbstractCollection<V> {

		@Override
		public int size() {
			return GSMutableLinkedHashMap.this.size();
		}

		@Override
		public void clear() {
			GSMutableLinkedHashMap.this.clear();
		}

		public Iterator<V> iterator() {
			return new GSValueIterator();
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

		@Override
		public final void forEach(Consumer<? super V> action) {
			if (action == null)
				throw new NullPointerException("action is null");
			
			for (GSNode<K, V> node = first; node != null; node = node.next)
				action.accept(node.value);
		}
	}
	
	final class GSEntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public int size() {
			return GSMutableLinkedHashMap.this.size();
		}

		@Override
		public void clear() {
			GSMutableLinkedHashMap.this.clear();
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new GSEntryIterator();
		}

		@Override
		public boolean contains(Object obj) {
			if (!(obj instanceof Map.Entry))
				return false;
			
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>)obj;

			GSNode<K, V> node = nodes.get(entry.getKey());
			return (node != null && node.equals(entry));
		}

		@Override
		public boolean remove(Object obj) {
			if (obj instanceof Map.Entry) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>)obj;
				
				Object key = entry.getKey();
				Object value = entry.getValue();
				
				GSNode<K, V> node = nodes.get(key);
				if (node != null && Objects.equals(value, node.value)) {
					GSMutableLinkedHashMap.this.remove(key);
					return true;
				}
			}
			
			return false;
		}

		@Override
		public void forEach(Consumer<? super Map.Entry<K, V>> action) {
			if (action == null)
				throw new NullPointerException("action is null");
			
			for (GSNode<K, V> node = first; node != null; node = node.next)
				action.accept(node);
		}
	}
	
	abstract class GSMapIterator<E> implements Iterator<E> {

		private GSNode<K, V> prev;
		private GSNode<K, V> next;
		
		public GSMapIterator() {
			prev = null;
			next = first;
		}
		
		@Override
		public boolean hasNext() {
			return (next != null);
		}

		public GSNode<K, V> nextNode() {
			if (next == null)
				throw new NoSuchElementException();
			
			prev = next;
			next = next.next;
			
			return prev;
		}
		
		@Override
	    public void remove() {
			if (prev == null)
				throw new IllegalStateException();
			
			GSMutableLinkedHashMap.this.remove(prev.key);
			prev = null;
		}
	}
	
	final class GSValueIterator extends GSMapIterator<V> {

		@Override
		public V next() {
			return nextNode().value;
		}
	}

	final class GSKeyIterator extends GSMapIterator<K> {
		
		@Override
		public K next() {
			return nextNode().key;
		}
	}

	final class GSEntryIterator extends GSMapIterator<Map.Entry<K, V>> {
		
		@Override
		public Map.Entry<K, V> next() {
			return nextNode();
		}
	}
	
	private static class GSNode<K, V> implements Map.Entry<K, V> {
		
		private final K key;
		private V value;
		
		private GSNode<K, V> prev;
		private GSNode<K, V> next;
		
		public GSNode(K key, V value) {
			this.key = key;
			this.value = value;
		
			prev = next = null;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			return this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(key) ^ Objects.hashCode(value);
		}

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;

			if (other instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>)other;
				if (Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue()))
					return true;
			}

			return false;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
}
