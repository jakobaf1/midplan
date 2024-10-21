package app.program.model;

public class Edge {
    private static int totalEdges = 0;
    private int edgeIndex = -1;
    private int type = 0;
    private Vertex frm = null;
    private Vertex to = null;
    private int cap = 0;
    private Edge counterpart = null;
    private int weight = 0;
    private int lowerBound = 0;
    private int flow = 0;

    public Edge(int type, Vertex frm, Vertex to, int cap) {
        this.type = type;
        this.frm = frm;
        this.to = to;
        this.cap = cap;
        totalEdges++;
        edgeIndex = totalEdges - 1;
    }

    public void addFlow(int flow) {
        if (this.type == 0) {
            this.flow += flow;
            this.counterpart.addToCap(flow);
        } else {
            this.counterpart.addFlow(-flow);
        }
    }

    public void addToCap(int cap) {
        this.cap += cap;
    }

    public String toString() {
        if (this.type == 0) {
            return this.frm + " -- (" + this.flow +"/" + this.cap +", w="+this.weight+", lw_b = "+this.lowerBound+" --> "+this.to;
        }
        return this.frm + " -- (" + this.cap + ") --> " + this.to;
    }
}
