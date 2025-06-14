package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.IloLinearNumExpr;

public class BinarySolver extends MIPSolver {

    private double lower = 0;
    private double upper;
    private LagrangeSolver lagrangeSolver;

    public BinarySolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.lower = waveSizeLB;
        this.upper = waveSizeUB;
        this.lagrangeSolver = new LagrangeSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public int binarySearchSolution(List<Integer> used_orders, List<Integer> used_aisles, List<Map<Integer, Integer>> aisles , StopWatch stopWatch) {

        generateMIP(used_orders, used_aisles, (cplex, X, Y) -> {
            // Exigimos que sea mayor a k|A|
            IloLinearNumExpr exprX  = cplex.linearNumExpr();
            IloLinearNumExpr exprkY = cplex.linearNumExpr();

            for(int o = 0; o < this.orders.size(); o++) {
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                    exprX.addTerm(entry.getValue(), X[o]);
                }
            }
            
            for(int a = 0; a < this.aisles.size(); a++) 
                exprkY.addTerm(1, Y[a]);
            
            cplex.addGe(exprX, exprkY);
        });

        solveMIPWith(0, used_orders, used_aisles, 0);

        if (solutionInfeasible) return -1;
        
        // this.lower = Math.max(this.currentBest, (double) this.waveSizeLB / (double) this.aislesArray.length); // LB / |A| es una lower bound
        // this.upper = Math.min(this.upper, Math.min(greedyUpperBound(aisles), relaxationUpperBound()));

        double k;
        int it = 0;

        Duration startOfIteration = stopWatch.getDuration();
        boolean useLagrange = true;

        while (it < MAX_ITERATIONS && TOLERANCE <= this.upper - this.lower) {
            k = (this.lower + this.upper) / 2;

            System.out.println("Current range: (" + this.lower + ", " + this.upper + ")");

            solveMIPWith(k, used_orders, used_aisles, it);

            if (solutionInfeasible) 
                this.lower = this.currentBest;
            else 
                this.upper = k;
            
            if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;

            it++;
        }

        return it;
    }

    // We find upper bounds using the following observation: the best solution with
    // k aisles has is bounded by L_k / k where L_k is the sum of elements in the k aisles
    // with more elements.
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
