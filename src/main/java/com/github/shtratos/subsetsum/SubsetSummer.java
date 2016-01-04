package com.github.shtratos.subsetsum;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.Set;

/**
 * Problem statement:
 * given an integer {@code u} and a set of {@code n} integers {@code S} in the range {@code 0..u-1},
 * calculate all subset sums up to {@code u}.
 */
public interface SubsetSummer {

    ImmutableSet<Long> subsetSums(Set<Long> s, long u);

    default void validateInput(Set<Long> s, long u) {
        Preconditions.checkArgument(u > 0, "u must be natural, was: %s", u);
        Preconditions.checkArgument(Iterables.all(s, e -> e > 0 && e < u),
                "all elements in S must in range: [1..%s]", u-1);
    }

    default void validateOutput(Set<Long> output, long u) {
        Preconditions.checkArgument(Iterables.all(output, e -> e > 0 && e < u),
                "all elements in output must in range: [1..%s", u-1);
    }
}
