package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;


public class ParametricSolver extends MIPSolver {

    private final GreedySolver greedySolver;
    private final TimeListener timeListener;

    public ParametricSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        greedySolver = new GreedySolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        timeListener = new TimeListener(TIME_LIMIT_SEC_IT);
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, double gapTolerance, StopWatch stopWatch) {
        this.currentBest = greedySolver.solve_with_both_greedies(used_orders, used_aisles, this.currentBest);

        double objValue = -1, lambda = this.currentBest;
        int it = 0;
        int startLocalSearch = 2;
        int neighbourhoodSize = 3;
        int limitToStartDoingLocal = 0;

        generateMIP(used_orders, used_aisles, null);

        Duration startOfInstance = stopWatch.getDuration();
        
        while (it < MAX_ITERATIONS) {
            System.out.println("it: " + it + " lambda: " + lambda + " obj: " + objValue);

            updateCutConstraint(lambda);

            long remainingTime = TIME_LIMIT_SEC - stopWatch.getDuration().getSeconds() - 5;

            double oldObjValue = objValue;

            long startOfIteration = stopWatch.getDuration().getSeconds();

            if ((it >= startLocalSearch && it%2==0) || (remainingTime < limitToStartDoingLocal))
                objValue = solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, timeListener, remainingTime, true, neighbourhoodSize);
            else
                objValue = solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, timeListener, remainingTime - limitToStartDoingLocal);

            long endOfIteration = stopWatch.getDuration().getSeconds();


            System.out.println("TIME IT: " + (endOfIteration - startOfIteration));

            long iterationDuration = endOfIteration - startOfIteration;
            if (timeListener.isGreaterThan(iterationDuration)) timeListener.updateTimeLimitTo(iterationDuration);

            if (solutionInfeasible) break;
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            lambda += objValue / used_aisles.size(); 

            boolean isAGoodSolution = Math.abs(objValue + objValue * gapTolerance) <= PRECISION;

            if (oldObjValue == objValue || objValue <= 0 || isAGoodSolution ) {
                if (gapTolerance <= 0.01 && objValue <= PRECISION) break; // BORRAR

                if (timeListener.fastIteration(iterationDuration) || isAGoodSolution) gapTolerance /= 2;
                
                System.out.println("GAP: " + gapTolerance + " TL: " + timeListener);
            }

            if (stopWatch.getDuration().getSeconds() - startOfInstance.getSeconds() > TIME_LIMIT_SEC - 5) break;
            
            it++;
        }

        endCplex();

        return it;
    }

    
}
