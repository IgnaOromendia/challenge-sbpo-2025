package org.sbpo2025.challenge;

import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class FixedAisleSolver extends MIPSolver {

    public FixedAisleSolver(int[][] ordersArray, int[][] aislesArray, int nItems, int waveSizeLB, int waveSizeUB) {
        super(ordersArray, aislesArray, nItems, waveSizeLB, waveSizeUB);
    }
    
    public double solveFixedAisles(List<Integer> used_orders, List<Integer> used_aisles) {
        double best_value = -1;
        int fixedAislesBound = 40;
        for (int k=1; k<=Math.min(aislesArray.length, fixedAislesBound); k++) {
            best_value = Math.max(best_value, solveMIP2(k, used_orders, used_aisles));
        }
        return best_value;
    }
    
    private double solveMIP2(Integer k, List<Integer> used_orders, List<Integer> used_aisles) {
        IloCplex cplex = null;
        double result = -1;
        try {
            cplex = new IloCplex();

            IloIntVar[] X = new IloIntVar[ordersArray.length]; // La orden o está completa
            IloIntVar[] Y = new IloIntVar[aislesArray.length]; // El pasillo a fue recorrido

            initializeVariables(cplex, X, Y);

            // Mayor que LB y menor que UB
            setBoundsConstraints(cplex, X, Y);
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints(cplex, X, Y);

            // La solucion debe usar k pasillos
            IloLinearIntExpr exprY = cplex.linearIntExpr();
            for(int a = 0; a < aislesArray.length; a++) 
                exprY.addTerm(1, Y[a]);
            cplex.addEq(exprY, k);
            
            setCPLEXParamsTo(cplex);           

            // Función objetivo
            IloLinearNumExpr obj = cplex.linearNumExpr();

            for(int o = 0; o < ordersArray.length; o++) 
                for(int i = 0; i < nItems; i++)
                    obj.addTerm(ordersArray[o][i], X[o]);
            
            cplex.addMaximize(obj);

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
    
}
