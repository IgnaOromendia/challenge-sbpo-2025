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
    protected final double TOLERANCE        = 1e-2;
    protected double PRECISION              = 1e-4;
    protected final double BINARY_RANGE     = 1e-4;
    protected final long TIME_LIMIT_SEC     = 600;
    protected final long TIME_LIMIT_SEC_IT  = 10;
    protected final int MAX_ITERATIONS      = 100;

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
                    if (getBestObjValue() < -PRECISION) {
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
        // Prints
        this.cplex.setParam(IloCplex.Param.Simplex.Display, 0); 
        this.cplex.setParam(IloCplex.Param.MIP.Display, 0);    
        this.cplex.setOut(null); 
        this.cplex.setWarning(null); 

        // Threads
        int availableThreads = Runtime.getRuntime().availableProcessors();
        this.cplex.setParam(IloCplex.Param.Threads, availableThreads);
        this.cplex.setParam(IloCplex.Param.Parallel, IloCplex.ParallelMode.Opportunistic);
        cplex.setParam(IloCplex.Param.RandomSeed, 0);

        // Emphasis
        // 0 Balanceado
        // 1 Factibilidad
        // 2 Proximidad a solucion entera (GAP)
        // 3 Mejor bound dual
        // 4 Menos preprocesar
        // 5 Encontrar la solución probada más rapido
        this.cplex.setParam(IloCplex.Param.Emphasis.MIP, 0);
        
        // Preprocesamiento
        this.cplex.setParam(IloCplex.Param.Preprocessing.Presolve, true);
        this.cplex.setParam(IloCplex.Param.Preprocessing.Reduce, 3);

        /// RINS
        this.cplex.setParam(IloCplex.Param.MIP.Strategy.RINSHeur, 60);

        // Planos de corte
        // Volver a testear
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Cliques, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Covers, -1); // La que menos ayuda
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Disjunctive, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.FlowCovers, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Gomory, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.Implied, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.MIRCut, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.PathCut, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.LiftProj, 0);
        this.cplex.setParam(IloCplex.Param.MIP.Cuts.ZeroHalfCut, 0);

        // Heuristica primal
        this.cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, 0); // 1 ejecuta solo en raiz, n > 1 ejecuta cada n nodos del árbol 
    }

    protected void updateEmphasis(int newValue) {
        try {
            this.cplex.setParam(IloCplex.Param.Emphasis.MIP, newValue);
        } catch (IloException e){ 
            System.out.println("Error en la actualización de enfásis");
        }
        
    }

    protected void endCplex() {
        if (this.cplex != null) this.cplex.end();
    }

    protected abstract void initializeVariables() throws IloException;

    protected abstract void setBoundsConstraints() throws IloException;

    protected abstract void setOrderSelectionConstraints() throws IloException;

    protected abstract void setAtLeastOneAisleConstraint() throws IloException;


}
