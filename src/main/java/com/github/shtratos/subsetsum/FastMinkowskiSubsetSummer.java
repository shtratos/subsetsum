package com.github.shtratos.subsetsum;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.math.DoubleMath;
import com.google.common.math.LongMath;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.shtratos.subsetsum.MinkowskiSumUtils.minkowskiSum;
import static com.google.common.base.Preconditions.checkState;

/**
 * Subset Sums problem solution using algorithm described <a href="http://arxiv.org/pdf/1507.02318v1.pdf">here</a>.
 * <p>
 * Running time is expected to be {@code O(sqrt(n) * u * (log^C(sqrt(n) * u)))}.
 * @see <a href="https://docs.google.com/spreadsheets/d/1lamswVUOoZZo4v-frodPNhA3Y5I8Ke5IyOy-EkytNLw/edit?usp=sharing">experiments</a>
 */
public class FastMinkowskiSubsetSummer implements SubsetSummer {

    @Override
    public ImmutableSet<Long> subsetSums(Set<Long> inputS, final long u) {
        validateInput(inputS, u);
        final ImmutableSortedSet<Long> S = ImmutableSortedSet.copyOf(inputS);
        if (inputS.isEmpty()) return ImmutableSet.of();

        final long n = S.size(); // #1, here and further #i denotes corresponding line in the algorithm pseudo-code in the paper

        // split S in k + 2 intervals to get predictable running times
        final int k = log2(Math.max(log2(n), 1)); // #3
        final List<Long> a = new ArrayList<>(k + 2);
        a.add(0L); // #2

        for (int i = 1; i <= k; i++) { // #4
            double power = ((double) (pow2(k) - pow2(i) + 2)) / pow2(k + 1);
            double interval = Math.pow(n, power);
            long a_i = DoubleMath.roundToLong(u / interval, RoundingMode.CEILING);
            a.add(a_i); // #5
        }
        a.add(u); // #6

        // independently calculate subset sums on each interval of known size
        // (I think this can be done in parallel)
        List<SubsetSums> A = new ArrayList<>(k + 2);
        for (int i = 0; i <= k; i++) { // #7
            final ImmutableSortedSet<Long> subset = S.subSet(a.get(i), a.get(i + 1)); // #8
            final int t = subset.size();
            if (t == 0) continue; // skip the interval if it's empty
            final List<SubsetSums> B = new ArrayList<>(t);
            for (Long s_j : subset) { // #9
                checkState(s_j < u);
                B.add(SubsetSums.ofSingleElement(s_j)); // #10
            }
            A.add(combine(B, u)); // #11
        }

        // merge results from all intervals
        final SubsetSums output = combine(A, u); // #12

        validateOutput(output.sums, u);
        checkState(Range.closed(0L, u - 1).encloses(output.subsetSpan));
        checkState(output.subsetSize == n);
        return output.sums;
    }

    private static long pow2(int k) {
        return LongMath.checkedPow(2, k);
    }

    private static int log2(long n) {
        return LongMath.log2(n, RoundingMode.CEILING);
    }

    /**
     * This subroutine takes a sequence of sets S1, . . . , Sm,
     * finds the Minkowski sum of adjacent sets and recurses.
     * <p>
     * All elements of Si fit in the {@code span} range, which is a subset of {@code [0..u-1]}
     */
    static SubsetSums combine(final List<SubsetSums> sets, final long u) {
        if (sets.size() <= 1) {
            Preconditions.checkArgument(!sets.isEmpty(), "sets must have at least one element!");
            return sets.get(0);
        } else {
            final List<SubsetSums> combinedSets = new ArrayList<>(sets.size() / 2 + 1);
            for (List<SubsetSums> pairOfSets : Lists.partition(sets, 2)) {
                if (pairOfSets.size() == 2) {
                    combinedSets.add(mergeSubsetSums(pairOfSets.get(0), pairOfSets.get(1), u));
                } else {
                    combinedSets.add(pairOfSets.get(0));
                }
            }
            return combine(combinedSets, u);
        }
    }

    /**
     * Applying {@code Theorem 2} to calculate subset sums of concatenation of 2 sets,
     * given subset sums of those 2 sets.
     * <p>
     * <strong>Theorem 2.</strong> Let A and B be two sequences with total length n.
     * All elements in A and B are in a+[l−1].
     * If Σu(A) and Σu(B) are given, then we can compute Σu(AB) = (Σu(A)+Σu(B))∩[u−1] in O~(min{lk2, u}) time,
     * where k = min{n, u/a}.
     *
     * @param ssA Σu(A)
     * @param ssB Σu(B)
     * @param u   target bound
     * @return Σu(AB)
     */
    static SubsetSums mergeSubsetSums(SubsetSums ssA, SubsetSums ssB, final long u) {
        // define the span a+[l−1] where all values of A and B fit
        final Range<Long> span = ssA.subsetSpan.span(ssB.subsetSpan);
        final long n = ssA.subsetSize + ssB.subsetSize;
        final long a = span.lowerEndpoint();
        final long l = span.upperEndpoint() + 1 - a;
        final long k = Math.min(n, LongMath.divide(u, a, RoundingMode.CEILING));

        final ImmutableSet<Long> C;
        if (k * k * l >= u) {
            // apply standard algorithm
            C = minkowskiSum(ssA.sums, ssB.sums);
        } else {
            // apply fast algorithm
            // it basically tries to shrink the range of values in order to speed up Minkowski sum calculation
            final long maxL = k * l;
            final ImmutableSet<Long> hA = perfectH(ssA.sums, a, maxL);
            final ImmutableSet<Long> hB = perfectH(ssB.sums, a, maxL);
            final ImmutableSet<Long> hAB = minkowskiSum(hA, hB);
            C = inverseH(hAB, a, maxL);
        }
        final ImmutableSet<Long> sums = FluentIterable.from(Iterables.concat(ssA.sums, ssB.sums, C))
                .filter(e -> e < u) // limit sums by target value
                .toSet();
        return new SubsetSums(sums, span, n);
    }

    /**
     * Calculate S-perfect function <em>h(x)</em> on bounded set S by applying <strong>Lemma 1</strong>.
     *
     * @param S set to calculate h on
     * @param d lower bound of S
     * @param l length of span covering S
     * @return h(S) = { h(x) | x in S}
     */
    static ImmutableSet<Long> perfectH(ImmutableSet<Long> S, long d, long l) {
        if (d >= 2 * l) {
            return FluentIterable.from(S)
                    .transform(x -> {
                        long q = x / d;
                        long r = x % d;
                        return 2 * l * q + r;
                    })
                    .toSet();
        } else {
            return S;
        }
    }

    /**
     * Calculate inverse of {@link #perfectH(ImmutableSet, long, long)}.
     *
     * @param hS h(S)
     * @param d  lower bound of S
     * @param l  length of span covering S
     * @return S = h<sup>-1</sup>(h(S))
     */
    static ImmutableSet<Long> inverseH(ImmutableSet<Long> hS, long d, long l) {
        if (d >= 2 * l) {
            return FluentIterable.from(hS)
                    .transform(x -> {
                        long q = x / (2 * l);
                        long r = x % (2 * l);
                        return d * q + r;
                    })
                    .toSet();
        } else {
            return hS;
        }
    }


}
