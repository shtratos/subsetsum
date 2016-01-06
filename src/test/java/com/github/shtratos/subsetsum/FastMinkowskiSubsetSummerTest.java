package com.github.shtratos.subsetsum;

import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.combine;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.inverseH;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.mergeSubsetSums;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.minkowskiSum;
import static com.github.shtratos.subsetsum.FastMinkowskiSubsetSummer.perfectH;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

public class FastMinkowskiSubsetSummerTest {

    @Test
    public void subset_are_summed_correctly() throws Exception {
        final SubsetSummer summer = new FastMinkowskiSubsetSummer();
        final ImmutableSet<Long> S = ImmutableSet.of(1l, 2l, 3l, 4l, 5l);
        final long u = 100l;

        final ImmutableSet<Long> subsetSums = summer.subsetSums(S, u);
        assertEquals(naiveSubsetSums(S, u).sums, subsetSums);
    }

    @Test
    public void subset_sums_randomized() throws Exception {
        final SubsetSummer summer = new FastMinkowskiSubsetSummer();
        final Random r = new Random();
        for (int n = 1; n < 100; n++) {
            final long u = 10000l ;
            for (int i = 0; i < 1; i++) {
                final ImmutableSet<Long> S = randomSetOfFixedSize(n * 100, n * 100);
                final Stopwatch timer = Stopwatch.createStarted();
                final ImmutableSet<Long> subsetSums = summer.subsetSums(S, u);
//                System.out.printf("n = %d,\ttime,ms = %.5f%n", S.size(), timer.elapsed(TimeUnit.NANOSECONDS) / 1000000.0);
                System.out.printf("%d\t%.5f%n", S.size(), timer.elapsed(TimeUnit.NANOSECONDS) / 1000000.0);
//            assertEquals(naiveSubsetSums(S, u).sums, subsetSums);
            }
        }
    }
    /* ----------------------------------------------------------------------------------*/

    @Test
    public void can_combine_multiple_subset_sums() throws Exception {
        final ImmutableSet<Long> S = ImmutableSet.of(1l, 2l, 3l, 4l, 5l);
        final ImmutableList<SubsetSums> singleSums = FluentIterable.from(S)
                .transform(x -> SubsetSums.ofSingleElement(x))
                .toList();
        final long u = 100l;

        final SubsetSums subsetSums = combine(singleSums, u);
        assertEquals(naiveSubsetSums(S, u).sums, subsetSums.sums);
    }

    @Test
    public void can_combine_one_subset_sum() throws Exception {
        final SubsetSums singleSum = combine(ImmutableList.of(SubsetSums.ofSingleElement(42l)), 100l);
        assertEquals(ImmutableSet.of(42l), singleSum.sums);

        final SubsetSums noSums = combine(ImmutableList.of(SubsetSums.ofSingleElement(42l)), 20l);
        assertEquals(ImmutableSet.of(42l), noSums.sums);
    }


/* ----------------------------------------------------------------------------------*/

    @Test
    public void can_merge_subset_sums_via_fast_algorithm() throws Exception {
        final ImmutableSet<Long> A = ImmutableSet.of(51l, 52l, 53l);
        final ImmutableSet<Long> B = ImmutableSet.of(50l, 54l, 55l);
        final ImmutableSet<Long> AB = ImmutableSet.copyOf(concat(A, B));

        final long u = 100l;
        final SubsetSums ssA = naiveSubsetSums(A, u);
        final SubsetSums ssB = naiveSubsetSums(B, u);

        final SubsetSums subsetSums = mergeSubsetSums(ssA, ssB, u);
        assertEquals(naiveSubsetSums(AB, u).sums, subsetSums.sums);
    }

    @Test
    public void can_merge_subset_sums_via_standard_algorithm() {
        final ImmutableSet<Long> A = ImmutableSet.of(5l, 6l, 7l);
        final ImmutableSet<Long> B = ImmutableSet.of(8l, 9l, 10l);
        final ImmutableSet<Long> AB = ImmutableSet.copyOf(concat(A, B));

        final long u = 20l;
        final SubsetSums ssA = naiveSubsetSums(A, u);
        final SubsetSums ssB = naiveSubsetSums(B, u);

        final SubsetSums subsetSums = mergeSubsetSums(ssA, ssB, u);
        assertEquals(naiveSubsetSums(AB, u).sums, subsetSums.sums);
    }

    static SubsetSums naiveSubsetSums(ImmutableSet<Long> S, long u) {
        return new SubsetSums(naiveSubsetSumsSet(S, u), Range.closed(baseOf(S), maxOf(S)), S.size());
    }

    static ImmutableSet<Long> naiveSubsetSumsSet(ImmutableSet<Long> S, long u) {
        return FluentIterable.from(Sets.powerSet(S))
                .filter(subset -> !subset.isEmpty())
                .transform(subset -> subset.stream().reduce(0l, (x, y) -> x + y))
                .filter(sum -> sum < u)
                .toSortedSet(Ordering.natural());
    }

/* ----------------------------------------------------------------------------------*/

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
        return randomSetOfFixedSize(valueLimit, size);
    }

    private static ImmutableSet<Long> randomSetOfFixedSize(int valueLimit, int size) {
        final Random random = new Random();
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
            checkState(d >= 2 * l, "expected: %s >= 2 * %s", d, l);
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