package org.sbpo2025.challenge;

import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

public class DivideSolver extends MIPSolver {


    private final List<Map<Integer, Integer>> sizeOneOrders;
    private final List<Map<Integer, Integer>> bigOrders;
    

    public DivideSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        sizeOneOrders = new ArrayList<>();
        bigOrders = new ArrayList<>();
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        Map<Integer, Integer> smallToOld = new HashMap<>();
        
        for (int o=0; o<orders.size(); o++) {
            int acum = 0;
            for (Map.Entry<Integer, Integer> entry : orders.get(0).entrySet()) {
                acum += entry.getValue();
                if (acum > 1) break;
            }
            if (acum > 1) bigOrders.add(orders.get(o));
            else {
                smallToOld.put(sizeOneOrders.size(), o);
                sizeOneOrders.add(orders.get(o));    
            }
        }

        used_orders.clear();
        used_aisles.clear();

        if (sizeOneOrders.size() > this.waveSizeLB) {
            GreedyCovering greedyCovering = new GreedyCovering(sizeOneOrders, aisles, nItems, waveSizeLB, waveSizeUB);
            double value = greedyCovering.solve(used_orders, used_aisles);
            System.out.println(value);
        }

        ParametricSolver paramSolver = new ParametricSolver(sizeOneOrders, aisles, nItems, waveSizeLB, waveSizeUB);
        
        int iter = paramSolver.solveMILFP(sizeOneOrders, used_orders, used_aisles, stopWatch);
        
        for (int i=0; i<used_orders.size(); i++) 
            used_orders.set(i, smallToOld.get(used_orders.get(i)));
        
        return iter;
    }
}
