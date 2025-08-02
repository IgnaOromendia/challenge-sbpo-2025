package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

public class RelaxationSolver extends CPLEXSolver {

    private final IloNumVar[] X;
    private final IloNumVar[] Y;
    private IloNumVar T;

    public RelaxationSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        this.X = new IloNumVar[this.orders.size()]; // La orden o está completa
        this.Y = new IloNumVar[this.aisles.size()]; // El pasillo a fue recorrido

        try {
            this.T = cplex.numVar(0, Double.MAX_VALUE, "T");

            // Cplex Params
            setCPLEXParamsTo();
            
            // Variables
            initializeVariables();

            // Mayor que LB y menor que UB
            setBoundsConstraints();

            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints();

            // Hay que elegir por lo menos un pasillo
            // Queda opacada por la transformación que obliga a que el denominador sea igual a 1

            // Denominador igual a 1 (Cooper)
            setDenominatorEqualToOne();

            // Funcion obj
            setObjectiveFunction();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    // Solve the linear relaxation of the problem using the Charnes-Cooper transformation
    protected double solveLP() {
        double result = 1e9;
        try {

            if (this.cplex.solve()) 
                result = this.cplex.getObjValue();
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        endCplex();

        return result;
    }

    private void setObjectiveFunction() throws IloException {
        IloLinearNumExpr obj = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) {
            int sumOfOrder = 0;
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                sumOfOrder += entry.getValue();
   
            obj.addTerm(sumOfOrder, this.X[o]);
        }

        this.cplex.addMaximize(obj);
    }

    private void setDenominatorEqualToOne() throws IloException {
        IloLinearNumExpr exprD = this.cplex.linearNumExpr();

        for(int a = 0; a < this.aisles.size(); a++) 
            exprD.addTerm(1, Y[a]);

        this.cplex.addEq(1, exprD);
    }

    protected void initializeVariables() throws IloException {
        for(int o = 0; o < this.orders.size(); o++) 
            X[o] = this.cplex.numVar(0, 1, "X_" + o);

        for (int a = 0; a < this.aisles.size(); a++) 
            Y[a] = this.cplex.numVar(0, 1, "Y_" + a);
    }

    protected void setBoundsConstraints() throws IloException {
        IloLinearNumExpr exprLB = this.cplex.linearNumExpr();
        IloLinearNumExpr exprUB = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) {
            int sizeOfOrder = 0;
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                sizeOfOrder += entry.getValue();
                
            exprLB.addTerm(sizeOfOrder, this.X[o]);
            exprUB.addTerm(sizeOfOrder, this.X[o]);
        }

        exprLB.addTerm(-this.waveSizeLB, this.T);
        exprUB.addTerm(-this.waveSizeUB, this.T);
        
        this.cplex.addGe(exprLB, 0);
        this.cplex.addLe(exprUB, 0);
    }

    protected void setOrderSelectionConstraints() throws IloException {
        IloLinearNumExpr[] exprsX = new IloLinearNumExpr[nItems];

        for(int i = 0; i < nItems; i++) 
            exprsX[i] = this.cplex.linearNumExpr();
        
        for(int o = 0; o < this.orders.size(); o++) 
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                exprsX[entry.getKey()].addTerm(entry.getValue(), this.X[o]);
            
        for(int a = 0; a < this.aisles.size(); a++) 
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) 
                exprsX[entry.getKey()].addTerm(-entry.getValue(), this.Y[a]);
            
        for(int i = 0; i < nItems; i++)
            this.cplex.addLe(exprsX[i], 0);
    }

    protected void setAtLeastOneAisleConstraint() throws IloException {};



    
    
}
