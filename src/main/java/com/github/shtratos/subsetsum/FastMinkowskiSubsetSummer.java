package com.github.shtratos.subsetsum;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.math.DoubleMath;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import org.jtransforms.fft.FloatFFT_1D;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementing algorithm described <a href="http://arxiv.org/pdf/1507.02318v1.pdf">here</a>.
 */
public class FastMinkowskiSubsetSummer implements SubsetSummer {
    @Override
    public ImmutableSet<Long> subsetSums(Set<Long> inputS, final long u) {
        validateInput(inputS, u);
        final ImmutableSortedSet<Long> S = ImmutableSortedSet.copyOf(inputS);

        final long n = S.size(); // #1

        final int k = log2(log2(n)); // #3
        final List<Long> a = new ArrayList<>(k + 2);
        a.add(0L); // #2

        for (int i = 1; i <= k; i++) { // #4
            double power = ((double) (pow2(k) - pow2(i) + 2)) / pow2(k + 1);
            double interval = Math.pow(n, power);
            long a_i = DoubleMath.roundToLong(u / interval, RoundingMode.CEILING);
            a.add(a_i); // #5
        }
        a.add(u); // #6

        List<SubsetSums> A = new ArrayList<>(k + 2);
        for (int i = 0; i <= k; i++) { // #7
            final ImmutableSortedSet<Long> subset = S.subSet(a.get(i), a.get(i + 1)); // #8
            final int t = subset.size();
            final List<SubsetSums> B = new ArrayList<>(t);
            for (Long s_j : subset) { // #9
                assert s_j < u;
                B.add(new SubsetSums(ImmutableSet.of(s_j), Range.closed(s_j, s_j), 1)); // #10
            }
            A.add(combine(B, u)); // #11
        }

        final SubsetSums output = combine(A, u); // #12
        validateOutput(output.sums, u);
        assert Range.closed(0l, u - 1).encloses(output.subsetSpan);
        assert output.subsetSize == n;
        return output.sums;
    }

    static long pow2(int k) {
        return LongMath.checkedPow(2, k);
    }

    static int log2(long n) {
        return LongMath.log2(n, RoundingMode.CEILING);
    }

    /**
     * This subroutine takes a sequence of sets S1, . . . , Sm,
     * finds the Minkowski sum of adjacent sets and recurses.
     * <p>
     * All elements of Si fit in the {@code span} range, which is a subset of {@code [0..u-1]}
     */
    SubsetSums combine(final List<SubsetSums> sets, final long u) {
        if (sets.size() == 1) {
            return sets.iterator().next();
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
    SubsetSums mergeSubsetSums(SubsetSums ssA, SubsetSums ssB, final long u) {
        // define the span a+[l−1] where all values of A and B fit
        final Range<Long> span = ssA.subsetSpan.span(ssB.subsetSpan);
        final long n = ssA.subsetSize + ssB.subsetSize;
        final long a = span.lowerEndpoint();
        final long l = span.upperEndpoint() + 1 - a;
        final long k = Math.min(n, LongMath.divide(u, a, RoundingMode.CEILING));

        if (k * k * l >= u) {
            // apply standard algorithm
            return new SubsetSums(minkowskiSum(ssA.sums, ssB.sums), span, n);
        } else {
            // apply fast algorithm
            final long maxL = k * l;
            final ImmutableSet<Long> hA = perfectH(ssA.sums, a, maxL);
            final ImmutableSet<Long> hB = perfectH(ssB.sums, a, maxL);
            final ImmutableSet<Long> hAB = minkowskiSum(hA, hB);
            ImmutableSet<Long> C = inverseH(hAB, a, maxL);
            return new SubsetSums(ImmutableSet.copyOf(Iterables.concat(ssA.sums, ssB.sums, C)), span, n);
        }
    }

    /**
     * Calculate Minkowski sum of 2 bounded sets via convolution.
     *
     * @param A first set
     * @param B second set
     * @return A + B = { a + b | a in A, b in B }
     * @see <a href="http://stackoverflow.com/a/11478023">Efficient Minkowski sum calculation</a>
     */
    // TODO extract this into separate class for ease of testing and benchmarking
    static ImmutableSet<Long> minkowskiSum(ImmutableSet<Long> A, ImmutableSet<Long> B) {
        final long limit = 2 + 2 * Ordering.natural().max(Iterables.concat(A, B));
        assert limit < (1 << 29); // ensure we do not hit large array limit
        float[] cA = characteristic(A, limit);
        float[] cB = characteristic(B, limit);
        assert cA.length == cB.length; // ensure both characteristic vectors are of the same size
        assert cA.length == 2 * limit; // ensure characteristic vectors are twice the limit in size, that's required by FFT

        final float[] cC = convolution(cA, cB, limit);
        return inverseCharacteristic(cC, limit);
    }

    static float[] convolution(float[] cA, float[] cB, long limit) {
        final FloatFFT_1D fft = new FloatFFT_1D(limit);
        fft.realForwardFull(cA);
        fft.realForwardFull(cB);
        float[] cC = multiplyComplexVectors(cA, cB, limit);

        fft.complexInverse(cC, true);
        return cC;
    }

    private static float[] multiplyComplexVectors(float[] cA, float[] cB, long limit) {
        assert limit * 2 == cA.length;
        for (int i = 0; i < limit; i++) {
            float realA = cA[i * 2];
            float imA = cA[i * 2 + 1];
            float realB = cB[i * 2];
            float imB = cB[i * 2 + 1];

            cA[i * 2] = realA * realB - imA * imB;
            cA[i * 2 + 1] = realA * imB + imA * realB;
        }
        return cA;
    }

    static private float[] characteristic(ImmutableSet<Long> set, long limit) {
        final float[] c = new float[Ints.checkedCast(limit * 2)];
        for (Long e : set) {
            assert e >= 0 && e < limit;
            c[Ints.checkedCast(e)] = 1;
        }
        return c;
    }

    static private ImmutableSet<Long> inverseCharacteristic(float[] c, long limit) {
        final ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
        assert limit < c.length;
        final float eps = 0.5f;
//        printVectorStats(c, limit, eps);
        for (int i = 0; i < limit; i++) {
            final float v = Math.abs(c[i * 2]);
            if (v > eps) {
                builder.add((long) i);
            }
        }
        return builder.build();
    }

    static private void printVectorStats(float[] c, long limit, float eps) {
        float min = 0f, max = 0f, absMin = Float.MAX_VALUE, absMax = 0f, sum = 0f;
        for (int i = 0; i < limit; i++) {
            final float v = Math.abs(c[i * 2]);
            min = Math.min(min, c[i * 2]);
            max = Math.max(max, c[i * 2]);
            sum += v;
            if (v > eps) {
                absMin = Math.min(absMin, v);
                absMax = Math.max(absMax, v);
            }
        }
        System.out.printf("min = %f, max = %f, absMin = %f, absMax = %f, avg = %f\n", min, max, absMin, absMax, (sum / limit));
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


    /**
     * Subset sums for some set.
     * Contains some metadata about the set.
     */
    static class SubsetSums {
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
    }
}
