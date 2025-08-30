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
        try {
            this.currentBest = greedySolver.solve_with_both_greedies(used_orders, used_aisles, this.currentBest);
        } catch (Exception e) {
            this.currentBest = 0;
            used_orders.clear();
            used_aisles.clear();
        }


        double objValue = -1, lambda = this.currentBest;
        int it = 0;
        int startLocalSearch = 2;
        int neighbourhoodSize = 3;

        generateMIP(used_orders, used_aisles);

        Duration startOfInstance = stopWatch.getDuration();
        
        while (stopWatch.getDuration().getSeconds() - startOfInstance.getSeconds() <= TIME_LIMIT_SEC - 5) {
            System.out.println("it: " + it + " lambda: " + lambda + " obj: " + objValue);

            long remainingTime = TIME_LIMIT_SEC - stopWatch.getDuration().getSeconds() - 5;

            long startOfIteration = stopWatch.getDuration().getSeconds();

            if ((it >= startLocalSearch && it%2==0))
                objValue = solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, timeListener, remainingTime, true, neighbourhoodSize);
            else
                objValue = solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, timeListener, remainingTime);

            long endOfIteration = stopWatch.getDuration().getSeconds();

            long iterationDuration = endOfIteration - startOfIteration;
            if (timeListener.isGreaterThan(iterationDuration)) timeListener.updateTimeLimitTo(iterationDuration);

            if (solutionInfeasible) {
                used_orders.clear();
                used_aisles.clear();
                break;
            }
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            lambda += objValue / used_aisles.size(); 

            boolean isAGoodSolution = Math.abs(objValue + objValue * gapTolerance) <= PRECISION;

            if (timeListener.fastIteration(iterationDuration) || isAGoodSolution) {

                gapTolerance /= 2;

                System.out.println("Borra la linea de abajo SALAME");
                if (gapTolerance <= 0.01 && isAGoodSolution) break; // BORRAR
                
                System.out.println("GAP: " + gapTolerance + " TL: " + timeListener);
            }
            
            it++;
        }

        endCplex();

        return it;
    }

    
}
