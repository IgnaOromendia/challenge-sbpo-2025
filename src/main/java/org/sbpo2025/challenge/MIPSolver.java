package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public abstract class MIPSolver {

    protected final List<Map<Integer, Integer>> orders;
    protected final List<Map<Integer, Integer>> aisles;
    protected final int nItems;
    protected final int waveSizeLB;
    protected final int waveSizeUB;

    // CPLEX
    private IloCplex cplex;
    private IloObjective objective;
    protected final IloIntVar[] X; // La orden o está completa
    protected final IloIntVar[] Y; // El pasillo a fue recorrido
    protected IloRange aisleConstraintRange; // Rango de la pasillos
    
    protected Boolean solutionInfeasible = false;

    // Constants
    protected final double TOLERANCE      = 1e-2;
    protected final long TIME_LIMIT_SEC   = 60;
    protected final double GAP_TOLERANCE  = 0.25;
    protected final int MAX_ITERATIONS    = 10;

    protected double currentBest;

    public MIPSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders     = orders;
        this.aisles     = aisles;
        this.nItems     = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;

        this.currentBest = 0;

        try {
            this.cplex = new IloCplex();
        } catch (IloException e) {
            System.out.println("Error!!!!!!" + e.getMessage());
            this.cplex = null;
        } 

        X = new IloIntVar[this.orders.size()];
        Y = new IloIntVar[this.aisles.size()];
    }

    protected void endCplex() {
        if (this.cplex != null) this.cplex.end();
    }

    @FunctionalInterface
    protected interface RunnableCode {
        void run(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException;
    }

    // Resovlemos acutalizando la función objetivo
    protected double solveMIPWith(double q, List<Integer> used_orders, List<Integer> used_aisles) {
        try {
            setObjectiveFunction(q);
            if (this.cplex.solve())  {
                extractSolutionFrom(used_orders, used_aisles);
                return this.cplex.getObjValue();
            } else {
                solutionInfeasible = cplex.getStatus() == IloCplex.Status.Infeasible;
            }
        } catch (IloException e) {
            System.out.println(e.getMessage());
        } 
        return -1;
    }

    // En este caso el k es la cantidad de pasillos la cual vamos a querer tener
    protected double solveMIPFixed(int k, List<Integer> used_orders, List<Integer> used_aisles) {
        try {
            aisleConstraintRange.setBounds(k, k);
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return solveMIPWith(0, used_orders, used_aisles);
    }

    // Generamos el modelo una única vez por instancia
    protected void generateMIP(List<Integer> used_orders, List<Integer> used_aisles, RunnableCode extraCode) {
        try {
            initializeVariables();

            // Mayor que LB y menor que UB
            setBoundsConstraints();
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints();

            // Hay que elegir por lo menos un pasillo
            setAtLeastOneAisleConstraint();
            
            // Cplex Params
            setCPLEXParamsTo();

            if (extraCode != null) extraCode.run(this.cplex, this.X, this.Y);            
            
            // Inicializamos con el valor anterior
            // usePreviousSolution(used_orders, used_aisles);
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }
        
    }
    // Variables
    protected void initializeVariables() throws IloException {
        for(int o = 0; o < this.orders.size(); o++) 
            this.X[o] = this.cplex.intVar(0, 1, "X_" + o);

        for (int a = 0; a < this.aisles.size(); a++) 
            this.Y[a] = this.cplex.intVar(0, 1, "Y_" + a);
    }

    // Constraints
    protected void setBoundsConstraints() throws IloException {
        IloLinearIntExpr exprLB = this.cplex.linearIntExpr();
        IloLinearIntExpr exprUB = this.cplex.linearIntExpr();


        for(int o = 0; o < this.orders.size(); o++) {
            int sizeOfOrder = 0;
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) 
                sizeOfOrder += entry.getValue();
                
            exprLB.addTerm(sizeOfOrder, this.X[o]);
            exprUB.addTerm(sizeOfOrder, this.X[o]);
        }
            
        
        this.cplex.addGe(exprLB, this.waveSizeLB);
        this.cplex.addLe(exprUB, this.waveSizeUB);
    }

    protected void setOrderSelectionConstraints() throws IloException {
        IloLinearIntExpr[] exprsY = new IloLinearIntExpr[nItems];
        IloLinearIntExpr[] exprsX = new IloLinearIntExpr[nItems];

        for(int i = 0; i < nItems; i++) {
            exprsX[i] = this.cplex.linearIntExpr();
            exprsY[i] = this.cplex.linearIntExpr();
        }

        for(int o = 0; o < this.orders.size(); o++) {
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                exprsX[entry.getKey()].addTerm(entry.getValue(), this.X[o]);
            }
        }

        for(int a = 0; a < this.aisles.size(); a++) {
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) {
                exprsY[entry.getKey()].addTerm(entry.getValue(), this.Y[a]);
            }
        }

        for(int i = 0; i < nItems; i++)
            this.cplex.addLe(exprsX[i], exprsY[i]);

    }
    
    private void setAtLeastOneAisleConstraint() throws IloException {
        IloLinearIntExpr exprY = this.cplex.linearIntExpr();
            
        for(int a = 0; a < this.aisles.size(); a++) 
            exprY.addTerm(1, this.Y[a]);

        this.cplex.addGe(exprY, 1);
    }

    // Objective function
    protected void setObjectiveFunction(double alpha) throws IloException {
        IloLinearNumExpr obj = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) {
            int sumOfOrder = 0;
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                sumOfOrder += entry.getValue();

            obj.addTerm(sumOfOrder, this.X[o]);
        }
            
        
        for(int a = 0; a < this.aisles.size(); a++) 
            obj.addTerm(-alpha, this.Y[a]);

        if (this.objective != null) 
            this.cplex.delete(this.objective);
        

        this.objective = this.cplex.addMaximize(obj);
    }

    // CPLEX conifig
    protected void setCPLEXParamsTo() throws IloException {
        int cutValue = 0;

        // Prints
        this.cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
        this.cplex.setParam(IloCplex.Param.MIP.Display, 0);    
        this.cplex.setOut(null); 
        this.cplex.setWarning(null); 

        // Time
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT_SEC);
        
        // GAP tolerance
        this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, GAP_TOLERANCE);

        // Preprocesamiento
        this.cplex.setParam(IloCplex.Param.Preprocessing.Presolve, true);

        // Planos de corte
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Cliques, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Covers, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Disjunctive, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.FlowCovers, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Gomory, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Implied, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.MIRCut, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.PathCut, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.LiftProj, cutValue);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.ZeroHalfCut, cutValue);

        // Heuristica primal
        this.cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, 0); // 1 ejecuta solo en raiz, n > 1 ejecuta cada n nodos del árbol 
    }

    private void usePreviousSolution(List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        if (used_orders.size() != 0 || used_aisles.size() != 0) {
            IloIntVar[] varsToSet = new IloIntVar[used_orders.size() + used_aisles.size()];

            int index = 0;

            for (int o: used_orders) {
                varsToSet[index] = this.X[o];
                index++;
            }

            for (int a: used_aisles) {
                varsToSet[index] = this.Y[a];
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

            this.cplex.addMIPStart(varsToSet, ones, IloCplex.MIPStartEffort.Auto);
        }
    }

    // Soltuion
    protected void extractSolutionFrom(List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        double curSolutionValue = getSolutionValueFromCplex();

        if (curSolutionValue > this.currentBest) {
            this.currentBest = curSolutionValue;

            used_orders.clear();
            used_aisles.clear();      
            
            fillSolutionList(this.X, used_orders, this.orders.size());
            fillSolutionList(this.Y, used_aisles, this.aisles.size());
           
        }
    }

    private void fillSolutionList(IloIntVar[] V, List<Integer> solution, Integer size) throws IloException {
        for(int i = 0; i < size; i++)
            if (this.cplex.getValue(V[i]) > TOLERANCE) 
                solution.add(i);
    }

    private double getSolutionValueFromCplex() {
        double pickedObjects = 0, usedColumns = 0;
        
        try {
            for (int o=0; o < this.orders.size(); o++)
                if (this.cplex.getValue(this.X[o]) > TOLERANCE)
                    for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())        
                        pickedObjects += entry.getValue();
        
            for (int a=0; a < this.aisles.size(); a++)
                if (this.cplex.getValue(this.Y[a]) > TOLERANCE)
                    usedColumns++;
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return pickedObjects / usedColumns;
    }

    /* 
    // Solve the linear relaxation of the problem using the Charnes-Cooper transformation
    protected double relaxationUpperBound() {
        IloCplex tempCplex = null;
        double result = 1e9;
        try {
            tempCplex = new IloCplex();

            setCPLEXParamsTo();
            
            IloNumVar[] X = new IloNumVar[this.ordersArray.length]; // La orden o está completa
            IloNumVar[] Y = new IloNumVar[this.aislesArray.length]; // El pasillo a fue recorrido
            IloNumVar T = tempCplex.numVar(0, Double.MAX_VALUE, "T");

            IloLinearNumExpr obj = tempCplex.linearNumExpr();

            for(int o = 0; o < this.ordersArray.length; o++) 
                X[o] = tempCplex.numVar(0, 1, "X_" + o);

            for (int a = 0; a < this.aislesArray.length; a++) 
                Y[a] = tempCplex.numVar(0, 1, "Y_" + a);

            // Funcion obj

            for(int o = 0; o < this.ordersArray.length; o++) 
                for(int i = 0; i < nItems; i++)
                    obj.addTerm(this.ordersArray[o][i], X[o]);

            tempCplex.addMaximize(obj);

            // Denominador igual a 1 (Cooper)
            IloLinearNumExpr exprD = tempCplex.linearNumExpr();

            for(int a = 0; a < this.aislesArray.length; a++) 
                exprD.addTerm(1, Y[a]);

            tempCplex.addEq(1, exprD);

            // Mayor que LB y menor que UB
            IloLinearNumExpr exprLB = tempCplex.linearNumExpr();
            IloLinearNumExpr exprUB = tempCplex.linearNumExpr();

            for(int o = 0; o < this.ordersArray.length; o++)
                for(int i = 0; i < nItems; i++) {
                    exprLB.addTerm(this.ordersArray[o][i], X[o]);
                    exprUB.addTerm(this.ordersArray[o][i], X[o]);
                }

            exprLB.addTerm(-this.waveSizeLB, T);
            exprUB.addTerm(-this.waveSizeUB, T);
            
            tempCplex.addGe(exprLB, 0);
            tempCplex.addLe(exprUB, 0);
            
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            for(int i = 0; i < this.nItems; i++) {
                IloLinearNumExpr exprX = tempCplex.linearNumExpr();
    
                for(int o = 0; o < this.ordersArray.length; o++) 
                    exprX.addTerm(this.ordersArray[o][i], X[o]);
    
                for(int a = 0; a < this.aislesArray.length; a++) 
                    exprX.addTerm(-this.aislesArray[a][i], Y[a]);
    
                tempCplex.addLe(exprX, 0);
            }

            // Hay que elegir por lo menos un pasillo
            IloLinearNumExpr exprY = tempCplex.linearNumExpr();
            
            for(int a = 0; a < this.aislesArray.length; a++) 
                exprY.addTerm(1, Y[a]);

            exprY.addTerm(-1, T);

            tempCplex.addGe(exprY, 0);

            if (tempCplex.solve()) 
                result = tempCplex.getObjValue();
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (tempCplex != null) tempCplex.end();
        }

        return result;
    }
    */

}
