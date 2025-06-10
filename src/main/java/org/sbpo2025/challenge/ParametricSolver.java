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
        int it = 1;

        Duration startOfIteration = stopWatch.getDuration();

        generateMIP(used_orders, used_aisles, null);

        while (Math.abs(objValue + Math.abs(objValue) * GAP_TOLERANCE) >= PRECISION && it < MAX_ITERATIONS) {
            objValue = solveMIPWith(q, used_orders, used_aisles);
            
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            if (solutionInfeasible) break;

            // System.out.println("Antes: " + used_orders.size());

            // localSearch(orders, used_orders, used_aisles);

            // System.out.println("Después: " + used_orders.size());
            
            // Newton -> Qn+1 = Qn - F(Qn) / F'(Qn), F'(Qn) ≈ D(x^*)
            // Qn+1 = Qn - F(Qn) / -D(x^*) = N(x^*) / D(x^*)
            
            q += (objValue / used_aisles.size()); // Antes era objValue / used_aisles.size() 

            // if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        endCplex();

        return it;
    }

    public void startFromGreedySolution(double greedySolutionValue) {
        this.currentBest = greedySolutionValue;
    }

    private void localSearch(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles) {
        // Dispoinibildad de items
        int[] availableItems = new int[this.nItems]; 
        int[] availableOrders = new int[this.orders.size()];
        int availableGap = this.waveSizeUB;

        for(Integer a : used_aisles) 
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet())
                availableItems[entry.getKey()] += entry.getValue();

        for(Integer o : used_orders) 
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                availableGap -= entry.getValue();
                availableItems[entry.getKey()] -= entry.getValue();
            }
                

        for(int o = 0; o < this.orders.size(); o++)
            if (!used_orders.contains(o)) 
                availableOrders[o] = 1;
    
        // Por cada orden no usada intentar agregar en los pasillos usados
        for(int o = 0; o < this.orders.size(); o++)
            if (availableOrders[o] == 1) {
                boolean flag = false;
                int availableGapCopy = availableGap;

                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                    availableGapCopy -= entry.getValue();
                    if (availableItems[entry.getKey()] < entry.getValue() || availableGapCopy <= 0) flag = true;
                    if (flag) break;
                }

                if (flag) continue;
                    
                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet())
                    availableItems[entry.getKey()] -= entry.getValue();

                availableGap = availableGapCopy;
                
            }
                
        // Por cada orden vemos si al eliminarla podemos agregar otra orden con más elementos
    }
    
}
