package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.IloLinearIntExpr;

public class HybridSolver extends MIPSolver {

    private double lowerBound;
    private double upperBound;
    
    private final RelaxationSolver relaxationSolver;
    private final LagrangeSolver lagrangeSolver;
    private final LocalSearcher localSearcher;

    public HybridSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.lowerBound = waveSizeLB / aisles.size();
        this.upperBound = waveSizeUB;
        this.relaxationSolver = new RelaxationSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.lagrangeSolver = new LagrangeSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.localSearcher = new LocalSearcher(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public int solveMILFP(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        double objValue = 1, q;
        int it = 0;

        boolean breakSym = false;

        if (breakSym) {
            List<List<Integer>> equivalentOrders = groupEquivalent(orders);
            List<List<Integer>> equivalentAisles = groupEquivalent(aisles);

            generateMIP(used_orders, used_aisles, (cplex, X, Y) -> {
                    for (List<Integer> groupOfOrders : equivalentOrders)
                        for (int idx=0; idx<groupOfOrders.size()-1; idx++)
                            cplex.addLe(X[groupOfOrders.get(idx+1)], X[groupOfOrders.get(idx)]);

                    for (List<Integer> groupOfAisles : equivalentAisles)
                        for (int idx=0; idx<groupOfAisles.size()-1; idx++)
                            cplex.addLe(Y[groupOfAisles.get(idx+1)], Y[groupOfAisles.get(idx)]);
                }
            );}
        else {generateMIP(used_orders, used_aisles, null);}

        this.lowerBound = Math.max(this.lowerBound, this.currentBest);
        this.upperBound = Math.min(this.upperBound, Math.min(greedyUpperBound(aisles), relaxationSolver.solveLP()));

        boolean onlyBinary = false;
        boolean mixWithBinary = false;
        boolean useLagrange = false;
        boolean useLocalSearch = true;

        while (Math.abs(objValue) > PRECISION && this.upperBound - this.lowerBound > BINARY_RANGE && it < MAX_ITERATIONS) {
            if (onlyBinary || (it % 2 == 0 && mixWithBinary)) {
                q = (this.lowerBound + this.upperBound) / 2;
                //q = (3 * this.lowerBound + this.upperBound) / 4
            } else {
                q = this.currentBest;
            }

            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);
            System.out.println("Current range: (" + this.lowerBound + ", " + this.upperBound + ")");

            if (useLagrange) {
                double lagrangeBound = lagrangeSolver.upperBounds(q);
                System.out.println("Lagrange encontro cota " + lagrangeBound);
                if (lagrangeBound < -PRECISION) {
                    this.upperBound = q; 
                    continue;
                }
            }
            
            objValue = solveMIPWith(q, used_orders, used_aisles);

            if (useLocalSearch) {

                double localBest = this.localSearcher.search(used_orders, used_aisles);

                if (localBest > this.currentBest) {
                    System.out.println("Local search mejora: " + this.currentBest + " a " + localBest);
                    this.currentBest = localBest;
                }
            }

            int usedItems = 0;

            for(int o: used_orders) 
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                    usedItems += entry.getValue();

            if (Math.abs(objValue) < PRECISION) {
                break;
            } else if (objValue >= PRECISION) {
                this.lowerBound = (double) usedItems / used_aisles.size();
                this.upperBound = Math.min(this.upperBound, getCplexUpperBound() + q);
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
    
    private List<List<Integer>> groupEquivalent(List<Map<Integer,Integer>> maps) {

        Map<Map<Integer,Integer>, List<Integer>> buckets = new HashMap<>();

        for (int i = 0; i < maps.size(); i++) {
            Map<Integer,Integer> m = maps.get(i);
            buckets.computeIfAbsent(m, k -> new ArrayList<>())
                .add(i);
        }

        return buckets.values().stream()
                  .sorted(Comparator.comparingInt(list -> list.get(0)))
                  .collect(Collectors.toList());
    }
    
}
