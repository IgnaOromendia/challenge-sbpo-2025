package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LagrangeSolver {

    // Problem data
    protected final List<Map<Integer, Integer>> orders;
    protected final List<Map<Integer, Integer>> aisles;
    protected final int nItems;
    protected final int waveSizeLB;
    protected final int waveSizeUB;

    private final List<Integer> usedOrders;
    private final List<Integer> usedAisles;

    public LagrangeSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders     = orders;
        this.aisles     = aisles;
        this.nItems     = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;

        usedOrders = new ArrayList<>();
        usedAisles = new ArrayList<>();
    }

    public double upperBounds(double lambda) {
        double phi = 1, psiL = 1, psiU = 1, bestBound = 0, learningRate = 2;
        double[] mu = new double[nItems];
        int count = 0;

        while(count < 75) {
            double current = solve(lambda, mu, phi, psiL, psiU);

            
            // Mejoramos los mu
            // REVISAR
            for(int o: usedOrders) 
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                    // Elementos en pasillos - Elementos en ordenes (ambas usadas)
                    int usedItemAisles = 0, usedItemsOrders = 0;

                    for(int i = 0; i < nItems; i++) {
                        
                    }

                    // mu[entry.getKey()] = Math.max(0, mu[entry.getKey()])
                }


            
            
            // Mejoramos phi

            // Mejoramos psi LB

            // Mejoramos psi UB


            learningRate *= 0.9;

            count++;
        }

        return bestBound;
        
    }

    private double solve(double lambda, double[] mu, double phi, double psiL, double psiU) {
        double sum = 0;

        for(int o = 0; o < this.orders.size(); o++) {
            int acum = 0;

            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                acum += entry.getValue() * (1 - mu[entry.getKey()] + psiL - psiU);
            
            if (acum <= 0) continue;

            usedOrders.add(o);
            sum += acum;
        }

        for(int a = 0; a < this.aisles.size(); a++) {
            int acum = 0;

            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) 
                acum += entry.getValue() * mu[entry.getKey()];
            
            acum += phi - lambda;

            if (acum <= 0) continue;

            usedAisles.add(a);
            sum += acum;
        }

        return sum;
    }

 }