package org.sbpo2025.challenge;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    protected final int[] orderItemSum;
    protected final int[] perElemDemand;

    private IloRange cutConstraint;

    protected double currentBest;

    private long itStartingTime;
    private TimeListener itTimeListener;

    private IloRange aisleUpperBoundConstraint;

    public MIPSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        this.currentBest = 0;

        X = new IloIntVar[this.orders.size()];
        Y = new IloIntVar[this.aisles.size()];
        
        this.orderItemSum   = new int[orders.size()];
        this.perElemDemand  = new int[nItems];

        for(int o = 0; o < this.orders.size(); o++) {
            this.orderItemSum[o] = this.orders.get(o).values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                this.perElemDemand[entry.getKey()] += entry.getValue();
        }

    }


    protected double solveMIPWith(double lambda, List<Integer> used_orders, List<Integer> used_aisles, 
                        double gapTolerance, TimeListener timeListener, long remainingTime) {
        return solveMIPWith(lambda, used_orders, used_aisles, gapTolerance, timeListener, remainingTime, false, -1);
    }

    // Resovlemos acutalizando la función objetivo
    protected double solveMIPWith(double lambda, List<Integer> used_orders, List<Integer> used_aisles, 
                        double gapTolerance, TimeListener timeListener, long remainingTime,
                        boolean localSearch, long neighbourhoodSize) {
        
        IloRange neigbourhoodConstraint = null;
        double res = -1;

        try {
            if (localSearch) gapTolerance = gapTolerance / 2;

            this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, Math.max(0, gapTolerance));
            this.cplex.setParam(IloCplex.Param.TimeLimit, Math.max(0, remainingTime));

            this.itTimeListener = timeListener;
            this.itStartingTime = System.nanoTime();

            setObjectiveFunction(lambda);

            usePreviousSolution(used_orders, used_aisles);

            if (localSearch)
                neigbourhoodConstraint = addConstraintOfChangingFewAisles(used_aisles, neighbourhoodSize);

            if (this.cplex.solve())  {
                extractSolutionFrom(used_orders, used_aisles);
                res = this.cplex.getObjValue();
            } else {
                solutionInfeasible = cplex.getStatus() == IloCplex.Status.Infeasible;
            }
        } catch (IloException e) {
            System.out.println(e.getMessage());
        } 

        if (localSearch) {
            try {
                this.cplex.remove(neigbourhoodConstraint);
            } catch (IloException e) {
                System.out.println("Error borrando constraint de busqueda local");
            }
        }

        return res;
    }

    // Generamos el modelo una única vez por instancia
    protected void generateMIP(List<Integer> used_orders, List<Integer> used_aisles) {
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

            // Agrega restricciones de corte
            setCutConstraint(this.currentBest);

            // Acotamos la cantidad de pasillos con el mejor valor de lambda
            setNumberOfAislesUpperbound();

            this.cplex.use(new IloCplex.MIPInfoCallback() {
                public void main() throws IloException {
                    long currTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - itStartingTime);
                    if (itTimeListener.isLessOrEqualThan(currTime)) {   
                        if (getIncumbentObjValue() > IMPROVEMENT_LB) abort(); 
                        itTimeListener.doubleTimeLimit();         
                    }
                }
            });
        
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
            exprLB.addTerm(this.orderItemSum[o], this.X[o]);
            exprUB.addTerm(this.orderItemSum[o], this.X[o]);
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
                exprsY[entry.getKey()].addTerm(Math.min(entry.getValue(), this.perElemDemand[entry.getKey()]), this.Y[a]);
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

    private void setCutConstraint(double lambda) throws IloException {
        // Sum order item >=  lambda|A|
        IloLinearNumExpr exprX  = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) 
            exprX.addTerm(this.orderItemSum[o], X[o]);
        
        for(int a = 0; a < this.aisles.size(); a++) 
            exprX.addTerm(-lambda, Y[a]);
        
        this.cutConstraint = this.cplex.addGe(exprX, 0);
    }

    protected void updateCutConstraint(double lambda) {
        try {
            for (int a = 0; a < this.aisles.size(); a++)
                this.cplex.setLinearCoef(this.cutConstraint, this.Y[a], -lambda);
            
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }
        
    }

    // Objective function
    protected void setObjectiveFunction(double lambda) throws IloException {
        IloLinearNumExpr obj = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) 
            obj.addTerm(this.orderItemSum[o], this.X[o]);    
        
        for(int a = 0; a < this.aisles.size(); a++) 
            obj.addTerm(-lambda, this.Y[a]);

        if (this.objective != null) 
            this.cplex.delete(this.objective);
        
        this.objective = this.cplex.addMaximize(obj);
    }

    private void usePreviousSolution(List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        if (!used_orders.isEmpty() || !used_aisles.isEmpty()) {
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

            //  0 MIPStartAuto: Automático, deja que CPLEX decida.
            //  1 MIPStartCheckFeas: Verifica si la solución es válida, pero no la usa para la búsqueda.
            //  2 MIPStartSolveFixed: Fija las variables enteras a los valores dados y resuelve el problema resultante (solo variables continuas libres).
            //  3 MIPStartSolveMIP: Usa tu solución como punto de partida para resolver el MIP completo desde ahí.
            //  4 MIPStartRepair: Si no es factible, intenta repararla para convertirla en una solución válida.
            //  5 MIPStartNoCheck: Supone que la solución es factible sin comprobarla. Solo válida si el modelo no cambia desde que se generó el MIP start.

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

    protected void fillSolutionList(IloIntVar[] V, List<Integer> solution, Integer size) throws IloException {
        for(int i = 0; i < size; i++)
            if (this.cplex.getValue(V[i]) > TOLERANCE) 
                solution.add(i);
    }

    private double getSolutionValueFromCplex() {
        double pickedObjects = 0, usedColumns = 0;
        
        try {
            for (int o=0; o < this.orders.size(); o++)
                if (this.cplex.getValue(this.X[o]) > TOLERANCE)
                    pickedObjects += this.orderItemSum[o];
        
            for (int a=0; a < this.aisles.size(); a++)
                if (this.cplex.getValue(this.Y[a]) > TOLERANCE)
                    usedColumns++;
        
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return pickedObjects / usedColumns;
    }

    private IloRange addConstraintOfChangingFewAisles(List<Integer> used_aisles, long neighbourhoodSize) throws IloException {        
        IloLinearIntExpr expr = this.cplex.linearIntExpr();

        for (int a=0; a<this.aisles.size(); a++) {
            if (used_aisles.contains(a))
                expr.addTerm(1, this.Y[a]);
            else
                expr.addTerm(-1, this.Y[a]);
        }

        return this.cplex.addGe(expr, used_aisles.size() - neighbourhoodSize);
    }

    protected void setNumberOfAislesUpperbound() throws IloException {
        IloLinearIntExpr exprY = this.cplex.linearIntExpr();
            
        for(int a = 0; a < this.aisles.size(); a++) 
            exprY.addTerm(1, this.Y[a]);

        int bound;
        if (this.currentBest > 0) bound = (int) Math.floor(this.waveSizeUB / this.currentBest);
        else bound = this.aisles.size();

        aisleUpperBoundConstraint = this.cplex.addLe(exprY, bound);
    }

    protected void updateNumberOfAislesUpperbound(double lambda) throws IloException {
        int bound = (int) Math.floor(this.waveSizeUB / lambda);
        aisleUpperBoundConstraint.setUB(bound);
    }

}
