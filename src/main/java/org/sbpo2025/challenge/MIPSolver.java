package org.sbpo2025.challenge;

import java.util.Arrays;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public abstract class MIPSolver {

    protected final int[][] ordersArray;
    protected final int[][] aislesArray;
    protected final int nItems;
    protected final int waveSizeLB;
    protected final int waveSizeUB;

    // Constants
    protected final double TOLERANCE      = 1e-2;
    protected final long TIME_LIMIT_SEC   = 50;
    protected final double GAP_TOLERANCE  = 0.25;
    protected final int MAX_ITERATIONS    = 10;

    protected double currentBest;

    public MIPSolver(int[][] ordersArray, int[][] aislesArray, int nItems, int waveSizeLB, int waveSizeUB) {
        this.ordersArray    = ordersArray;
        this.aislesArray    = aislesArray;
        this.nItems         = nItems;
        this.waveSizeLB     = waveSizeLB;
        this.waveSizeUB     = waveSizeUB;

        this.currentBest = 0;
    }

    @FunctionalInterface
    public interface RunnableCode {
        void run(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y);
    }

    protected double solveMIP(double k, List<Integer> used_orders, List<Integer> used_aisles, RunnableCode extraCode) {
        double result = -1;
        IloCplex cplex = null;
        try {
            cplex = new IloCplex();

            IloIntVar[] X = new IloIntVar[this.ordersArray.length]; // La orden o está completa
            IloIntVar[] Y = new IloIntVar[this.aislesArray.length]; // El pasillo a fue recorrido

            initializeVariables(cplex, X, Y);

            // Mayor que LB y menor que UB
            setBoundsConstraints(cplex, X, Y);
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints(cplex, X, Y);

            // Hay que elegir por lo menos un pasillo
            setAtLeastOneAisleConstraint(cplex, Y);
            
            // Cplex Params
            setCPLEXParamsTo(cplex);

            if (extraCode != null) extraCode.run(cplex, X,Y);            

            // Función objetivo
            setObjectiveFunction(cplex, X, Y, k);
            
            // Inicializamos con el valor anterior
            usePreviousSolution(cplex, X, Y, used_orders, used_aisles);

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

    // Variables
    protected void initializeVariables(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        for(int o = 0; o < ordersArray.length; o++) 
            X[o] = cplex.intVar(0, 1, "X_" + o);

        for (int a = 0; a < aislesArray.length; a++) 
            Y[a] = cplex.intVar(0, 1, "Y_" + a);
    }

    // Constraints
    protected void setBoundsConstraints(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        IloLinearIntExpr exprLB = cplex.linearIntExpr();
        IloLinearIntExpr exprUB = cplex.linearIntExpr();

        for(int o = 0; o < ordersArray.length; o++) 
            for(int i = 0; i < nItems; i++) {
                exprLB.addTerm(ordersArray[o][i], X[o]);
                exprUB.addTerm(ordersArray[o][i], X[o]);
            }
        
        cplex.addGe(exprLB, this.waveSizeLB);
        cplex.addLe(exprUB, this.waveSizeUB);
    }

    protected void setOrderSelectionConstraints(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException {
        for(int i = 0; i < nItems; i++) {
            IloLinearIntExpr exprX = cplex.linearIntExpr();
            IloLinearIntExpr exprY = cplex.linearIntExpr();

            for(int o = 0; o < ordersArray.length; o++) 
                exprX.addTerm(ordersArray[o][i], X[o]);

            for(int a = 0; a < aislesArray.length; a++) 
                exprY.addTerm(aislesArray[a][i], Y[a]);

            cplex.addLe(exprX, exprY);
        }
    }
    
    private void setAtLeastOneAisleConstraint(IloCplex cplex, IloIntVar[] Y) throws IloException {
        IloLinearIntExpr exprY = cplex.linearIntExpr();
            
        for(int a = 0; a < aislesArray.length; a++) 
            exprY.addTerm(1, Y[a]);

        cplex.addGe(exprY, 1);
    }

    // Objective function
    private void setObjectiveFunction(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y, double alfa) throws IloException {
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for(int o = 0; o < ordersArray.length; o++) 
            for(int i = 0; i < nItems; i++)
                obj.addTerm(ordersArray[o][i], X[o]);
        
        for(int a = 0; a < aislesArray.length; a++) 
            obj.addTerm(-alfa, Y[a]);

        cplex.addMaximize(obj);
    }

    // CPLEX conifig
    protected void setCPLEXParamsTo(IloCplex cplex) throws IloException {
        int cutValue = 0;

        // Prints
        cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
        cplex.setParam(IloCplex.Param.MIP.Display, 0);    
        cplex.setOut(null); 
        cplex.setWarning(null); 

        // Time
        cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT_SEC);
        
        // GAP tolerance
        cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, GAP_TOLERANCE);

        // Preprocesamiento
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, true);

        // Planos de corte
        cplex.setParam(IloCplex.Param.MIP.Cuts.Cliques, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Covers, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Disjunctive, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.FlowCovers, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Gomory, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Implied, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.MIRCut, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.PathCut, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.LiftProj, cutValue);
        cplex.setParam(IloCplex.Param.MIP.Cuts.ZeroHalfCut, cutValue);

        // Heuristica primal
        cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, 0); // 1 ejecuta solo en raiz, n > 1 ejecuta cada n nodos del árbol 
    }

    private void usePreviousSolution(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y, List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        if (used_orders.size() != 0 || used_aisles.size() != 0) {
            IloIntVar[] varsToSet = new IloIntVar[used_orders.size() + used_aisles.size()];

            int index = 0;

            for (int o: used_orders) {
                varsToSet[index] = X[o];
                index++;
            }

            for (int a: used_aisles) {
                varsToSet[index] = Y[a];
                index++;
            }

            double[] ones = new double[varsToSet.length];
            Arrays.fill(ones, 1);

            // Nivel 0 (cero) MIPStartAuto: Automático, deja que CPLEX decida.
            // Nivel 1 (uno) MIPStartCheckFeas: CPLEX comprueba la viabilidad del inicio del PIM.
            // Nivel 2 MIPStartSolveFixed: CPLEX resuelve el problema fijo especificado por el inicio MIP.
            // Nivel 3 MIPStartSolveMIP: CPLEX resuelve un subMIP.
            // Nivel 4 MIPStartRepair: CPLEX intenta reparar el inicio MIP si es inviable, según el parámetro que establece la frecuencia para intentar reparar el inicio MIP inviable, ' CPXPARAM_MIP_Limits_RepairTries (es decir, ' IloCplex::Param::MIP::Limits::RepairTries).
            // Nivel 5 MIPStartNoCheck: CPLEX acepta el inicio del PIM sin ninguna comprobación. Es necesario completar el inicio de MIP.

            cplex.addMIPStart(varsToSet, ones, IloCplex.MIPStartEffort.Auto);
        }
    }

    // Soltuion
    protected void extractSolutionFrom(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y, List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        double curSolutionValue = getSolutionValueFromCplex(cplex, X, Y);

        if (curSolutionValue > this.currentBest) {
            this.currentBest = curSolutionValue;

            used_orders.clear();
            used_aisles.clear();      
            
            fillSolutionList(cplex, X, used_orders, ordersArray.length);
            fillSolutionList(cplex, Y, used_aisles, aislesArray.length);
           
        }
    }

    private void fillSolutionList(IloCplex cplex, IloIntVar[] V, List<Integer> solution, Integer size) throws IloException {
        for(int i = 0; i < size; i++)
            if (cplex.getValue(V[i]) > TOLERANCE) 
                solution.add(i);
    }

    private double getSolutionValueFromCplex(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) {
        double pickedObjects = 0, usedColumns = 0;
        
        try {
            for (int o=0; o < ordersArray.length; o++)
                if (cplex.getValue(X[o]) > TOLERANCE)
                    for(int i = 0; i < nItems; i++)
                        pickedObjects += ordersArray[o][i];
        
            for (int a=0; a < aislesArray.length; a++)
                if (cplex.getValue(Y[a]) > TOLERANCE)
                    usedColumns += 1;
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return pickedObjects / usedColumns;
    }


    // Solve the linear relaxation of the problem using the Charnes-Cooper transformation
    protected double relaxationUpperBound() {
        IloCplex cplex = null;
        double result = 1e9;
        try {
            cplex = new IloCplex();

            setCPLEXParamsTo(cplex);
            
            IloNumVar[] X = new IloNumVar[this.ordersArray.length]; // La orden o está completa
            IloNumVar[] Y = new IloNumVar[this.aislesArray.length]; // El pasillo a fue recorrido
            IloNumVar T = cplex.numVar(0, Double.MAX_VALUE, "T");

            IloLinearNumExpr obj = cplex.linearNumExpr();

            for(int o = 0; o < this.ordersArray.length; o++) 
                X[o] = cplex.numVar(0, 1, "X_" + o);

            for (int a = 0; a < this.aislesArray.length; a++) 
                Y[a] = cplex.numVar(0, 1, "Y_" + a);

            // Funcion obj

            for(int o = 0; o < this.ordersArray.length; o++) 
                for(int i = 0; i < nItems; i++)
                    obj.addTerm(this.ordersArray[o][i], X[o]);

            cplex.addMaximize(obj);

            // Denominador igual a 1 (Cooper)
            IloLinearNumExpr exprD = cplex.linearNumExpr();

            for(int a = 0; a < this.aislesArray.length; a++) 
                exprD.addTerm(1, Y[a]);

            cplex.addEq(1, exprD);

            // Mayor que LB y menor que UB
            IloLinearNumExpr exprLB = cplex.linearNumExpr();
            IloLinearNumExpr exprUB = cplex.linearNumExpr();

            for(int o = 0; o < this.ordersArray.length; o++)
                for(int i = 0; i < nItems; i++) {
                    exprLB.addTerm(this.ordersArray[o][i], X[o]);
                    exprUB.addTerm(this.ordersArray[o][i], X[o]);
                }

            exprLB.addTerm(-this.waveSizeLB, T);
            exprUB.addTerm(-this.waveSizeUB, T);
            
            cplex.addGe(exprLB, 0);
            cplex.addLe(exprUB, 0);
            
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            for(int i = 0; i < this.nItems; i++) {
                IloLinearNumExpr exprX = cplex.linearNumExpr();
    
                for(int o = 0; o < this.ordersArray.length; o++) 
                    exprX.addTerm(this.ordersArray[o][i], X[o]);
    
                for(int a = 0; a < this.aislesArray.length; a++) 
                    exprX.addTerm(-this.aislesArray[a][i], Y[a]);
    
                cplex.addLe(exprX, 0);
            }

            // Hay que elegir por lo menos un pasillo
            IloLinearNumExpr exprY = cplex.linearNumExpr();
            
            for(int a = 0; a < this.aislesArray.length; a++) 
                exprY.addTerm(1, Y[a]);

            exprY.addTerm(-1, T);

            cplex.addGe(exprY, 0);

            if (cplex.solve()) 
                result = cplex.getObjValue();
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (cplex != null) cplex.end();
        }

        return result;
    }

}
