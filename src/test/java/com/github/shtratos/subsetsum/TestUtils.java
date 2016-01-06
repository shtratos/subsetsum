package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;

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
}
