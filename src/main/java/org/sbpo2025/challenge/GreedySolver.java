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

    public GreedySolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles,
                                    int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB; 
    }

    public double solve(List<Integer> ordersToSave, List<Integer> aislesToSave) {
        List<Integer> aislesSortedByNumElements = getMapIndicesSortedByMapSumOfValues(this.aisles);
        LinkedList<Integer> ordersSortedByNumElements = new LinkedList<>(getMapIndicesSortedByMapSumOfValues(this.orders)); // Linked list para remover en O(1) desde adentro
        
        int[] elementsPerType = new int[this.nItems]; // Inicializa a 0 automaticamente
        double curElements = 0, bestRatio = -1;

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
                if (this.waveSizeLB <= curElements && bestRatio < curElements / (curAisleIndex + 1)) {
                    bestRatio = curElements / (curAisleIndex + 1);
                    indexOrderSolution = ordersChosen.size();
                    indexAisleSolution = curAisleIndex + 1;
                }
            }
        }

        if (bestRatio != -1) { // Se encontro alguna solucion
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
}