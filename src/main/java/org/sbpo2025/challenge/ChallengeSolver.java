package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ilog.concert.*;
import ilog.cplex.*;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    
    private double tolerance = 1e-6;
    private int lower = 1;
    private int upper;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.upper = orders.size();
    }

    public ChallengeSolution solve(StopWatch stopWatch) {  
        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        Boolean useBinarySearchSolution = false;
        Boolean useParametricAlgorithmMILFP = true;
        String strategy = "";
        Integer maxIterations = 10, iterations = 1;


        if (useBinarySearchSolution) {
            binarySearchSolution(used_orders, used_aisles, maxIterations);
            strategy = "binary";
        } else if (useParametricAlgorithmMILFP) {
            iterations = parametricAlgorithmMILFP(used_orders, used_aisles, maxIterations);
            strategy = "parametric";
        }

        ChallengeSolution solution = new ChallengeSolution(Set.copyOf(used_orders), Set.copyOf(used_aisles));

        writeResults(strategy, solution, stopWatch, maxIterations, iterations);

        return solution;
    } 

    // Parametric algorithm MILFP

    private int parametricAlgorithmMILFP(List<Integer> used_orders, List<Integer> used_aisles, int maxIterations) {
        double q = 0;
        double objValue = 1;
        int it = 0;

        while (objValue >= tolerance && it < maxIterations) {
            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            objValue = solveParametricMILFP(q, used_orders, used_aisles);

            if (objValue != -1) q = (double) used_orders.size() / used_aisles.size();
            
            it++;
        }

        return it;
    }

    private double solveParametricMILFP(double q, List<Integer> used_orders, List<Integer> used_aisles) {
        double objValue = -1;
        try (IloCplex cplex = new IloCplex()) {
            cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
            cplex.setParam(IloCplex.Param.MIP.Display, 0);     
            cplex.setParam(IloCplex.Param.TimeLimit, 60);
            
            cplex.setOut(null); 
            cplex.setWarning(null); 

            // Variables
            
            IloNumVar[] X = new IloNumVar[orders.size()]; // La orden o está completa
            IloNumVar[] Y = new IloNumVar[aisles.size()]; // El pasillo a fue recorrido

            for(int o = 0; o < orders.size(); o++) 
                X[o] = cplex.intVar(0, 1, "X_" + o);

            for (int a = 0; a < aisles.size(); a++) 
                Y[a] = cplex.intVar(0, 1, "Y_" + a);

            // Restricciónes

            // Mayor que LB y menor que UB
            IloLinearNumExpr exprLB = cplex.linearNumExpr();
            IloLinearNumExpr exprUB = cplex.linearNumExpr();

            for(int o = 0; o < orders.size(); o++)
                for(int i = 0; i < nItems; i++) {
                    int itemAmount = orders.get(o).getOrDefault(i, 0);
                    exprLB.addTerm(itemAmount, X[o]); 
                    exprUB.addTerm(itemAmount, X[o]);
                }
                    
            cplex.addGe(exprLB, waveSizeLB);
            cplex.addLe(exprUB, waveSizeUB);
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            for(int i = 0; i < nItems; i++) {
                IloLinearNumExpr exprX = cplex.linearNumExpr();
                IloLinearNumExpr exprY = cplex.linearNumExpr();

                for(int o = 0; o < orders.size(); o++) 
                    exprX.addTerm(orders.get(o).getOrDefault(i, 0), X[o]);

                for(int a = 0; a < aisles.size(); a++) 
                    exprY.addTerm(aisles.get(a).getOrDefault(i, 0), Y[a]);

                cplex.addLe(exprX, exprY);
            }

            // Función objetivo
            
            IloLinearNumExpr obj = cplex.linearNumExpr();
            for(int o = 0; o < orders.size(); o++) 
                obj.addTerm(1, X[o]);
            
            for(int a = 0; a < aisles.size(); a++) 
                obj.addTerm(-q, Y[a]);

            cplex.addMaximize(obj);
                        
            // Resolver

            if (cplex.solve()) {
                used_orders.clear();
                used_aisles.clear();        

                for(int o = 0; o < orders.size(); o++)
                    if (cplex.getValue(X[o]) > tolerance) 
                        used_orders.add(o);
                        
                for(int a = 0; a < aisles.size(); a++)
                    if (cplex.getValue(Y[a]) > tolerance)
                         used_aisles.add(a);
                
                objValue = cplex.getObjValue();
            } else {
                System.out.println("Infactible - q: " + q);
            }

            cplex.end();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return objValue;
    }

    // Busqueda Binaria
    
    private void binarySearchSolution(List<Integer> used_orders, List<Integer> used_aisles, int maxIterations) {
        setBinarySearchBounds();

        int k = (lower + upper) / 2, it = 0;

        while (it < maxIterations && lower <= upper) {
            k = (lower + upper) / 2;
            
            if (solveMIP(k, used_orders, used_aisles)) 
                lower = k + 1;
            else 
                upper = k - 1;
            
            it++;
        }

    }

    private void setBinarySearchBounds() {
        List<Integer> itemsInOrders = new ArrayList<>();

        for (int o = 0; o < orders.size(); o++) {
            Integer sum = 0;
            for(int i = 0; i < nItems; i++) sum += orders.get(o).getOrDefault(i, 0);
            itemsInOrders.add(sum);
        }

        itemsInOrders.sort(Integer::compareTo);

        Integer cant_low = 0;
        Integer cant_upp = 0;

        for(int i = 0; i < itemsInOrders.size(); i++) {
            cant_low += itemsInOrders.get(itemsInOrders.size() - 1 - i);
            cant_upp += itemsInOrders.get(i);

            if (cant_low > waveSizeLB && lower == 1) lower = i;
            if (cant_upp > waveSizeUB && upper == itemsInOrders.size()) upper = i;

            if (cant_low > waveSizeLB && cant_upp > waveSizeUB) break;
        }
    }
    
    private Boolean solveMIP(Integer k, List<Integer> used_orders, List<Integer> used_aisles) {
        try (IloCplex cplex = new IloCplex()) {
            cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
            cplex.setParam(IloCplex.Param.MIP.Display, 0);     
            
            cplex.setOut(null); 
            cplex.setWarning(null); 

            // Variables
            
            IloNumVar[] X = new IloNumVar[orders.size()]; // La orden o está completa
            IloNumVar[] Y = new IloNumVar[aisles.size()]; // El pasillo a fue recorrido

            for(int o = 0; o < orders.size(); o++) 
                X[o] = cplex.intVar(0, 1, "X_" + o);

            for (int a = 0; a < aisles.size(); a++) 
                Y[a] = cplex.intVar(0, 1, "Y_" + a);

            // Restricciónes

            // Mayor que LB y menor que UB
            IloLinearNumExpr exprLB = cplex.linearNumExpr();
            IloLinearNumExpr exprUB = cplex.linearNumExpr();

            for(int o = 0; o < orders.size(); o++)
                for(int i = 0; i < nItems; i++) {
                    int itemAmount = orders.get(o).getOrDefault(i, 0);
                    exprLB.addTerm(itemAmount, X[o]); 
                    exprUB.addTerm(itemAmount, X[o]);
                }
                    
            cplex.addGe(exprLB, waveSizeLB);
            cplex.addLe(exprUB, waveSizeUB);
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            for(int i = 0; i < nItems; i++) {
                IloLinearNumExpr exprX = cplex.linearNumExpr();
                IloLinearNumExpr exprY = cplex.linearNumExpr();

                for(int o = 0; o < orders.size(); o++) 
                    exprX.addTerm(orders.get(o).getOrDefault(i, 0), X[o]);

                for(int a = 0; a < aisles.size(); a++) 
                    exprY.addTerm(aisles.get(a).getOrDefault(i, 0), Y[a]);

                cplex.addLe(exprX, exprY);
            }

            // Exigimos que sea mayor a k|A|
            IloLinearNumExpr exprX  = cplex.linearNumExpr();
            IloLinearNumExpr exprkY = cplex.linearNumExpr();

            for(int o = 0; o < orders.size(); o++) 
                exprX.addTerm(1, X[o]);
            
            for(int a = 0; a < aisles.size(); a++) 
                exprkY.addTerm(k, Y[a]);
            
            cplex.addGe(exprX, exprkY);

            // Función objetivo
            
            // nada por ahora
            
            // Resolver

            if (cplex.solve()) {
                used_orders.clear();
                used_aisles.clear();        

                for(int o = 0; o < orders.size(); o++)
                    if (cplex.getValue(X[o]) > tolerance) 
                        used_orders.add(o);
                        
                for(int a = 0; a < aisles.size(); a++)
                    if (cplex.getValue(Y[a]) > tolerance)
                         used_aisles.add(a);

            } else {
                cplex.end();
                return false;
            }
        
            cplex.end();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return true;
    }


    // Para experimentar

    private void writeResults(String strategy, ChallengeSolution solution, StopWatch stopWatch, int maxIterations, int iterations) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("results_" + strategy + "_" + maxIterations + ".csv",  true))) {
            writer.write(orders.size() + "," + aisles.size() + "," + nItems + "," + isSolutionFeasible(solution) + "," + computeObjectiveFunction(solution) + "," + (MAX_RUNTIME / 1000 - getRemainingTime(stopWatch)) + "," + iterations + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
