package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

import ilog.cplex.IloCplex;

public class ParametricSolver extends MIPSolver {

    private final GreedySolver greedySolver;

    public ParametricSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        greedySolver = new GreedySolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, double gapTolerance, StopWatch stopWatch) {
        return solveMILFP(used_orders, used_aisles, gapTolerance, IloCplex.MIPStartEffort.Auto, stopWatch);
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, double gapTolerance, IloCplex.MIPStartEffort anEffort, StopWatch stopWatch) {
        this.currentBest = greedySolver.solve(used_orders, used_aisles, this.currentBest);

        double objValue = 1, q = this.currentBest;
        int it = 0;

        generateMIP(used_orders, used_aisles, anEffort, null);

        Duration startOfIteration = stopWatch.getDuration();

        while (Math.abs(objValue + Math.abs(objValue) * gapTolerance) >= PRECISION && it < MAX_ITERATIONS) {
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            objValue = solveMIPWith(q, used_orders, used_aisles, gapTolerance);
            
            if (solutionInfeasible) break;
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            q += (objValue / used_aisles.size()); // Antes era objValue / used_aisles.size() 

            // if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        endCplex();

        return it;
    }

    
}
