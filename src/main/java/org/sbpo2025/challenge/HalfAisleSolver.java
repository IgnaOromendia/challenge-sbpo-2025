package org.sbpo2025.challenge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

public class HalfAisleSolver {

    private final Map<Integer,Integer> newToOldIndex;
    private final ParametricSolver parametricSolver;
    
    public HalfAisleSolver (List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        newToOldIndex = new HashMap<>();

        double reductionSize = 0.75;

        List<Integer> aisleUniqeItemsSorted = getMapIndicesSortedByMapSumOfValues(aisles);

        List<Map<Integer, Integer>> filteredAisles = new ArrayList<Map<Integer,Integer>>();

        for(int i = 0; i < aisleUniqeItemsSorted.size() * reductionSize; i++) {
            int aisle = aisleUniqeItemsSorted.get(i);
            newToOldIndex.put(filteredAisles.size(), aisle);
            filteredAisles.add(aisles.get(aisle));
        }

        parametricSolver = new ParametricSolver(orders, filteredAisles, nItems, waveSizeLB, waveSizeUB);

    }

    private List<Integer> getMapIndicesSortedByMapSumOfValues(List<Map<Integer, Integer>> mapWithvalues) {
        List<Map.Entry<Integer, Integer>> indicesAndSum = new ArrayList<>();

        for (int i = 0; i < mapWithvalues.size(); i++) {
            int sum = mapWithvalues.get(i).keySet().size();
            indicesAndSum.add(new AbstractMap.SimpleEntry<>(i, sum));
        }

        indicesAndSum.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return indicesAndSum.stream()
                            .map(a -> a.getKey())
                            .collect(Collectors.toList());
    }

    public int solveHalfAisleMILFP(List<Integer> used_orders, List<Integer> used_aisles, double gapTolerance, StopWatch stopWatch) {
        int iterations = parametricSolver.solveMILFP(used_orders, used_aisles, gapTolerance, stopWatch);

        for(int a = 0; a < used_aisles.size(); a++) {
            int filterdAisle = used_aisles.get(a);
            used_aisles.set(a, newToOldIndex.get(filterdAisle));
        }

        return iterations;
    }
    
}
