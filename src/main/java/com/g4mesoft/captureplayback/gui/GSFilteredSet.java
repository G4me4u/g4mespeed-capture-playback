package com.g4mesoft.captureplayback.gui;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class GSFilteredSet<T extends Comparable<T>> extends AbstractSet<T> {

	private final GSSearchFilter<T> filter;
	private final SortedSet<T> values;
	
	public GSFilteredSet(GSSearchFilter<T> filter) {
		if (filter == null)
			throw new IllegalArgumentException("filter is null!");
		this.filter = filter;
		values = new TreeSet<>(filter);
	}
	
    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
	@Override
	public boolean contains(Object o) {
		return values.contains(o);
	}
	
    /**
     * {@inheritDoc}
     *
     * @return true, if the value passes the search filter, and
     *         did not already exist in the set prior to adding.
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
	@Override
	public boolean add(T value) {
		if (filter.filter(value))
			return values.add(value);
		return false;
	}

    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
	@Override
	public boolean remove(Object o) {
		return values.remove(o);
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
	public Iterator<T> iterator() {
		return values.iterator();
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public int size() {
		return values.size();
	}
}
