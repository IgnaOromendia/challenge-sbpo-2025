package org.sbpo2025.challenge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloRange;

public class GreedyLPSolver extends MIPSolver {

    private final IloRange[] elementsConstraintRange;
    private final int[] limitByElem; 
    private final List<Integer> sortedAislesBySize;
    private int lastAisle;
    private double bestValue;
    
    public GreedyLPSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);

        elementsConstraintRange = new IloRange[nItems];
        
        limitByElem = new int[nItems];
        for (int i=0; i<nItems; i++) limitByElem[i] = 0;

        List<Map.Entry<Integer, Integer>> indicesAndSum = new ArrayList<>();

        for (int i = 0; i < aisles.size(); i++) {
            int sum = aisles.get(i).values().stream().mapToInt(Integer::intValue).sum();
            indicesAndSum.add(new AbstractMap.SimpleEntry<>(i, sum));
        }

        indicesAndSum.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        sortedAislesBySize = indicesAndSum.stream()
                            .map(a -> a.getKey())
                            .collect(Collectors.toList());

        lastAisle = 0;
        bestValue = -1;
    }

    protected void generateMIP() {
        try {
            setCPLEXParamsTo();
            
            initializeVariables();

            setBoundsConstraints();
                                
            setOrderSelectionConstraints();

            setObjectiveFunction();
     
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }
    }

    protected double updateWithAisles(int newAisleLimit, List<Integer> used_orders) {
        if (newAisleLimit <= this.lastAisle) {
            System.out.println("Se paso una cantidad de pasillos menor o igual a la actual");
            return -1;
        }

        newAisleLimit = Math.min(newAisleLimit, aisles.size());

        updateConstraints(newAisleLimit);

        try {
            
            if (cplex.solve()) {
                double objValue = cplex.getObjValue();
                if (objValue / newAisleLimit > this.bestValue) {
                    
                    used_orders.clear();
                    fillSolutionList(this.X, used_orders, this.orders.size());

                    this.bestValue = objValue / newAisleLimit;
                }

                return objValue / newAisleLimit;
            }


        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }

    protected void buildUsedAisles(int lastUsedAisle, List<Integer> used_aisles) {
        used_aisles.clear();
        for (int a=0; a<lastUsedAisle; a++) used_aisles.add(sortedAislesBySize.get(a));
    }

    protected void initializeVariables() throws IloException {
        for(int o = 0; o < this.orders.size(); o++) 
            this.X[o] = this.cplex.intVar(0, 1, "X_" + o);
    }

    protected void setOrderSelectionConstraints() throws IloException {
        IloLinearIntExpr[] exprsX = new IloLinearIntExpr[nItems];

        for(int i = 0; i < nItems; i++) {
            exprsX[i] = this.cplex.linearIntExpr();
        }

        for(int o = 0; o < this.orders.size(); o++) {
            for (Map.Entry<Integer, Integer> entry : this.orders.get(o).entrySet()) {
                exprsX[entry.getKey()].addTerm(entry.getValue(), this.X[o]);
            }
        }

        for(int i = 0; i < nItems; i++)
            this.elementsConstraintRange[i] = this.cplex.addLe(exprsX[i], 0);

    }

    private void updateConstraints(int newAisleLimit) {
        for (int a=lastAisle; a<newAisleLimit; a++) {
            for (Map.Entry<Integer,Integer> entry : this.aisles.get(sortedAislesBySize.get(a)).entrySet()) {
                this.limitByElem[entry.getKey()] += entry.getValue();
            }
        }
        
        try {
            for (int e=0; e<nItems; e++) {
                elementsConstraintRange[e].setBounds(0, limitByElem[e]);
            }
        } catch (IloException e) {
            System.out.println(e.getMessage());
        }

        lastAisle = newAisleLimit;
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
}
