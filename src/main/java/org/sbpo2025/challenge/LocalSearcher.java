package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalSearcher {

    // Problem data
    protected final List<Map<Integer, Integer>> orders;
    protected final List<Map<Integer, Integer>> aisles;
    protected final int nItems;
    protected final int waveSizeLB;
    protected final int waveSizeUB;

    public LocalSearcher(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders     = orders;
        this.aisles     = aisles;
        this.nItems     = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
    }

    public double search(List<Integer> used_orders, List<Integer> used_aisles) {
        return search(used_orders, used_aisles, 0);
    }

    public double search(List<Integer> used_orders, List<Integer> used_aisles, double currentBest) {

        List<Integer> localOrders = new ArrayList<>(used_orders);

        // Dispoinibildad de items
        int[] availableItems = new int[this.nItems]; 
        int[] availableOrders = new int[this.orders.size()];
        int availableGap = this.waveSizeUB;

        for(Integer a : used_aisles) 
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet())
                availableItems[entry.getKey()] += entry.getValue();

        for(Integer o : localOrders) 
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                availableGap -= entry.getValue();
                availableItems[entry.getKey()] -= entry.getValue();
            }

        for(int o = 0; o < this.orders.size(); o++)
            if (!localOrders.contains(o)) 
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
                localOrders.add(o);
                availableOrders[o] = 0;
                
            }
                
        // Por cada orden vemos si al eliminarla podemos agregar otra orden con más elementos
        for(int i = 0; i < localOrders.size(); i++) {
            int o = localOrders.get(i);

            int availableGapWithoutOrder = availableGap;
            boolean swapHappend = false;

            // Simulamos eliminar la orden o
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                availableGapWithoutOrder += entry.getValue();
                availableItems[entry.getKey()] += entry.getValue();
            }

            // Recorremos la ordenes no usadas
            for(int swapableOrder = 0; swapableOrder < this.orders.size(); swapableOrder++) {
                if (availableOrders[swapableOrder] == 0) continue;
                
                boolean flag = false;
                int availableGapCopy = availableGapWithoutOrder;

                // Simulamos agregarla chequeando que entre
                for (Map.Entry<Integer, Integer> entry : orders.get(swapableOrder).entrySet()) {
                    availableGapCopy -= entry.getValue();

                    if (availableItems[entry.getKey()] < entry.getValue() || availableGapCopy <= 0) {
                        flag = true;
                        break;
                    }
                }

                // Nos fijamos si nos pasamos del UB o si no mejoramos la canitdad de elementos usados
                if (flag || availableGap < availableGapCopy) continue;

                // Agregamos verdaderamente la orden
                for (Map.Entry<Integer, Integer> entry : orders.get(swapableOrder).entrySet())
                    availableItems[entry.getKey()] -= entry.getValue();

                // Actualizamos el gap
                availableGap = availableGapCopy;

                // Actualizamos las ordenes
                availableOrders[o] = 1;
                availableOrders[swapableOrder] = 0;

                localOrders.set(i, swapableOrder);

                swapHappend = true;

                break;
            }

            // Simulamos volver a meter la orden
            if (!swapHappend) {
                for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                    availableItems[entry.getKey()] -= entry.getValue();
                }
            }

        }

        double newBest = (double) (this.waveSizeUB - availableGap) / used_aisles.size();

        if (newBest > currentBest) {
            used_orders.clear();
            for(int o: localOrders) used_orders.add(o);
            currentBest = newBest;
        }

        return currentBest;
            
    }
    
}
