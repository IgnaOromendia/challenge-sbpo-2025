package org.sbpo2025.challenge;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public abstract class MIPSolver extends CPLEXSolver {

    // CPLEX
    private IloObjective objective;
    protected final IloIntVar[] X; // La orden o está completa
    protected final IloIntVar[] Y; // El pasillo a fue recorrido
    protected IloRange aisleConstraintRange; // Rango de la pasillos

    protected double currentBest;

    public MIPSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        this.currentBest = 0;

        X = new IloIntVar[this.orders.size()];
        Y = new IloIntVar[this.aisles.size()];
    }


    @FunctionalInterface
    protected interface RunnableCode {
        void run(IloCplex cplex, IloIntVar[] X, IloIntVar[] Y) throws IloException;
    }

    public void startFromGreedySolution(double greedySolutionValue) {
        this.currentBest = greedySolutionValue;
    }

    protected double getCplexUpperBound() {
        try {
            return this.cplex.getBestObjValue();
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    // Resovlemos acutalizando la función objetivo
    protected double solveMIPWith(double q, List<Integer> used_orders, List<Integer> used_aisles, int it) {
        try {
            this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, Math.max(0, GAP_TOLERANCE - (0.5 * it)));

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

        return solveMIPWith(0, used_orders, used_aisles, 0);
    }

    // Generamos el modelo una única vez por instancia
    protected void generateMIP(List<Integer> used_orders, List<Integer> used_aisles, RunnableCode extraCode) {
        try {
            // Cplex Params
            setCPLEXParamsTo();
            
            // Variables
            initializeVariables();

            // Mayor que LB y menor que UB
            setBoundsConstraints();
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints();

            // Hay que elegir por lo menos un pasillo
            setAtLeastOneAisleConstraint();

            if (extraCode != null) extraCode.run(this.cplex, this.X, this.Y);            
            
            // Inicializamos con el valor anterior
            usePreviousSolution(used_orders, used_aisles);
        
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
    
    protected void setAtLeastOneAisleConstraint() throws IloException {
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

}
