package com.github.shtratos.subsetsum;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Subset Sums problem statement.
 */
public interface SubsetSummer {

    /**
     * Given a positive integer {@code u} and a set of {@code n} integers {@code S} in the range {@code [1..u-1]},
     * calculate all subset sums less than {@code u}.
     */
    ImmutableSet<Long> subsetSums(Set<Long> s, long u);

    default void validateInput(Set<Long> s, long u) {
        Preconditions.checkArgument(u > 0, "u must be natural, was: %s", u);
        Preconditions.checkArgument(s.stream().allMatch(e -> e > 0 && e < u),
                "all elements in S must in range: [1..%s]", u - 1);
    }

    default void validateOutput(Set<Long> output, long u) {
        Preconditions.checkState(output.stream().allMatch(e -> e > 0 && e < u),
                "all elements in output must in range: [1..%s", u - 1);
    }
}
