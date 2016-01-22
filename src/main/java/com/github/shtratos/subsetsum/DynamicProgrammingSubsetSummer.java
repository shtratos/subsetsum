package com.github.shtratos.subsetsum;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class DynamicProgrammingSubsetSummer implements SubsetSummer {

    @Override
    public ImmutableSet<Long> subsetSums(Set<Long> inputS, long u) {
        validateInput(inputS, u);

        final ImmutableList<Long> S = Ordering.natural().immutableSortedCopy(inputS);
        if (inputS.isEmpty()) return ImmutableSet.of();

        final int n = S.size();

        // state is a table with rows i=[0..u), columns j=[0..n)
        // each cell meaning is "does any subset of {S[0]..S[j]} sum exactly to `i`?"
        final ContiguousSet<Long> rowKeys = ContiguousSet.create(Range.closedOpen(0L, u), DiscreteDomain.longs());
        final ContiguousSet<Integer> columnKeys = ContiguousSet.create(Range.closedOpen(0, n), DiscreteDomain.integers());
        final Table<Long, Integer, Boolean> state = ArrayTable.create(rowKeys, columnKeys);

        // first column has true value only for the first element
        state.column(0).replaceAll((k, v) -> k.equals(S.get(0)));

        // first row is entirely true - empty subset sums to 0
        state.row(0L).replaceAll((k, v) -> true);

        for (long i = 1; i < u; i++) {
            for (int j = 1; j < n; j++) {
                boolean v = state.get(i, j - 1)            // either there's already subset that sums to `i`
                        || state.get(i - S.get(j), j - 1) == Boolean.TRUE; // or there's a subset that sums to `i - S[j]`
                state.put(i, j, v);
            }
        }
//        printState(state);

        state.put(0L, n - 1, false); // exclude 0 from resulting sums
        ImmutableSet<Long> sums = ImmutableSet.copyOf(state.column(n - 1).entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(toList()));

        validateOutput(sums, u);
        return sums;
    }

    private void printState(Table<?, ?, Boolean> state) {
        System.out.println("Dumping state:");
        final Function<Boolean, String> stateMapper = flag -> flag == null ? "." : (flag ? "X" : "_");
        state.rowMap().forEach((i, row) -> {
            System.out.println(i + ": " + row.values().stream().map(stateMapper).collect(joining()));
        });
        System.out.println("END OF DUMP ----------------------------");
    }
}
