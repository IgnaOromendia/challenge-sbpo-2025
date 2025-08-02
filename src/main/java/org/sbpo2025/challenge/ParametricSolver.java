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
        this.currentBest = greedySolver.solve_with_both_greedies(used_orders, used_aisles, this.currentBest);

        double objValue = -1, lambda = this.currentBest;
        int it = 0;

        generateMIP(used_orders, used_aisles, anEffort, null);

        Duration startOfInstance = stopWatch.getDuration();
        long timeLimit = TIME_LIMIT_SEC_IT;
        int gapIteration = 4;
        
        while (it < MAX_ITERATIONS && timeLimit > 0) {
            System.out.println("it: " + it + " lambda: " + lambda + " obj: " + objValue);

            double old_obj = objValue;

            updateCutConstraint(lambda);

            long startOfIteration = stopWatch.getDuration().getSeconds();
            objValue = solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, Math.min(timeLimit, TIME_LIMIT_SEC - stopWatch.getDuration().getSeconds() - 5));
            long endOfIteration = stopWatch.getDuration().getSeconds();

            System.out.println("TIME IT: " + (endOfIteration - startOfIteration));

            if (solutionInfeasible) break;
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            lambda += objValue / used_aisles.size(); 

            if (objValue == old_obj || objValue <= 0 || Math.abs(objValue + objValue * gapTolerance) <= PRECISION) {
                if (gapTolerance <= 0.05 && objValue <= PRECISION) break; // BORRAR

                if (endOfIteration - startOfIteration >= timeLimit && timeLimit <= 80) 
                    timeLimit *= 2;
                else
                    gapTolerance /= 2;
                
                System.out.println("GAP: " + gapTolerance + " TL: " + timeLimit);
            }

            if (stopWatch.getDuration().getSeconds() - startOfInstance.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        endCplex();

        return it;
    }

    
}
