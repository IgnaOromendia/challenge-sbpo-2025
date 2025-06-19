package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

import ilog.cplex.IloCplex;

public class DivideSolver extends MIPSolver {


    private final List<Map<Integer, Integer>> sizeOneOrders;
    private final List<Map<Integer, Integer>> bigOrders;

    private final Map<Integer, Integer> smallToOld;
    private final Map<Integer, Integer> bigToOld;
    

    public DivideSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        sizeOneOrders   = new ArrayList<>();
        bigOrders       = new ArrayList<>();
        smallToOld      = new HashMap<>();
        bigToOld        = new HashMap<>();
    }

    public int solveMILFP(List<Integer> used_orders, List<Integer> used_aisles, StopWatch stopWatch) {
        List<Integer> big_used_orders = new ArrayList<>();
        List<Integer> big_used_aisles = new ArrayList<>();
        List<Integer> small_used_orders = new ArrayList<>();
        List<Integer> small_used_aisles = new ArrayList<>();

        // Se dividen ordenes en grandes y chicas, se arman mappeos para indexar
        divideOrders();

        used_orders.clear();
        used_aisles.clear();

        // Resolvemos de forma exacta para las ordenes de tamaño 1 y para las otras
        double smallOpt = solveSubMIP(sizeOneOrders, smallToOld, small_used_orders, small_used_aisles, stopWatch);
        double bigOpt   = solveSubMIP(bigOrders, bigToOld, big_used_orders, big_used_aisles, stopWatch);

        double bestOpt = -1;

        if (bigOpt > smallOpt) {
            for (int o : big_used_orders) used_orders.add(o);
            for (int a : big_used_aisles) used_aisles.add(a);
            
            // Xq no funciona esto
            // used_aisles = new ArrayList<>(big_used_aisles);
            bestOpt = bigOpt;
        } else {
            for (int o : small_used_orders) used_orders.add(o);
            for (int a : small_used_aisles) used_aisles.add(a);
            bestOpt = smallOpt;
        }

        ParametricSolver finalSolver = new ParametricSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        finalSolver.startFromGreedySolution(bestOpt);
    
        return finalSolver.solveMILFP(used_orders, used_aisles, 0.01, IloCplex.MIPStartEffort.SolveMIP, stopWatch);
    }

    private double solveSubMIP(List<Map<Integer,Integer>> subMIPOrders, Map<Integer,Integer> ordersMap, List<Integer> subMIPUsedOrders, List<Integer> subMIPUsedAisles, StopWatch stopWatch) {
        ParametricSolver paramSolver = new ParametricSolver(subMIPOrders, aisles, nItems, waveSizeLB, waveSizeUB);
        
        paramSolver.solveMILFP(subMIPUsedOrders, subMIPUsedAisles, 0.25, stopWatch);

        // Remappeamos las soluciones a los indices reales
        mapOrders(subMIPUsedOrders, ordersMap);

        double subMIPUsedItems = ordersItemSum(subMIPUsedOrders);
        double opt = -1;

        if (!subMIPUsedAisles.isEmpty()) {
            LocalSearcher localSearcher = new LocalSearcher(orders, aisles, nItems, waveSizeLB, waveSizeUB);
            opt = Math.max(subMIPUsedItems / subMIPUsedAisles.size(), localSearcher.search(subMIPUsedOrders, subMIPUsedAisles));
        }

        return opt;
    }

    private void mapOrders(List<Integer> used_orders, Map<Integer, Integer> ordersMap) {
        for (int i = 0; i < used_orders.size(); i++) 
            used_orders.set(i, ordersMap.get(used_orders.get(i)));   
    }

    private void divideOrders() {
        for (int o = 0; o < orders.size(); o++) {
            int acum = orderItemSum(o); // No es tan rápido como el for con el break pero no creo q tenga tanto impacto
            
            if (acum > 1) {
                bigToOld.put(bigOrders.size(), o);
                bigOrders.add(orders.get(o));
            } else {
                smallToOld.put(sizeOneOrders.size(), o);
                sizeOneOrders.add(orders.get(o));    
            }
        }
    }

}
