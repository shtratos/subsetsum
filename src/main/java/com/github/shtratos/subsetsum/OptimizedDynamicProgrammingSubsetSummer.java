package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Set;

public class OptimizedDynamicProgrammingSubsetSummer implements SubsetSummer {

    @Override
    public ImmutableSet<Long> subsetSums(Set<Long> inputS, long u) {
        assert u < Integer.MAX_VALUE;
        validateInput(inputS, u);
        if (inputS.isEmpty()) return ImmutableSet.of();

        final int[] S = inputS.stream().mapToInt(Long::intValue).toArray();
        Arrays.sort(S);

        final int n = S.length;

        // state is a table with rows i=[0..u), columns j=[0..n)
        // each cell is boolean meaning is "does any subset of {S[0]..S[j]} sum exactly to `i`?"

        // Here we replace boolean matrix with array of integers:
        // `i`th element indicates index of first cell that is true in the matrix above.
        // It's just a compacted representation of boolean matrix.
        // We can do this, because if cell[i,j] is true,
        // then all other cells to the right in `i`th row are true
        final int[] newState = new int[(int) u];
        Arrays.fill(newState, Integer.MAX_VALUE); // MAX_VALUE means no subset of S sums to `i`

        // first column has true value only for the first element
        newState[S[0]] = 0;

        // first row is entirely true - empty subset sums to 0
        newState[0] = 0;

        for (int i = 1; i < u; i++) {
            for (int j = 1; j < n; j++) {
                final int reducedSum = i - S[j];
                if (reducedSum >= 0 && (j - 1) >= newState[reducedSum]) {
                    // there's a subset that sums to `i - S[j]`
                    newState[i] = j;
                    break;
                }
            }
        }

        final ImmutableSet.Builder<Long> sumsBuilder = ImmutableSet.builder();
        for (int i = 1; i < u; i++) {
            if (newState[i] < Integer.MAX_VALUE) {
                sumsBuilder.add((long) i);
            }
        }
        ImmutableSet<Long> sums = sumsBuilder.build();

        validateOutput(sums, u);
        return sums;
    }

}
