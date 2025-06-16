package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

public class ParametricSolver extends MIPSolver {

    public ParametricSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public int solveMILFP(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        double objValue = 1, q = this.currentBest;
        int it = 0;

        generateMIP(used_orders, used_aisles, null);

        Duration startOfIteration = stopWatch.getDuration();

        while (Math.abs(objValue + Math.abs(objValue) * GAP_TOLERANCE) >= PRECISION && it < MAX_ITERATIONS) {
            objValue = solveMIPWith(q, used_orders, used_aisles, it);
            
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            if (solutionInfeasible) break;

            // localSearch(orders, used_orders, used_aisles);
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            q += (objValue / used_aisles.size()); // Antes era objValue / used_aisles.size() 

            if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        endCplex();

        return it;
    }

    
}
