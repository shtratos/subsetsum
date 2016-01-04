package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Random;

import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.minkowskiSum;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class FastMinkowskiSubsetSummerTest {

    @Test
    public void minkowski_sum() throws Exception {
        assertEquals(ImmutableSet.of(3l),
                minkowskiSum(ImmutableSet.of(1l), ImmutableSet.of(2l)));
        assertEquals(ImmutableSet.of(2l),
                minkowskiSum(ImmutableSet.of(1l), ImmutableSet.of(1l, 1l)));
        assertEquals(ImmutableSet.of(4l, 5l, 6l, 7l, 8l),
                minkowskiSum(ImmutableSet.of(1l, 2l, 4l), ImmutableSet.of(3l, 4l)));

        verifyMinkowskiSum(ImmutableSet.of(1l, 100l, 1000l, 10000l), ImmutableSet.of(1l, 10000l));
    }

    @Test
    public void minkowski_sum_randomized_test() throws Exception {
        for (int i = 0; i < 100; i++) {
            verifyMinkowskiSum(randomSet(1000, 100), randomSet(10, 10));
        }
    }

    private void verifyMinkowskiSum(ImmutableSet<Long> A, ImmutableSet<Long> B) {
        assertEquals(naiveMinkowskiSum(A, B), minkowskiSum(A, B));
    }

    private static ImmutableSet<Long> naiveMinkowskiSum(ImmutableSet<Long> A, ImmutableSet<Long> B) {
        ImmutableSet.Builder<Long> c = ImmutableSet.<Long>builder();
        for (Long a : A) {
            for (Long b : B) {
                c.add(a + b);
            }
        }
        return c.build();
    }

    private static ImmutableSet<Long> randomSet(int valueLimit, int sizeLimit) {
        final Random random = new Random();
        final int size = random.nextInt(sizeLimit) + 1;
        ImmutableSet.Builder<Long> r = ImmutableSet.<Long>builder();
        for (int i = 0; i < size; i++) {
            final long v = random.nextInt(valueLimit - 1) + 1;
            checkState(v > 0 && v < valueLimit, "bad value: %s", v);
            r.add(v);
        }
        return r.build();
    }
}