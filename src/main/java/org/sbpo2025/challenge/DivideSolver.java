package org.sbpo2025.challenge;

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
        Map<Integer, Integer> bigToOld = new HashMap<>();
        
        // Se dividen ordenes en grandes y chicas, se arman mappeos para indexar
        for (int o=0; o<orders.size(); o++) {
            int acum = 0;
            for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                acum += entry.getValue();
                if (acum > 1) break;
            }
            if (acum > 1) {
                bigToOld.put(bigOrders.size(), o);
                bigOrders.add(orders.get(o));
            }
            else {
                smallToOld.put(sizeOneOrders.size(), o);
                sizeOneOrders.add(orders.get(o));    
            }
        }

        used_orders.clear();
        used_aisles.clear();

        // Solucion greedy que usa solo ordenes de tamaño 1
        double greedySolution = 0;

        if (sizeOneOrders.size() > this.waveSizeLB) {
            GreedyCovering greedyCovering = new GreedyCovering(sizeOneOrders, aisles, nItems, waveSizeLB, waveSizeUB);
            greedySolution = greedyCovering.solve(used_orders, used_aisles);
        }

        // Resolvemos de forma exacta para las ordenes de tamaño 1 y para las otras
        ParametricSolver smallParamSolver = new ParametricSolver(sizeOneOrders, aisles, nItems, waveSizeLB, waveSizeUB);
        ParametricSolver bigParamSolver   = new ParametricSolver(bigOrders, aisles, nItems, waveSizeLB, waveSizeUB);

        smallParamSolver.startFromGreedySolution(greedySolution);

        List<Integer> big_used_orders = new ArrayList<>();
        List<Integer> big_used_aisles = new ArrayList<>();
        
        GreedySolver greedySolver = new GreedySolver(bigOrders, aisles, nItems, waveSizeLB, waveSizeUB);
        double greedySolutionForBigOrders = greedySolver.solve(big_used_orders, big_used_aisles);

        bigParamSolver.startFromGreedySolution(greedySolutionForBigOrders);

        int iter  = smallParamSolver.solveMILFP(sizeOneOrders, used_orders, used_aisles, stopWatch);
        int biter = bigParamSolver.solveMILFP(bigOrders, big_used_orders, big_used_aisles, stopWatch);

        // Remappeamos las soluciones a los indices reales
        for (int i=0; i< used_orders.size(); i++) {
            used_orders.set(i, smallToOld.get(used_orders.get(i)));
        }

        for (int i=0; i< big_used_orders.size(); i++) {
            big_used_orders.set(i, bigToOld.get(big_used_orders.get(i)));
        }

        double small_used_items = used_orders.size(); // Las ordenes tienen tamaño 1, la cantidad de items es simplemente la cantidad de ordenes 
        
        double big_used_items = 0;
        for(int o: big_used_orders) {
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                big_used_items += entry.getValue();
        }

        double smallOpt = used_aisles.size() > 0 ? small_used_items / used_aisles.size() : 0;
        double bigOpt = big_used_aisles.size() > 0 ? big_used_items / big_used_aisles.size() : 0;

        System.out.println("(Pre local serach) S: " + smallOpt + ", B: " + bigOpt);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        if (!big_used_aisles.isEmpty()) bigOpt = localSearcher.search(big_used_orders, big_used_aisles);
        if (!used_aisles.isEmpty()) smallOpt = localSearcher.search(used_orders, used_aisles);

        System.out.println("S: " + smallOpt + ", B: " + bigOpt);

        double bestOpt = smallOpt;

        if (bigOpt > smallOpt) {
            used_orders.clear();
            used_aisles.clear();

            for (int o : big_used_orders) used_orders.add(o);
            for (int a : big_used_aisles) used_aisles.add(a);
            
            // Xq no funciona esto
            // used_aisles = new ArrayList<>(big_used_aisles);
            bestOpt = bigOpt;
            iter = biter;
        }

        ParametricSolver finalSolver = new ParametricSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        finalSolver.startFromGreedySolution(bestOpt);
    
        finalSolver.solveMILFP(orders, used_orders, used_aisles, stopWatch);

        return iter;
    }
}
