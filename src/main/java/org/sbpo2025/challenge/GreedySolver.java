package org.sbpo2025.challenge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GreedySolver {

    private final List<Map<Integer, Integer>> orders;
    private final List<Map<Integer, Integer>> aisles;
    private final int nItems;
    private final int waveSizeLB;
    private final int waveSizeUB;

    public GreedySolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB; 
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave) {
        return solve(ordersToSave, aislesToSave, -1);
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave, double aLowerBound) {
        return solve(ordersToSave, aislesToSave, aLowerBound, true);
    }

    public double solve_with_both_greedies(List<Integer> ordersToSave, List<Integer> aislesToSave, double aLowerBound) {
        LinkedList<Integer> ordersSortedByNumElements = new LinkedList<>(getMapIndicesSortedByMapSumOfValues(this.orders)); // Linked list para remover en O(1) desde adentro
        LinkedList<Integer> ordersSortedByNumElementsClone = (LinkedList<Integer>) ordersSortedByNumElements.clone();

        double first_greedy_value = solve_for_sorted_aisles_and_orders(ordersToSave, aislesToSave, ordersSortedByNumElements, getMapIndicesSortedByMapSumOfValues(this.aisles), aLowerBound);

        double second_greedy_value = solve_for_sorted_aisles_and_orders(ordersToSave, aislesToSave, ordersSortedByNumElementsClone, getMapIndicesSortedByNumDistinct(this.aisles), Math.max(first_greedy_value, aLowerBound));

        return second_greedy_value;
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave, double aLowerBound, boolean sortByNumberOfValues) {
        List<Integer> aislesSortedByNumElements;

        if (sortByNumberOfValues)
            aislesSortedByNumElements = getMapIndicesSortedByMapSumOfValues(this.aisles);
        else
            aislesSortedByNumElements = getMapIndicesSortedByNumDistinct(this.aisles);

        LinkedList<Integer> ordersSortedByNumElements = new LinkedList<>(getMapIndicesSortedByMapSumOfValues(this.orders)); // Linked list para remover en O(1) desde adentro
        
        return solve_for_sorted_aisles_and_orders(ordersToSave, aislesToSave, ordersSortedByNumElements, aislesSortedByNumElements, aLowerBound);
    }

    private double solve_for_sorted_aisles_and_orders(List<Integer> ordersToSave, List<Integer> aislesToSave, LinkedList<Integer> ordersSortedByNumElements, List<Integer> aislesSortedByNumElements, double aLowerBound) {
        int[] elementsPerType = new int[this.nItems]; // Inicializa a 0 automaticamente
        int curElements = 0; double bestRatio = aLowerBound;

        // Guardo todas las ordenes elegidas en orden, y un indice que me dice donde termina la mejor
        // encontrada hasta el momento (que debe ser un prefijo de las totales elegidas)
        List<Integer> ordersChosen = new ArrayList<>();
        int indexOrderSolution = 0, indexAisleSolution = 0;

        for (int curAisleIndex = 0; curAisleIndex < aislesSortedByNumElements.size(); curAisleIndex++) {
            
            // Load elements from aisle
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(aislesSortedByNumElements.get(curAisleIndex)).entrySet())
                elementsPerType[entry.getKey()] += entry.getValue();

            // Iterate reminaing orders
            Iterator<Integer> orderIndexIterator = ordersSortedByNumElements.iterator();
            while (orderIndexIterator.hasNext()) {

                int o = orderIndexIterator.next();
                boolean fail = false;
                
                int elementsInOrder = 0;
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                    elementsInOrder += entry.getValue();
                    if (elementsPerType[entry.getKey()] < entry.getValue()) {
                        fail = true;
                        break;
                    }
                }

                if (fail) continue;
                
                if (elementsInOrder + curElements <= this.waveSizeUB) {

                    for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                        elementsPerType[entry.getKey()] -= entry.getValue();
                    }
                    curElements += elementsInOrder;

                    ordersChosen.add(o);
                }
                
                orderIndexIterator.remove();
                if (this.waveSizeLB <= curElements && bestRatio < (double) curElements / (curAisleIndex + 1)) {
                    bestRatio = (double) curElements / (curAisleIndex + 1);
                    indexOrderSolution = ordersChosen.size();
                    indexAisleSolution = curAisleIndex + 1;
                }
            }
        }

        if (bestRatio > aLowerBound) { // Se encontro alguna solucion
            ordersToSave.clear();
            aislesToSave.clear();

            for (int o=0; o<indexOrderSolution; o++) ordersToSave.add(ordersChosen.get(o));
            for (int a=0; a<indexAisleSolution; a++) aislesToSave.add(aislesSortedByNumElements.get(a));  
        }

        return bestRatio;
    }

    private List<Integer> getMapIndicesSortedByMapSumOfValues(List<Map<Integer, Integer>> mapWithvalues) {
        List<Map.Entry<Integer, Integer>> indicesAndSum = new ArrayList<>();

        for (int i = 0; i < mapWithvalues.size(); i++) {
            int sum = mapWithvalues.get(i).values().stream().mapToInt(Integer::intValue).sum();
            indicesAndSum.add(new AbstractMap.SimpleEntry<>(i, sum));
        }

        indicesAndSum.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return indicesAndSum.stream()
                            .map(a -> a.getKey())
                            .collect(Collectors.toList());
    }

    private List<Integer> getMapIndicesSortedByNumDistinct(List<Map<Integer, Integer>> mapWithValues) {
        List<Map.Entry<Integer, Integer>> indicesAndSum = new ArrayList<>();

        for (int i = 0; i < mapWithValues.size(); i++) {
            int sum = mapWithValues.get(i).keySet().size();
            indicesAndSum.add(new AbstractMap.SimpleEntry<>(i, sum));
        }

        indicesAndSum.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return indicesAndSum.stream()
                            .map(a -> a.getKey())
                            .collect(Collectors.toList());
    }
}