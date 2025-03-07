package org.sbpo2025.challenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    
    private final double tolerance = 1e-2;
    private double lower = 0;
    private double upper;
    private double curValue;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.lower = waveSizeLB;
        this.upper = waveSizeUB;
        this.curValue = 0;
    }

    public ChallengeSolution solve(StopWatch stopWatch) {  
        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        Boolean useBinarySearchSolution = true;
        Boolean useParametricAlgorithmMILFP = false;
        String strategy = "";
        Integer maxIterations = 30, iterations = 1;


        if (useBinarySearchSolution) {
            iterations = binarySearchSolution(used_orders, used_aisles, maxIterations);
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
        double q = 0, objValue = 1;
        int it = 0;

        while (objValue >= 1e-3 && it < maxIterations) {
            objValue = solveParametricMILFP(q, used_orders, used_aisles);

            System.out.println("it: " + it + " q: " + q + " obj: " + objValue);

            if (objValue != -1) {
                int used_items = 0;

                for(int o = 0; o < used_orders.size(); o++) 
                    for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet())
                        used_items += entry.getValue();

                double last_q = q;
                
                q = (double) used_items / used_aisles.size();

                if (last_q == q) break;
            }
            
            it++;
        }

        return it;
    }

    private double solveParametricMILFP(double q, List<Integer> used_orders, List<Integer> used_aisles) {
        return solveMIP(q, used_orders, used_aisles, null);
    }

    // Busqueda Binaria
    
    private int binarySearchSolution(List<Integer> used_orders, List<Integer> used_aisles, int maxIterations) {
        if (binaryMIP(0, used_orders, used_aisles) == -1) return -1; // There is no solution
        
        this.lower = this.curValue;
        this.upper = Math.min(this.upper, Math.min(findAnotherUpperBound(), relaxationUpperBound()));
        double k;
        int it = 0;

        while (it < maxIterations && tolerance <= this.upper - this.lower) {
            k = (this.lower + this.upper) / 2;

            System.out.println("Current range: (" + this.lower + ", " + this.upper + ")");

            if (binaryMIP(k, used_orders, used_aisles) != -1) 
                this.lower = this.curValue;
            else 
                this.upper = k;
            
            it++;
        }

        return it;
    }

    // We find upper bounds using the following observation: the best solution with
    // k aisles has is bounded by L_k / k where L_k is the sum of elements in the k aisles
    // with more elements.
    private double findAnotherUpperBound() {
        // Get number of elements per aisle sorted increasingly
        List<Double> sortedElementsPerAisle = this.aisles.stream()
                                .mapToDouble(aisle -> aisle.values().stream()
                                                .mapToDouble(Integer::doubleValue)
                                                .sum())
                                .sorted()
                                .boxed()
                                .collect(Collectors.toList());
        
        Collections.reverse(sortedElementsPerAisle);

        double acum = 0;
        double upper_bound = 0;
        double iter = 1;

        for (double value : sortedElementsPerAisle) {
            acum += value;
            upper_bound = Math.max(upper_bound, acum / iter);
            iter++;
        }

        return upper_bound;
    }

    // Solve the linear relaxation of the problem using the Charnes-Cooper transformation
    private double relaxationUpperBound() {
        IloCplex cplex = null;
        double result = 1e9;
        try {
            cplex = new IloCplex();

            setCPLEXParamsTo(cplex);
            
            IloNumVar[] X = new IloNumVar[orders.size()]; // La orden o está completa
            IloNumVar[] Y = new IloNumVar[aisles.size()]; // El pasillo a fue recorrido
            IloNumVar T = cplex.numVar(0, Double.MAX_VALUE, "T");

            IloLinearNumExpr obj = cplex.linearNumExpr();

            for(int o = 0; o < orders.size(); o++) 
                X[o] = cplex.numVar(0, 1, "X_" + o);

            for (int a = 0; a < aisles.size(); a++) 
                Y[a] = cplex.numVar(0, 1, "Y_" + a);

            for(int o = 0; o < orders.size(); o++) 
                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) 
                    obj.addTerm(entry.getValue(), X[o]);

            cplex.addMaximize(obj);

            // Denominador igual a 1
            IloLinearNumExpr exprD = cplex.linearNumExpr();

            for(int a = 0; a < aisles.size(); a++) 
                exprD.addTerm(1, Y[a]);

            cplex.addEq(1, exprD);

            // Mayor que LB y menor que UB
            IloLinearNumExpr exprLB = cplex.linearNumExpr();
            IloLinearNumExpr exprUB = cplex.linearNumExpr();

            for(int o = 0; o < orders.size(); o++)
                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                    exprLB.addTerm(-entry.getValue(), X[o]);
                    exprUB.addTerm(entry.getValue(), X[o]);
                }

            exprUB.addTerm(-waveSizeUB, T);
            exprLB.setConstant(waveSizeLB);
            
            cplex.addLe(exprLB, 0);
            cplex.addLe(exprUB, 0);
            
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            for(int i = 0; i < nItems; i++) {
                IloLinearNumExpr exprX = cplex.linearNumExpr();
    
                for(int o = 0; o < orders.size(); o++) 
                    exprX.addTerm(orders.get(o).getOrDefault(i, 0), X[o]);
    
                for(int a = 0; a < aisles.size(); a++) 
                    exprX.addTerm(-aisles.get(a).getOrDefault(i, 0), Y[a]);
    
                cplex.addLe(exprX, 0);
            }

            // Hay que elegir por lo menos un pasillo
            IloLinearNumExpr exprY = cplex.linearNumExpr();
            
            for(int a = 0; a < aisles.size(); a++) 
                exprY.addTerm(-1, Y[a]);
            
            exprY.setConstant(1);

            cplex.addLe(exprY, 0);

            if (cplex.solve()) 
                result = cplex.getObjValue();
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (cplex != null) cplex.end();
        }

        return result;
    }

    // Binary Solution

    private double binaryMIP(double k, List<Integer> used_orders, List<Integer> used_aisles) {
        return solveMIP(k, used_orders, used_aisles, (cplex,X,Y) -> {
            try {
                // Exigimos que sea mayor a k|A|
                IloLinearNumExpr exprX  = cplex.linearNumExpr();
                IloLinearNumExpr exprkY = cplex.linearNumExpr();

                for(int o = 0; o < orders.size(); o++) {
                    for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                        exprX.addTerm(entry.getValue(), X[o]);
                    }
                }
                
                for(int a = 0; a < aisles.size(); a++) 
                    exprkY.addTerm(k, Y[a]);
                
                cplex.addGe(exprX, exprkY);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            
        });
    }

    @FunctionalInterface
    private interface RunnableCode {
        void run(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y);
    }
    
    private double solveMIP(double k, List<Integer> used_orders, List<Integer> used_aisles, RunnableCode extraCode) {
        IloCplex cplex = null;
        double result = -1;
        try {
            cplex = new IloCplex();

            IloIntVar[] X = new IloIntVar[orders.size()]; // La orden o está completa
            IloIntVar[] Y = new IloIntVar[aisles.size()]; // El pasillo a fue recorrido

            setVariablesAndConstraints(cplex, X, Y);

            extraCode.run(cplex, X,Y);            

            // Función objetivo
            setObjectiveFunction(cplex, X, Y, k);

            // Resolver
            if (cplex.solve())  {
                extractSolutionFrom(cplex, X, Y, used_orders, used_aisles);
                result = cplex.getObjValue();
            }
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        } finally {
            if (cplex != null) cplex.end();
        }
        
        return result;
    }

    private double getSolutionValueFromCplex(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) {
        double pickedObjects = 0, usedColumns = 0;
        
        try {
            for (int o=0; o < orders.size(); o++)
                if (cplex.getValue(X[o]) > tolerance)
                    for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) 
                        pickedObjects += entry.getValue();
        
            for (int a=0; a < aisles.size(); a++)
                if (cplex.getValue(Y[a]) > tolerance)
                    usedColumns += 1;
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return pickedObjects / usedColumns;
    }

    // CPLEX

    // Agrega las variables y restricciónes compartidas entre modelos
    private void setVariablesAndConstraints(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        setCPLEXParamsTo(cplex);

        initializeVariables(cplex, X, Y);

        // Mayor que LB y menor que UB
        setBoundsConstraints(cplex, X, Y);
                            
        // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
        setOrderSelectionConstraints(cplex, X, Y);

        // Hay que elegir por lo menos un pasillo
        setAtLeastOneAisleConstraint(cplex, Y);
    }

    private void setObjectiveFunction(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y, double alfa) throws IloException {
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for(int o = 0; o < orders.size(); o++) 
            for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) 
                obj.addTerm(entry.getValue(), X[o]);
        
        for(int a = 0; a < aisles.size(); a++) 
            obj.addTerm(-alfa, Y[a]);

        cplex.addMaximize(obj);
    }

    private void setCPLEXParamsTo(IloCplex cplex) throws IloException {
        cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
        cplex.setParam(IloCplex.Param.MIP.Display, 0);    
        cplex.setParam(IloCplex.Param.TimeLimit, 60);
        
        cplex.setOut(null); 
        cplex.setWarning(null); 
    }

    private void initializeVariables(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        for(int o = 0; o < orders.size(); o++) 
            X[o] = cplex.intVar(0, 1, "X_" + o);

        for (int a = 0; a < aisles.size(); a++) 
            Y[a] = cplex.intVar(0, 1, "Y_" + a);
    }

    private void setBoundsConstraints(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        IloLinearIntExpr exprLB = cplex.linearIntExpr();
        IloLinearIntExpr exprUB = cplex.linearIntExpr();

        for(int o = 0; o < orders.size(); o++) 
            for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                exprLB.addTerm(entry.getValue(), X[o]);
                exprUB.addTerm(entry.getValue(), X[o]);
            }
        
        cplex.addGe(exprLB, waveSizeLB);
        cplex.addLe(exprUB, waveSizeUB);
    }

    private void setOrderSelectionConstraints(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        for(int i = 0; i < nItems; i++) {
            IloLinearIntExpr exprX = cplex.linearIntExpr();
            IloLinearIntExpr exprY = cplex.linearIntExpr();

            for(int o = 0; o < orders.size(); o++) 
                exprX.addTerm(orders.get(o).getOrDefault(i, 0), X[o]);

            for(int a = 0; a < aisles.size(); a++) 
                exprY.addTerm(aisles.get(a).getOrDefault(i, 0), Y[a]);

            cplex.addLe(exprX, exprY);
        }
    }

    private void setAtLeastOneAisleConstraint(IloCplex cplex, IloIntVar[] Y) throws IloException {
        IloLinearIntExpr exprY = cplex.linearIntExpr();
            
        for(int a = 0; a < aisles.size(); a++) 
            exprY.addTerm(1, Y[a]);

        cplex.addGe(exprY, 1);
    }

    private void extractSolutionFrom(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y, List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        double curSolutionValue = getSolutionValueFromCplex(cplex, X, Y);

        if (curSolutionValue > this.curValue) {
            this.curValue = curSolutionValue;

            used_orders.clear();
            used_aisles.clear();      
            
            fillSolutionList(cplex, X, used_orders, orders.size());
            fillSolutionList(cplex, Y, used_aisles, aisles.size());
           
        }
    }

    private void fillSolutionList(IloCplex cplex, IloIntVar[] V, List<Integer> solution, Integer size) throws IloException {
        for(int i = 0; i < size; i++)
            if (cplex.getValue(V[i]) > tolerance) 
                solution.add(i);
    }

    // Para experimentar

    @SuppressWarnings("CallToPrintStackTrace")
    private void writeResults(String strategy, ChallengeSolution solution, StopWatch stopWatch, int maxIterations, int iterations) {
        String filePath = "./results/results_" + strategy + "_" + maxIterations + "_cooper.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath,  true))) {
            if (Files.size(Paths.get(filePath)) == 0) writer.write("ordenes,pasillos,items,factibilidad,obj,tiempo,it\n");
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
