package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class GreedyCplexSolver extends CPLEXSolver {

    // CPLEX
    protected final IloIntVar[] X; // La orden o está completa
    protected final IloIntVar[] Y; // El pasillo a fue recorrido
    protected IloRange aisleConstraintRange; // Rango de la pasillos

    protected final int[] orderItemSum;
    protected final int[] aislesSizes;
    protected final int[] perElemDemand;
    protected final List<List<Integer>> orderDemandsWithElem;
    // El array maxIncrementForAisle intenta estimar el mayor aporte que puede hacer un pasillo
    // Este se acota superiormente suponiendo que cada item del pasillo es utilizado para completar
    // la orden mas grande con ese elemento. Este tipo de cuenta puede ser una cota muy mala (en particular
    // contamos dos veces la misma orden). Considerar mejorar.
    protected final int[] maxIncrementForAisle;

    protected double currentBest;

    public GreedyCplexSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        this.currentBest = 0;

        X = new IloIntVar[this.orders.size()];
        Y = new IloIntVar[this.aisles.size()];

        this.orderItemSum = new int[orders.size()];
        
        for(int o = 0; o < this.orders.size(); o++)
            this.orderItemSum[o] = this.orders.get(o).values().stream().mapToInt(Integer::intValue).sum();

        this.perElemDemand = new int[nItems];
        this.orderDemandsWithElem = new ArrayList<>();
        for (int e=0; e < nItems; e++) this.orderDemandsWithElem.add(new ArrayList<>());

        this.maxIncrementForAisle = new int[aisles.size()];
        
        for(int o = 0; o < this.orders.size(); o++) {
            this.orderItemSum[o] = this.orders.get(o).values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                this.perElemDemand[entry.getKey()] += entry.getValue();
        }
            
        this.aislesSizes = new int[this.aisles.size()];

        for (int a=0; a < this.aisles.size(); a++) {
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()) {
                aislesSizes[a] += Math.min(entry.getValue(), perElemDemand[entry.getKey()]);
            }
        }

        for (int o=0; o < this.orders.size(); o++)
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet())
                this.orderDemandsWithElem.get(entry.getKey()).add(this.orderItemSum[o]);

        for (int e=0; e < this.nItems; e++) Collections.sort(this.orderDemandsWithElem.get(e), Collections.reverseOrder());

        for (int a=0; a < this.aisles.size(); a++) {
            for (Map.Entry<Integer, Integer> entry : this.aisles.get(a).entrySet()){ 
                List<Integer> ordersDemandForElem = orderDemandsWithElem.get(entry.getKey());
                int size = ordersDemandForElem.size();
                for (int i=0; i < entry.getValue(); i++) {
                    if (i < size) 
                        maxIncrementForAisle[a] += ordersDemandForElem.get(i);
                    else {
                        break;
                    }
                }
            }
        }
    }

    // Resovlemos acutalizando la función objetivo
    protected double solveMIPWith(List<Integer> aislesToFix) {
        
        List<IloRange> newConstraints = new ArrayList<>();
        double res = -1;

        try {
            this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
            this.cplex.setParam(IloCplex.Param.TimeLimit, 100000);

            for (int a=0; a < this.aisles.size(); a++) {
                if (aislesToFix.contains(a)) newConstraints.add(this.cplex.addEq(1, Y[a]));
                else newConstraints.add(this.cplex.addEq(0, Y[a]));
            }

            if (this.cplex.solve())  {
                res = this.cplex.getObjValue();
            } else {
                solutionInfeasible = cplex.getStatus() == IloCplex.Status.Infeasible;
            }

            for (IloRange constraint : newConstraints) this.cplex.remove(constraint);
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return res;
    }

    // Generamos el modelo una única vez por instancia
    protected void generateMIP() {
        try {
            // Cplex Params
            setCPLEXParamsTo();
            
            // Variables
            initializeVariables();

            // Mayor que LB y menor que UB
            setBoundsConstraints();
                                
            // Si la orden O fue hecha con elementos de i entonces pasa por los pasillos _a_ donde se encuentra i
            setOrderSelectionConstraints();

            // Seteo objetivo
            setObjectiveFunction();

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


    // Objective function
    protected void setObjectiveFunction() throws IloException {
        IloLinearNumExpr obj = this.cplex.linearNumExpr();

        for(int o = 0; o < this.orders.size(); o++) 
            obj.addTerm(this.orderItemSum[o], this.X[o]);    

        this.cplex.addMaximize(obj);
    }

    // Soltuion
    protected void extractSolutionFrom(List<Integer> used_orders, List<Integer> used_aisles) throws IloException {
        double curSolutionValue = getSolutionValueFromCplex();

        if (curSolutionValue > this.currentBest) {
            this.currentBest = curSolutionValue;

            Set<Integer> aisles_before = new HashSet<>(used_aisles);
            Set<Integer> aisles_before_copy = new HashSet<>(used_aisles);

            System.out.println("Before: size is " + used_aisles.size());

            used_orders.clear();
            used_aisles.clear();      
            
            fillSolutionList(this.X, used_orders, this.orders.size());
            fillSolutionList(this.Y, used_aisles, this.aisles.size());
         
            Set<Integer> aisles_after = new HashSet<>(used_aisles);

            aisles_before.removeAll(aisles_after);
            aisles_after.removeAll(aisles_before_copy);

            System.out.println("After: size is " + used_aisles.size());
            System.out.println("Difference between iterations is " 
                    + (aisles_before.size() + aisles_after.size()));       
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

    protected int ordersItemSum(List<Integer> ordersToSum) {
        int sum = 0;
        
        for(int order: ordersToSum) 
            for (Map.Entry<Integer, Integer> entry : this.orders.get(order).entrySet()) 
                sum += entry.getValue();

        return sum;
    }

    @Override
    protected void setAtLeastOneAisleConstraint() throws IloException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAtLeastOneAisleConstraint'");
    }
}

