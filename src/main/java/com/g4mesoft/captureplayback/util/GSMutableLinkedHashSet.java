package com.g4mesoft.captureplayback.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GSMutableLinkedHashSet<E> extends AbstractSet<E> implements Set<E> {

	private static final Object PRESENT = new Object();
	
	private final GSMutableLinkedHashMap<E, Object> map;
	
    public GSMutableLinkedHashSet() {
        map = new GSMutableLinkedHashMap<>();
    }

    public GSMutableLinkedHashSet(int initialCapacity) {
        map = new GSMutableLinkedHashMap<>(initialCapacity);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

	public E getFirst() {
		Map.Entry<E, Object> entry = map.getFirstEntry();
		return (entry == null) ? null : entry.getKey();
	}
	
	public E getLast() {
		Map.Entry<E, Object> entry = map.getFirstEntry();
		return (entry == null) ? null : entry.getKey();
	}
	
	public E getPrevious(E e) {
		Map.Entry<E, Object> entry = map.getPreviousEntry(e);
		return (entry == null) ? null : entry.getKey();
	}

	public E getNext(E e) {
		Map.Entry<E, Object> entry = map.getNextEntry(e);
		return (entry == null) ? null : entry.getKey();
	}
    
    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }
    
	public boolean addFirst(E e) {
		return map.putFirst(e, PRESENT) == null;
	}

	public boolean addLast(E e) {
		return map.putLast(e, PRESENT) == null;
	}

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }
    
	public boolean moveAfter(E e, E other) {
		return map.moveAfter(e, other) != null;
	}

	public boolean moveBefore(E e, E other) {
		return map.moveBefore(e, other) != null;
	}
}
