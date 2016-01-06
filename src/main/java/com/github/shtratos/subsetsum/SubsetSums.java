package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

/**
 * Subset sums for some set.
 * Contains some metadata about the set.
 */
class SubsetSums {
    /**
     * A set of sums of all subsets of the current set.
     */
    final ImmutableSet<Long> sums;
    /**
     * Range of values in the current set.
     */
    final Range<Long> subsetSpan;
    /**
     * Size of the current set.
     */
    final long subsetSize;

    SubsetSums(ImmutableSet<Long> sums, Range<Long> subsetSpan, long subsetSize) {
        this.sums = sums;
        this.subsetSpan = subsetSpan;
        this.subsetSize = subsetSize;
    }

    static SubsetSums ofSingleElement(long x) {
        return new SubsetSums(ImmutableSet.of(x), Range.closed(x, x), 1);
    }
}
