package org.sbpo2025.challenge;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

public class HybridSolver extends MIPSolver {

    private double lowerBound;
    private double upperBound;

    public HybridSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.lowerBound = waveSizeLB / aisles.size();
        this.upperBound = waveSizeUB;
    }

    public int solveMILFP(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles, double greedyValue, StopWatch stopWatch) {
        double objValue = 1, q;
        int it = 1;

        generateMIP(used_orders, used_aisles, null);

        // System.out.println("h(lower) = " + solveMIPWith(this.lowerBound, used_orders, used_aisles));
        // System.out.println("h(upper) = " + solveMIPWith(this.upperBound, used_orders, used_aisles));

        this.lowerBound = Math.max(this.lowerBound, greedyValue);
        this.upperBound = Math.min(this.upperBound, greedyUpperBound(orders));

        while ((Math.abs(objValue) > PRECISION || this.upperBound - this.lowerBound > PRECISION) && it < MAX_ITERATIONS) {
            // q = (this.lowerBound + this.upperBound) / 2;
            q = (3 * this.lowerBound + this.upperBound) / 4;

            objValue = solveMIPWith(q, used_orders, used_aisles);

            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);
            System.out.println("Current range: (" + this.lowerBound + ", " + this.upperBound + ")");

            if (objValue > PRECISION) {
                int usedItems = 0;

                for(int o: used_orders) 
                    for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                        usedItems += entry.getValue();

                this.lowerBound = usedItems / used_aisles.size();
            } else {
                this.upperBound = q;
            }

            it++;
            
        }

        endCplex();

        return it;
    }

    private double greedyUpperBound(List<Map<Integer, Integer>> aisles) {
        // Get number of elements per aisle sorted increasingly
        List<Double> sortedElementsPerAisle = aisles.stream()
                                .mapToDouble(aisle -> aisle.values().stream()
                                                .mapToDouble(Integer::doubleValue)
                                                .sum())
                                .sorted()
                                .boxed()
                                .collect(Collectors.toList());
        
        Collections.reverse(sortedElementsPerAisle);

        double acum = 0;
        double upper_bound = 0;
        double iter = 1;

        for (double value : sortedElementsPerAisle) {
            acum += value;
            upper_bound = Math.max(upper_bound, acum / iter);
            iter++;
        }

        return upper_bound;
    }
    

    
}
