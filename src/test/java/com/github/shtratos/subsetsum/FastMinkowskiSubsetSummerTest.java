package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import org.junit.Test;

import java.util.Random;

import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.inverseH;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.minkowskiSum;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.perfectH;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
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

    /* ----------------------------------------------------------------------------------*/

    @Test
    public void perfect_h_is_injection() throws Exception {
        final long d = 100l;
        final long l = 10l;
        final ImmutableSet<Long> S = ImmutableSet.of(100l, 102l, 104l, 107l);
        final ImmutableSet<Long> hS = perfectH(S, d, l);
        final ImmutableSet<Long> inverse = inverseH(hS, d, l);

        assertEquals(S, inverse);
    }

    @Test
    public void perfect_h_is_injection_trivial_case() throws Exception {
        final long d = 1l;
        final long l = 3l;
        final ImmutableSet<Long> S = ImmutableSet.of(1l, 2l, 3l);
        final ImmutableSet<Long> hS = perfectH(S, d, l);
        final ImmutableSet<Long> inverse = inverseH(hS, d, l);

        assertEquals(S, inverse);
    }

    @Test
    public void perfect_h_is_injection_randomized() throws Exception {
        for (int i = 0; i < 100; i++) {
            final ImmutableSet<Long> rand = randomSet(1000, 1000);
            final long extraD = Math.max(lengthOf(rand) * 2 + new Random().nextInt(10000), 0l);
            final ImmutableSet<Long> S = rand.stream()
                    .map(e -> e + extraD)
                    .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));

            final long d = baseOf(S);
            final long l = lengthOf(S);
            checkState(d >= 2*l, "expected: %s >= 2 * %s", d, l);
            final ImmutableSet<Long> hS = perfectH(S, d, l);
            final ImmutableSet<Long> inverse = inverseH(hS, d, l);
            assertEquals(S, inverse);

            System.out.printf("S (d, l, max) = %d, %d, %d \t\t; h(S) (d, l, max) = %d, %d, %d (cut by %d%%)\n",
                    d, l, maxOf(S), baseOf(hS), lengthOf(hS), maxOf(hS), 100 - (maxOf(hS) * 100 / maxOf(S)));
        }
    }

    private static Long baseOf(ImmutableSet<Long> s) {
        return Ordering.natural().min(s);
    }

    private static Long lengthOf(ImmutableSet<Long> s) {
        return maxOf(s) - baseOf(s) + 1;
    }

    private static Long maxOf(ImmutableSet<Long> s) {
        return Ordering.natural().max(s);
    }
}