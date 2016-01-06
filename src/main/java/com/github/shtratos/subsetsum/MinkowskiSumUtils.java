package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.jtransforms.fft.FloatFFT_1D;

final class MinkowskiSumUtils {
    private MinkowskiSumUtils() { }

    private static final boolean DEBUG_MODE = System.getProperty("debug.mode") != null;

    /**
     * Calculate Minkowski sum of 2 bounded sets via convolution.
     *
     * @param A first set
     * @param B second set
     * @return A + B = { a + b | a in A, b in B }
     * @see <a href="http://stackoverflow.com/a/11478023">Efficient Minkowski sum calculation</a>
     */
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
        if (DEBUG_MODE) {
            printVectorStats(c, limit, eps);
        }
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
}
