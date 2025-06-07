
package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;

import ilog.concert.IloLinearIntExpr;

public class FixedAisleSolver extends MIPSolver {

    public FixedAisleSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }
    
    public double solveFixedAisles(List<Integer> used_orders, List<Integer> used_aisles) {
        double best_value = -1;
        int fixedAislesBound = 40;

        generateMIP(used_orders, used_aisles, (cplex, X, Y) -> {
            // La solucion debe usar k pasillos
            IloLinearIntExpr exprY = cplex.linearIntExpr();
            for(int a = 0; a < this.aisles.size(); a++) 
                exprY.addTerm(1, Y[a]);
            
            aisleConstraintRange = cplex.addEq(exprY, 1);
        });

        for (int k = 1; k <= Math.min(aisles.size(), fixedAislesBound); k++) {
            best_value = Math.max(best_value, solveMIPFixed(k, used_orders, used_aisles));
            System.out.println(k + " " + best_value);
        }
            
        
        return best_value;
    }
    

}
