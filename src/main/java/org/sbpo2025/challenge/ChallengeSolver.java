package org.sbpo2025.challenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    
    // Solvers
    private final ParametricSolver parametricSolver;
    private final BinarySolver binarySolver;
    private final HybridSolver hybridSolver;
    private final DivideSolver divideSolver;
    private final HalfAisleSolver halfAisleSolver;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
                
        this.parametricSolver   = new ParametricSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.binarySolver       = new BinarySolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.hybridSolver       = new HybridSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.divideSolver       = new DivideSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
        this.halfAisleSolver    = new HalfAisleSolver(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public ChallengeSolution solve(StopWatch stopWatch) {  
        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        Boolean useBinarySearchSolution = false;
        Boolean useParametricAlgorithmMILFP = true;
        Boolean useHybrid = false;
        Boolean useDivide = false;
        boolean useHalf   = false;
        String strategy = "";
        Integer iterations = 1;

        if (useBinarySearchSolution) {
            iterations = binarySolver.binarySearchSolution(used_orders, used_aisles, aisles, 0.25, stopWatch);
            strategy = "binary";
        } else if (useParametricAlgorithmMILFP) {
            iterations = parametricSolver.solveMILFP(used_orders, used_aisles, 0.4, stopWatch);
            strategy = "parametric";
        } else if (useHybrid) {
            iterations = hybridSolver.solveMILFP(orders, used_orders, used_aisles, 0.25, stopWatch);
            strategy = "hybrid";
        } else if (useDivide) {
            iterations = divideSolver.solveMILFP(used_orders, used_aisles, stopWatch);
            strategy = "divide";
        } else if (useHalf) {
            iterations = halfAisleSolver.solveHalfAisleMILFP(used_orders, used_aisles, 0, stopWatch);
            strategy = "half";
        }

        ChallengeSolution solution = new ChallengeSolution(Set.copyOf(used_orders), Set.copyOf(used_aisles));

        writeResults(strategy, solution, stopWatch, iterations);

        return solution;
    } 

    // Para experimentar

    @SuppressWarnings("CallToPrintStackTrace")
    private void writeResults(String strategy, ChallengeSolution solution, StopWatch stopWatch, int iterations) {
        String filePath = "./results/" + strategy + "_b.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath,  true))) {
            if (Files.size(Paths.get(filePath)) == 0) writer.write("ordenes,pasillos,usados,obj,tiempo,it\n");
            writer.write(this.orders.size() + "," + this.aisles.size() + "," + solution.aisles().size() + "," + computeObjectiveFunction(solution) + "," + (MAX_RUNTIME / 1000 - getRemainingTime(stopWatch)) + "," + iterations + "\n");
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
