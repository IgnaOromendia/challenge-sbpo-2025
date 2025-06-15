package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GreedyCovering {
    
    private final List<Map<Integer, Integer>> orders;
    private final List<Map<Integer, Integer>> aisles;
    private final int nItems;
    private final int waveSizeLB;
    private final int waveSizeUB;
    

    public GreedyCovering(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
    }

    public double solve(List<Integer> used_orders, List<Integer> used_aisles) {
        final int nOrders  = orders.size();
        final int nAisles  = aisles.size();

        // Agrupo las ordenes por item, para poder accederlas mas rapido mas adelante
        Map<Integer, List<Integer>> type2orders = new HashMap<>();
        for (int idx = 0; idx < nOrders; idx++) {
            int type = orders.get(idx).keySet().iterator().next();   // Tienen tamaño 1, esto me da el tipo del unico elemento
            type2orders.computeIfAbsent(type, k -> new ArrayList<>()).add(idx);
        }

        // Hago lo mismo con los pasillos
        Map<Integer, List<Integer>> type2aisles = new HashMap<>();
        for (int idx = 0; idx < nAisles; idx++) {
            for (int elemType : aisles.get(idx).keySet()) {
                type2aisles.computeIfAbsent(elemType, k -> new ArrayList<>()).add(idx);
            }
        }

        // El Score de un pasillo es la cantidad de ordenes que satisface si se elige, teniendo
        // en cuanta las ordenes ya agregadas
        int[] aisleScore = new int[nAisles];
        for (int i=0; i<nAisles; i++) aisleScore[i] = 0;

        // Al principio es facil de calcular. Por cada tipo, el minimo entre lo que tiene el pasillo y lo
        // que se necesita
        for (int idx = 0; idx < nAisles; idx++) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(idx).entrySet()) {
                if (type2orders.containsKey(entry.getKey()))
                    aisleScore[idx] += Math.min(entry.getValue(), type2orders.get(entry.getKey()).size());
            }
        }
        
        used_orders.clear();
        used_aisles.clear();

        // En cada iteracion elijo el pasillo de mayor score y lo pongo en la solucion parcial
        // Actualizo el estado de todas las variable (i.e. el score, y las ordenes disponibles)
        // Repito hasta llegar al lower bound de cantidad de ordenes a elegir.
        while (used_orders.size() < waveSizeLB || used_aisles.size() == 0) {
            // Elegimos el mejor pasillo
            int bestAisle = 0;
            for (int idx = 0; idx < nAisles; idx++) {
                //System.out.println("Aisle " + idx + " with score " + aisleScore[idx]);
                if (aisleScore[bestAisle] < aisleScore[idx]) {
                    bestAisle = idx;
                }
            }

            // Revisamos si ya usamos todos los pasillos, en cuyo caso el mejor score es negativo
            if (aisleScore[bestAisle] < 0) {
                break;
            }

            // Lo agregamos y actualizamos las ordenes cubiertas. Mandamos su score a -1 para marcarla usada
            used_aisles.add(bestAisle);
            aisleScore[bestAisle] = -1;
            for (Map.Entry<Integer, Integer> entry : aisles.get(bestAisle).entrySet()) {
                int typeElem = entry.getKey();
                int numElem = entry.getValue();

                // Calculamos cuantas ordenes puede satisfacer
                // Usamos el minimo entre las ordenes restantes y los que soporta el pasillo
                // Pero primero revisamos si siquiera hay una orden con ese elemento
                if (!type2orders.containsKey(typeElem) || type2orders.get(typeElem).isEmpty()) continue;

                int ordersOfThatTypeBeforeThisIter = type2orders.get(typeElem).size();
                int numCoveredByAisle = Math.min(ordersOfThatTypeBeforeThisIter, numElem);
                int orderOfThatTypeAfterIter = ordersOfThatTypeBeforeThisIter - numCoveredByAisle;

                // Actualizamos el conjunto de ordenes actuales 
                List<Integer> ordersWithElem = type2orders.get(typeElem);
                for (int i=0; i<numCoveredByAisle; i++) { 
                    int removedOrder = ordersWithElem.remove(ordersWithElem.size()-1); // Esto supuestamente es O(1)
                    used_orders.add(removedOrder);
                }

                // Actualizamos los scores. Solo impactamos en los pasillos con elementos de este tipo.
                // Este codigo en particular le baja el score al pasillo de la iteracion actual, pero no importa
                // a lo sumo queda mas negativo
                List<Integer> affectedAisles = type2aisles.get(typeElem);
                for (int aisleIdx : affectedAisles) {
                    int coverOfThisAisle = aisles.get(aisleIdx).get(typeElem);
                    // Vemos si todavia quedan suficientes ordenes de este tipo libres como para no cambiar
                    // el score
                    if (coverOfThisAisle <= orderOfThatTypeAfterIter)
                        continue;
                    // Sino, hay dos casos. Justo pasamos el limite, o ya lo habiamos pasado
                    if (coverOfThisAisle >= ordersOfThatTypeBeforeThisIter)
                        aisleScore[aisleIdx] -= numCoveredByAisle;
                    else // Sino, solo contamos los que se restaron a partir de los que podia cubrir.
                        aisleScore[aisleIdx] -= coverOfThisAisle - orderOfThatTypeAfterIter;
                }          
            }
        }

        // Si nos pasamos del upper, simplemente descartamos los ultimos pasillos.
        if (used_orders.size() > waveSizeUB) {
            int excess = used_orders.size() - waveSizeUB;
            for (int i=0; i<excess; i++) {
                used_orders.remove(used_orders.size()-1); // De nuevo, supuestamente O(1)
            }
            
        }
        
        if (used_orders.size() < waveSizeLB) { // No llegamos al LB.
            return -1;
        } else { // Sino, devolvemos el score de la solucion
            return (double) used_orders.size() / used_aisles.size();
        }
    }
}
