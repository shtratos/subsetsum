package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static com.github.shtratos.subsetsum.MinkowskiSumUtils.minkowskiSum;
import static org.junit.Assert.assertEquals;

public class MinkowskiSumUtilsTest {

    @Test
    public void minkowski_sum() throws Exception {
        assertEquals(ImmutableSet.of(3L),
                minkowskiSum(ImmutableSet.of(1L), ImmutableSet.of(2L)));
        assertEquals(ImmutableSet.of(2L),
                minkowskiSum(ImmutableSet.of(1L), ImmutableSet.of(1L, 1L)));
        assertEquals(ImmutableSet.of(4L, 5L, 6L, 7L, 8L),
                minkowskiSum(ImmutableSet.of(1L, 2L, 4L), ImmutableSet.of(3L, 4L)));

        verifyMinkowskiSum(ImmutableSet.of(1L, 100L, 1000L, 10000L), ImmutableSet.of(1L, 10000L));
    }

    @Test
    public void minkowski_sum_randomized_test() throws Exception {
        for (int i = 0; i < 100; i++) {
            verifyMinkowskiSum(TestUtils.randomSet(1000, 100), TestUtils.randomSet(10, 10));
        }
    }

    private void verifyMinkowskiSum(ImmutableSet<Long> A, ImmutableSet<Long> B) {
        assertEquals(naiveMinkowskiSum(A, B), minkowskiSum(A, B));
    }

    // TODO benchmark against FFT-based algorithm
    private static ImmutableSet<Long> naiveMinkowskiSum(ImmutableSet<Long> A, ImmutableSet<Long> B) {
        ImmutableSet.Builder<Long> c = ImmutableSet.<Long>builder();
        for (Long a : A) {
            for (Long b : B) {
                c.add(a + b);
            }
        }
        return c.build();
    }

}