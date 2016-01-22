package com.github.shtratos.subsetsum;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.shtratos.subsetsum.TestUtils.naiveSubsetSums;
import static com.github.shtratos.subsetsum.TestUtils.randomSetOfFixedSize;
import static org.junit.Assert.assertEquals;

public class DynamicProgrammingSubsetSummerTest {

    @Test
    public void subsets_are_summed_correctly() throws Exception {
        final SubsetSummer summer = new DynamicProgrammingSubsetSummer();
        final ImmutableSet<Long> S = ImmutableSet.of(1L, 2L, 3L, 4L, 5L);
        final long u = 100L;

        final ImmutableSet<Long> subsetSums = summer.subsetSums(S, u);
        assertEquals(naiveSubsetSums(S, u).sums, subsetSums);
    }

    @Test
    public void trivial_cases() throws Exception {
        final SubsetSummer summer = new DynamicProgrammingSubsetSummer();

        assertEquals(ImmutableSet.of(), summer.subsetSums(ImmutableSet.of(), 42L));
        assertEquals(ImmutableSet.of(7L), summer.subsetSums(ImmutableSet.of(7L), 42L));
        assertEquals(ImmutableSet.of(2L, 3L, 5L), summer.subsetSums(ImmutableSet.of(2L, 3L), 42L));
        assertEquals(ImmutableSet.of(2L, 3L), summer.subsetSums(ImmutableSet.of(2L, 3L), 5L));
    }

    @Test
    @Ignore("this test is for running experiments only")
    public void subset_sums_experiments() throws Exception {
        final SubsetSummer summer1 = new DynamicProgrammingSubsetSummer();
        final SubsetSummer summer2 = new FastMinkowskiSubsetSummer();
        boolean result = false;
        for (int n = 1; n < 200; n++) {
            final long u = 30000L;
            for (int i = 0; i < 1; i++) {
                final ImmutableSet<Long> S = randomSetOfFixedSize(n * 100, n * 100);

                final Stopwatch timer1 = Stopwatch.createStarted();
                final ImmutableSet<Long> subsetSums1 = summer1.subsetSums(S, u);
                final double summer1Time = timer1.elapsed(TimeUnit.NANOSECONDS) / 1000000.0;

                final Stopwatch timer2 = Stopwatch.createStarted();
                final ImmutableSet<Long> subsetSums2 = summer2.subsetSums(S, u);
                final double summer2Time = timer2.elapsed(TimeUnit.NANOSECONDS) / 1000000.0;

                System.out.printf("%d\t%.5f\t%.5f%n", S.size(), summer1Time, summer2Time);
                result = result || subsetSums2.equals(subsetSums1);
            }
        }
        if (result) {
            System.out.println("use result somehow to avoid runtime optimization");
        }
    }

}