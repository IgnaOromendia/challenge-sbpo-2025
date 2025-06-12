package org.sbpo2025.challenge;

import java.util.Arrays;
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
        double psiL = 0, psiU = 0, bestBound = 1e9, learningRate = 2;
        double[] mu = new double[nItems];
        for (int e=0; e<nItems; e++) mu[e] = 0;

        int count = 0;

        while(count < 75 && bestBound >= -1e-4) {
            double current = solve(lambda, mu, psiL, psiU);
            
            // Mejoramos los mu
            // Elementos en pasillos - Elementos en ordenes (ambas usadas)

            double[] muGap = new double[nItems];
            for (int e=0; e<nItems; e++) muGap[e] = 0;

            double psiLGap = -waveSizeLB, psiUGap = waveSizeUB;
            
            for(int o: usedOrders) {
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                    muGap[entry.getKey()] -= entry.getValue();
                    psiLGap += entry.getValue();
                    psiUGap -= entry.getValue();
                }
            }

            for(int a: usedAisles) {
                for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) {
                    muGap[entry.getKey()] += entry.getValue();
                }
            }

            for (int e=0; e<nItems; e++) {
                mu[e] = Math.max(0, mu[e] - learningRate * muGap[e]);
            }
            
            psiL = Math.max(0, psiL - learningRate * psiLGap);
            psiU = Math.max(0, psiU - learningRate * psiUGap);

            learningRate *= 0.9;

            count++;
            bestBound = Math.min(bestBound, current);
        }

        return bestBound;   
    }

    private double solve(double lambda, double[] mu, double psiL, double psiU) {
        usedOrders.clear();
        usedAisles.clear();
        
        double sum = 0;

        for(int o = 0; o < this.orders.size(); o++) {
            double acum = 0;

            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                acum += (double) entry.getValue() * (1 - mu[entry.getKey()] + psiL - psiU);
            
            if (acum <= 0) continue;

            usedOrders.add(o);
            sum += acum;
        }

        int bestAisle = -1;
        double bestAisleCost = -1e9;

        for(int a = 0; a < this.aisles.size(); a++) {
            double acum = 0;

            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) 
                acum += entry.getValue() * mu[entry.getKey()];
            
            acum -= lambda;

            if (bestAisleCost < acum) {
                bestAisle = a;
                bestAisleCost = acum;
            }

            if (acum <= 0) continue;

            usedAisles.add(a);
            sum += acum;
        }

        if (usedAisles.isEmpty()) {
            usedAisles.add(bestAisle);
            sum += bestAisleCost;
        }

        return sum;
    }

 }