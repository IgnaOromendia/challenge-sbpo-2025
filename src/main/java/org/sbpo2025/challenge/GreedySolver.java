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

    private final List<Integer> ordersIndicesSortedBySize;

    public GreedySolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB; 
        this.ordersIndicesSortedBySize = this.getMapIndicesSortedByMapSumOfValues(orders);
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave) {
        return solve(ordersToSave, aislesToSave, -1);
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave, double aLowerBound) {
        return solve(ordersToSave, aislesToSave, aLowerBound, true);
    }

    public double solve_with_both_greedies(List<Integer> ordersToSave, List<Integer> aislesToSave, double aLowerBound) {
        LinkedList<Integer> ordersSortedByNumElements = new LinkedList<>(this.ordersIndicesSortedBySize); // Linked list para remover en O(1) desde adentro
        LinkedList<Integer> ordersSortedByNumElementsClone = (LinkedList<Integer>) ordersSortedByNumElements.clone();

        double first_greedy_value = solve_for_sorted_aisles_and_orders(ordersToSave, aislesToSave, ordersSortedByNumElements, getMapIndicesSortedByMapSumOfValues(this.aisles), aLowerBound);
        System.out.println("First greedy gave: " + first_greedy_value);

        double second_greedy_value = solve_for_sorted_aisles_and_orders(ordersToSave, aislesToSave, ordersSortedByNumElementsClone, getMapIndicesSortedByNumDistinct(this.aisles), Math.max(first_greedy_value, aLowerBound));
        System.out.println("Second greedy gave: " + second_greedy_value);

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

    public double tryAddAisle(List<Integer> used_orders, List<Integer> used_aisles) {
        int oldItemsSum = 0;

        for (int o : used_orders)
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                oldItemsSum += entry.getValue();

        double oldSolutionValue = oldItemsSum / (double) used_aisles.size(); 

        List<Integer> freeOrders = new ArrayList<>();

        // Hacer en n log n
        for (int o=0; o<this.ordersIndicesSortedBySize.size(); o++) {
            int index = this.ordersIndicesSortedBySize.get(o);
            if (!used_orders.contains(index))
                freeOrders.add(index);
        }
        
        int[] capacityPerElem = new int[this.nItems];
        for (int e=0; e<this.nItems; e++) capacityPerElem[e] = 0;

        for (int j=0; j<used_aisles.size(); j++)
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(used_aisles.get(j)).entrySet())
                capacityPerElem[entry.getKey()] += entry.getValue();

        for (int i=0; i<used_orders.size(); i++)
            for (Map.Entry<Integer, Integer> entry : this.orders.get(used_orders.get(i)).entrySet())
                capacityPerElem[entry.getKey()] -= entry.getValue();

        int bestAisle = -1;
        List<Integer> newOrders = new ArrayList<>();

        int[] capacityPerElemCopy = new int[this.nItems];
        for (int e=0; e<this.nItems; e++) capacityPerElemCopy[e] = capacityPerElem[e];

        int cnt = 0;
        for (int i=0; i<freeOrders.size(); i++) {
            int curCnt = 0;
            boolean fits = true;

            for (Map.Entry<Integer, Integer> entry : this.orders.get(freeOrders.get(i)).entrySet()) {
                curCnt += entry.getValue();
                if (capacityPerElemCopy[entry.getKey()] < entry.getValue())
                    fits = false;
            }

            if (!fits || oldItemsSum + cnt + curCnt > this.waveSizeUB) continue;

            for (Map.Entry<Integer, Integer> entry : this.orders.get(freeOrders.get(i)).entrySet()) {
                capacityPerElemCopy[entry.getKey()] -= entry.getValue();
            }

            cnt += curCnt;

            newOrders.add(freeOrders.get(i));
        }
        
        double curBestSolution = (oldItemsSum + cnt) / (double) used_aisles.size();

        for (int a=0; a<this.aisles.size(); a++) {
            if (used_aisles.contains(a)) continue;

            for (int e=0; e<this.nItems; e++) capacityPerElemCopy[e] = capacityPerElem[e];

            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet())
                capacityPerElemCopy[entry.getKey()] += entry.getValue();

            List<Integer> addedOrders = new ArrayList<>();
            cnt = 0;

            for (int i=0; i<freeOrders.size(); i++) {
                double curCnt = 0;
                boolean fits = true;
                for (Map.Entry<Integer, Integer> entry : this.orders.get(freeOrders.get(i)).entrySet()) {
                    curCnt += entry.getValue();
                    if (capacityPerElemCopy[entry.getKey()] < entry.getValue())
                        fits = false;
                }

                if (!fits || oldItemsSum + cnt + curCnt > this.waveSizeUB) continue;

                for (Map.Entry<Integer, Integer> entry : this.orders.get(freeOrders.get(i)).entrySet()) {
                    capacityPerElemCopy[entry.getKey()] -= entry.getValue();
                }

                cnt += curCnt;

                addedOrders.add(freeOrders.get(i));
            }

            if ((oldItemsSum + cnt) / (double) (used_aisles.size() + 1) > curBestSolution) {
                bestAisle = a;
                newOrders.clear();
                for (int o : addedOrders) newOrders.add(o);
                curBestSolution = (oldItemsSum + cnt) / (double) (used_aisles.size() + 1);
            }
        }
        

        if (curBestSolution > oldSolutionValue) {
            if (bestAisle != -1) used_aisles.add(bestAisle);
            for (int o : newOrders) used_orders.add(o);
        }

        checkValid(used_orders, used_aisles);

        return curBestSolution;
    }

    private void checkValid(List<Integer> used_orders, List<Integer> used_aisles) {
        if (used_orders.size() != used_orders.stream().distinct().count()) {
            System.out.println("BUG: Orders repeated!!!");
        }

        if (used_aisles.size() != used_aisles.stream().distinct().count()) {
            System.out.println("BUG: Aisles repeated!!!");
        }

        int[] capacityPerElem = new int[this.nItems];

        for (int j=0; j<used_aisles.size(); j++)
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(used_aisles.get(j)).entrySet())
                capacityPerElem[entry.getKey()] += entry.getValue();

        double cnt = 0;
        for (int i=0; i<used_orders.size(); i++) {
            for (Map.Entry<Integer, Integer> entry : this.orders.get(used_orders.get(i)).entrySet()) {
                capacityPerElem[entry.getKey()] -= entry.getValue();
                cnt += entry.getValue();
            }
        }

        for (int e=0; e<this.nItems; e++) 
            if (capacityPerElem[e] < 0)
                System.out.println("BUG: Se excede la capacidad para el elemento de tipo " + e);

        if (cnt < this.waveSizeLB || cnt > this.waveSizeUB) 
            System.out.println("BUG: Demasiados elementos: LB=" + this.waveSizeLB + ", cnt=" + cnt + ",UB=" + this.waveSizeUB);
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