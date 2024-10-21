package app.program.model;

public class Algorithms {
    FlowGraph fg;
    Shift[][] employeeShifts;

    public Algorithms(FlowGraph fg) {
        this.fg = fg;
        this.employeeShifts = new Shift[fg.getDaysInPeriod()][fg.getEmps().length];
    }

    // Successive shortest path algorithm
    public void shortestPaths(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        
    }



}
