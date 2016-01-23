package com.github.shtratos.subsetsum;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Set;

public class DynamicProgrammingSubsetSummer implements SubsetSummer {

    @Override
    public ImmutableSet<Long> subsetSums(Set<Long> inputS, long u) {
        validateInput(inputS, u);

        final ImmutableList<Long> S = Ordering.natural().immutableSortedCopy(inputS);
        if (inputS.isEmpty()) return ImmutableSet.of();

        final int n = S.size();

        // state is a table with rows i=[0..u), columns j=[0..n)
        // each cell meaning is "does any subset of {S[0]..S[j]} sum exactly to `i`?"
        assert u < Integer.MAX_VALUE;
        final boolean[][] state = new boolean[(int) u][n];


        // first column has true value only for the first element
        state[S.get(0).intValue()][0] = true;

        // first row is entirely true - empty subset sums to 0
        Arrays.fill(state[0], true);

        for (int i = 1; i < u; i++) {
            for (int j = 1; j < n; j++) {
                // either there's already subset that sums to `i`
                final boolean previousCell = state[i][j - 1];
                if (previousCell) {
                    Arrays.fill(state[i], j, n, true); // shortcut to fill the rest of the row
                    break;
                } else {
                    // or there's a subset that sums to `i - S[j]`
                    final int reducedSum = i - S.get(j).intValue();
                    state[i][j] = reducedSum >= 0 && state[reducedSum][j - 1];
                }
            }
        }
//        printState(state);

        final ImmutableSet.Builder<Long> sumsBuilder = ImmutableSet.builder();
        for (int i = 1; i < u; i++) {
            if (state[i][n - 1]) {
                sumsBuilder.add((long) i);
            }
        }
        ImmutableSet<Long> sums = sumsBuilder.build();

        validateOutput(sums, u);
        return sums;
    }

    private void printState(boolean[][] state) {
        System.out.println("Dumping state:");
        for (int i = 0; i < state.length; i++) {
            final StringBuilder sb = new StringBuilder();
            for (boolean flag : state[i]) {
                sb.append((flag ? "X" : "_"));
            }
            System.out.println(i + ": " + sb);
        }
        System.out.println("END OF DUMP ----------------------------");
    }
}
