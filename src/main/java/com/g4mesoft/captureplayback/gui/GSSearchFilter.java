package com.g4mesoft.captureplayback.gui;

import java.util.Comparator;

public abstract class GSSearchFilter<T extends Comparable<T>> implements Comparator<T> {

	protected static final float NO_INSERTIONS_ALLOWED   = 0.0f;
	protected static final float HALF_INSERTIONS_ALLOWED = 0.5f;
	
	private static final int VALUE_INDEX_MASK = 0b11;
	
	private static final int SUBSTITUTION_COST  = 4;
	private static final int INSERTION_COST     = 3;
	private static final int DELETION_COST      = 1;
	private static final int LATE_DELETION_COST = 10;
	private static final int TRANSPOSITION_COST = 1;
	
	protected final String pattern;
	private final int[][] dists;
	
	public GSSearchFilter(String pattern) {
		if (pattern == null)
			throw new IllegalArgumentException("pattern is null!");
		this.pattern = pattern.toLowerCase();
		dists = new int[VALUE_INDEX_MASK + 1][this.pattern.length() + 1];
	}

	protected abstract int matchCost(T value);
	
	public final boolean filter(T o) {
		return matchCost(o) != Integer.MAX_VALUE;
	}
	
	@Override
	public final int compare(T o1, T o2) {
		// First ordered by match cost, then natural ordering.
		int c1 = matchCost(o1), c2 = matchCost(o2);
		return (c1 != c2) ? Integer.compare(c1, c2) : o1.compareTo(o2);
	}
	
	/**
	 * Compute the cost of matching the given value by the pattern, or
	 * {@code Integer.MAX_VALUE} if the value does not match anything
	 * in the pattern.
	 * 
	 * @param value
	 * 
	 * @return the cost of matching {@code value} and {@code pattern}.
	 */
	protected final int minimumMatchCost(String value) {
		return minimumMatchCost(value, HALF_INSERTIONS_ALLOWED);
	}
	
	/**
	 * Compute the cost of matching the given value by the pattern, or
	 * {@code Integer.MAX_VALUE} if the value does not match anything
	 * in the pattern.
	 * 
	 * @param value
	 * @param insertFraction - the maximum fraction of allowed pattern
	 *                         skips relative to the pattern length.
	 * 
	 * @return the cost of matching {@code value} and {@code pattern}.
	 */
	protected final int minimumMatchCost(String value, float insertFraction) {
		if (pattern.isEmpty()) {
			// Empty patterns match any value (relative cost zero).
			return 0;
		}
		int cost = weightedEditDistance(value);
		// The maximum edit distance if no characters match.
		int mxLength = Math.max(value.length(), pattern.length());
		if (cost >= mxLength * SUBSTITUTION_COST)
			return Integer.MAX_VALUE;
		// Compute relative cost of matching, with a penalty for
		// patterns longer than value.
		int rel = cost - (mxLength - pattern.length()) * DELETION_COST;
		if (rel > pattern.length() * INSERTION_COST * insertFraction)
			return Integer.MAX_VALUE;
		return rel;
	}
	
	/**
	 * Computes the weighted edit distance (Damerau–Levenshtein distance)
	 * between the {@code value} and {@code pattern} strings. The edit
	 * distance is the minimum number of single-character operations that
	 * are required to make {@code value} equal to {@code pattern}, allowing
	 * insertion, deletion, substitution of single characters, as well as
	 * transposition of two adjacent characters. These operations each have
	 * weights according to the constants in this class. There is also the
	 * notion of a late deletion which occurs after the first character of
	 * the pattern has already been processed.
	 * 
	 * @param value - the value to compute edit distance towards pattern
	 * 
	 * @return the edit distance between {@code value} and {@code pattern}.
	 */
	protected final int weightedEditDistance(String value) {
		int n = value.length(), m = pattern.length();
		if (n == 0 || m == 0) {
			// One of the two are empty, the edit distance is
			// inserting characters to match non-empty string.
			return n * DELETION_COST + m * INSERTION_COST;
		}
		
		// Initialize first row
		dists[0][0] = 0;
		for (int j = 1; j <= m; j++)
			dists[0][j] = j * INSERTION_COST;
		
		// Compute edit distance
		for (int i = 1; i <= n; i++) {
			char si = Character.toLowerCase(value.charAt(i - 1));
			// We only store the current and previous three rows.
			int q   = (i    ) & VALUE_INDEX_MASK;
			int qm1 = (i - 1) & VALUE_INDEX_MASK;
			dists[q][0] = i * DELETION_COST;
			for (int j = 1; j <= m; j++) {
				char pj = pattern.charAt(j - 1); /* already lowercase */
				int mnDist;
				if (pj == si) {
					mnDist = dists[qm1][j - 1];
				} else {
					mnDist = dists[qm1][j - 1] +
							SUBSTITUTION_COST; /* substitution */
				}
				if (j > 1 && j < m) {
					// Deletion has higher cost when we already matched
					// one character of the pattern.
					mnDist = Math.min(mnDist, dists[qm1][j] +
							LATE_DELETION_COST); /* deletion */
				} else {
					mnDist = Math.min(mnDist, dists[qm1][j] +
							DELETION_COST); /* deletion */
				}
				mnDist = Math.min(mnDist, dists[q][j - 1] +
						INSERTION_COST); /* insertion */
				if (i > 1 && j > 1) {
					int sim1 = Character.toLowerCase(value.charAt(i - 2));
					int pjm1 = pattern.charAt(j - 2) /* already lowercase */;
					if (sim1 == pj && pjm1 == si) {
						int qm2 = (i - 2) & VALUE_INDEX_MASK;
						mnDist = Math.min(mnDist, dists[qm2][j - 2] +
								TRANSPOSITION_COST); /* transposition */
					}
				}
				dists[q][j] = mnDist;
			}
		}
		return dists[n & VALUE_INDEX_MASK][m];
	}
}
