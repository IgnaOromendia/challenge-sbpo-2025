package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;

public class ParametricSolver extends MIPSolver {

    private final double PRECISION = 1e-4;

    public ParametricSolver(int[][] ordersArray, int[][] aislesArray, int nItems, int waveSizeLB, int waveSizeUB) {
        super(ordersArray, aislesArray, nItems, waveSizeLB, waveSizeUB);
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        double objValue = 1, q = this.currentBest;
        int it = 1;

        Duration startOfIteration = stopWatch.getDuration();

        while (objValue >= PRECISION && it < MAX_ITERATIONS) {
            objValue = solveMIP(q, used_orders, used_aisles, null);
            
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            if (objValue == -1) break;

            double last_q = q;
            
            q = (double) objValue / used_aisles.size() + q;

            if (Math.abs(last_q - q) < PRECISION) break;
            if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        return it;
    }

    public void startFromGreedySolution(double greedySolutionValue) {
        this.currentBest = greedySolutionValue;
    }
    
}
