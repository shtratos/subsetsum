package com.github.shtratos.subsetsum;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import java.util.Random;

import static com.google.common.base.Preconditions.checkState;

final class TestUtils {
    private TestUtils() { }


    static ImmutableSet<Long> randomSet(int valueLimit, int sizeLimit) {
        final Random random = new Random();
        final int size = random.nextInt(sizeLimit) + 1;
        return randomSetOfFixedSize(valueLimit, size);
    }

    static ImmutableSet<Long> randomSetOfFixedSize(int valueLimit, int size) {
        final Random random = new Random();
        ImmutableSet.Builder<Long> r = ImmutableSet.<Long>builder();
        for (int i = 0; i < size; i++) {
            final long v = random.nextInt(valueLimit - 1) + 1;
            checkState(v > 0 && v < valueLimit, "bad value: %s", v);
            r.add(v);
        }
        return r.build();
    }

    static SubsetSums naiveSubsetSums(ImmutableSet<Long> S, long u) {
        return new SubsetSums(naiveSubsetSumsSet(S, u), Range.closed(baseOf(S), maxOf(S)), S.size());
    }

    static ImmutableSet<Long> naiveSubsetSumsSet(ImmutableSet<Long> S, long u) {
        return FluentIterable.from(Sets.powerSet(S))
                .filter(subset -> !subset.isEmpty())
                .transform(subset -> subset.stream().reduce(0L, (x, y) -> x + y))
                .filter(sum -> sum < u)
                .toSortedSet(Ordering.natural());
    }

    static Long baseOf(ImmutableSet<Long> s) {
        return Ordering.natural().min(s);
    }

    static Long lengthOf(ImmutableSet<Long> s) {
        return maxOf(s) - baseOf(s) + 1;
    }

    static Long maxOf(ImmutableSet<Long> s) {
        return Ordering.natural().max(s);
    }
}
