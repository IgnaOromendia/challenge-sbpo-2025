package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public abstract class CPLEXSolver {

    // Problem data
    protected final List<Map<Integer, Integer>> orders;
    protected final List<Map<Integer, Integer>> aisles;
    protected final int nItems;
    protected final int waveSizeLB;
    protected final int waveSizeUB;
    
    // Cplex
    protected IloCplex cplex;

    // Constants
    protected final double TOLERANCE      = 1e-2;
    protected final double PRECISION      = 1e-4;
    protected final long TIME_LIMIT_SEC   = 60;
    protected final double GAP_TOLERANCE  = 0.25;
    protected final int MAX_ITERATIONS    = 10;

    protected Boolean solutionInfeasible = false;

    public CPLEXSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders     = orders;
        this.aisles     = aisles;
        this.nItems     = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;

        try {
            this.cplex = new IloCplex();
            
            this.cplex.use(new IloCplex.MIPInfoCallback() {
                @Override
                protected void main() throws IloException {
                    if (getBestObjValue() < PRECISION) {
                        System.out.print("Frenó por eps");
                        abort();
                    }
                }
            });

        } catch (IloException e) {
            System.out.println("Error!!!!!!" + e.getMessage());
            this.cplex = null;
        } 
    }

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

    protected void endCplex() {
        if (this.cplex != null) this.cplex.end();
    }

    protected abstract void initializeVariables() throws IloException;

    protected abstract void setBoundsConstraints() throws IloException;

    protected abstract void setOrderSelectionConstraints() throws IloException;

    protected abstract void setAtLeastOneAisleConstraint() throws IloException;


}
