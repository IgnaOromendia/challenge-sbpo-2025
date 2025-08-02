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
    private GreedySolver greedySolver;
    private RelaxationSolver relaxationSolver;

    public BinarySolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.lower = waveSizeLB;
        this.upper = waveSizeUB;
        this.greedySolver = new GreedySolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.relaxationSolver = new RelaxationSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public int binarySearchSolution(List<Integer> used_orders, List<Integer> used_aisles, List<Map<Integer, Integer>> aisles, double gapTolerance, StopWatch stopWatch) {

        generateMIP(used_orders, used_aisles, (cplex, X, Y) -> {
            // Exigimos que sea mayor a k|A|
            IloLinearNumExpr exprX  = cplex.linearNumExpr();
            IloLinearNumExpr exprkY = cplex.linearNumExpr();

            for(int o = 0; o < this.orders.size(); o++) 
                exprX.addTerm(this.orderItemSum[o], X[o]);
            
            for(int a = 0; a < this.aisles.size(); a++) 
                exprkY.addTerm(1, Y[a]);
            
            cplex.addGe(exprX, exprkY);
        });

        if (solutionInfeasible) return -1;

        this.currentBest = greedySolver.solve_with_both_greedies(used_orders, used_aisles, this.currentBest);

        this.lower = Math.max(this.currentBest, (double) this.waveSizeLB / (double) this.aisles.size()); // LB / |A| es una lower bound
        this.upper = Math.min(this.upper, Math.min(greedyUpperBound(aisles), relaxationSolver.solveLP()));

        double k;
        int it = 0;
        long remainingTime = -1;

        Duration startOfIteration = stopWatch.getDuration();

        while (it < MAX_ITERATIONS && TOLERANCE <= this.upper - this.lower) {
            k = (this.lower + this.upper) / 2;

            System.out.println("Current range: (" + this.lower + ", " + this.upper + ")");

            remainingTime = TIME_LIMIT_SEC - stopWatch.getDuration().getSeconds() - 5;
            solveMIPWith(k, used_orders, used_aisles, gapTolerance - 0.05 * it, remainingTime);

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
