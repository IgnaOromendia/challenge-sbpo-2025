package org.sbpo2025.challenge;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

public class ParametricSolver extends MIPSolver {

    private final double PRECISION = 1e-4;

    public ParametricSolver(int[][] ordersArray, int[][] aislesArray, int nItems, int waveSizeLB, int waveSizeUB) {
        super(ordersArray, aislesArray, nItems, waveSizeLB, waveSizeUB);
    }

    public int solveMILFP(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        double objValue = 1, q = this.currentBest;
        int it = 1;

        Duration startOfIteration = stopWatch.getDuration();

        while (objValue >= PRECISION && it < MAX_ITERATIONS) {
            objValue = solveMIP(q, used_orders, used_aisles, null);
            
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            if (objValue == -1) break;

            System.out.println("Antes: " + used_orders.size());

            localSearch(orders, used_orders, used_aisles);

            System.out.println("Después: " +used_orders.size());

            double last_q = q;
            
            int used_items = 0;

            for(Integer o : used_orders) 
                for(int i = 0; i < nItems; i++)
                    used_items += this.ordersArray[o][i];
            
            q = (double) used_items / used_aisles.size() + q; // Antes era objValue / used_aisles.size() 

            if (Math.abs(last_q - q) < PRECISION) break;
            if (stopWatch.getDuration().getSeconds() - startOfIteration.getSeconds() > TIME_LIMIT_SEC) break;
            
            it++;
        }

        return it;
    }

    public void startFromGreedySolution(double greedySolutionValue) {
        this.currentBest = greedySolutionValue;
    }

    private void localSearch(List<Map<Integer, Integer>> orders, List<Integer> used_orders, List<Integer> used_aisles) {
        // Dispoinibildad de items
        int[] availableItems = new int[this.nItems]; 
        int[] availableOrders = new int[this.ordersArray.length];
        int availableGap = this.waveSizeUB;

        for(Integer a : used_aisles) 
            for(int i = 0; i < nItems; i++)
                availableItems[i] += this.aislesArray[a][i];

        for(Integer o : used_orders) 
            for(int i = 0; i < nItems; i++) {
                availableGap -= this.ordersArray[o][i];
                availableItems[i] -= this.ordersArray[o][i];
            }
                

        for(int o = 0; o < this.ordersArray.length; o++)
            if (!used_orders.contains(o)) 
                availableOrders[o] = 1;
    
        // Por cada orden no usada intentar agregar en los pasillos usados
        for(int o = 0; o < this.ordersArray.length; o++)
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
