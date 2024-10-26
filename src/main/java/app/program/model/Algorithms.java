package app.program.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Algorithms {
    FlowGraph fg;
    Shift[][] employeeShifts;

    public Algorithms(FlowGraph fg) {
        this.fg = fg;
        this.employeeShifts = new Shift[fg.getEmps().length][fg.getDaysInPeriod()];
    }

    // Breadth-First-Search version 1 (standard)
    public boolean bfsV1(Vertex s, Vertex t, Edge[] parentEdges, boolean[] nodesVisited) {
        Queue<Vertex> q = new LinkedList<>();
        for (int i = 0; i < nodesVisited.length; i++) {
            nodesVisited[i] = false;
            parentEdges[i] = null;
        }

        nodesVisited[s.getVertexIndex()] = true;
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            for (Edge e : node.getOutGoing()) {
                if (!nodesVisited[e.getTo().getVertexIndex()] && (e.getCap() - e.getFlow()) > 0) {
                    q.add(e.getTo());
                    nodesVisited[e.getTo().getVertexIndex()] = true;
                    parentEdges[e.getTo().getVertexIndex()] = e;
                }
                if (nodesVisited[t.getVertexIndex()]) return true;
            }
        }
        return false;
    }

    // Breadth-First-Search Version 2 (holds lower bounds)
    // TODO: need to work on the backwards flow
    public boolean bfsV2(Vertex s, Vertex t, Edge[] parentEdges, boolean[] nodesVisited) {
        Queue<Vertex> q = new LinkedList<>();
        int[][] minFlowMaxBound = new int[s.getTotalVertices()][2];
        int[] backFlows = new int[s.getTotalVertices()];

        for (int i = 0; i < nodesVisited.length; i++) {
            nodesVisited[i] = false;
            parentEdges[i] = null;
            minFlowMaxBound[i][0] = Integer.MAX_VALUE;
        }

        nodesVisited[s.getVertexIndex()] = true;
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            int minFlow = minFlowMaxBound[node.getVertexIndex()][0];
            int maxBound = minFlowMaxBound[node.getVertexIndex()][1];
            int backFlow = backFlows[node.getVertexIndex()];

            for (Edge e : node.getOutGoing()) {
                if (!nodesVisited[e.getTo().getVertexIndex()] && (e.getCap() - e.getFlow()) > 0  && checkLowerBoundConditions(e, minFlow, maxBound, backFlow)) {
                    q.add(e.getTo());
                    nodesVisited[e.getTo().getVertexIndex()] = true;
                    parentEdges[e.getTo().getVertexIndex()] = e;

                    if (e.getType() == 0) {
                        minFlowMaxBound[e.getTo().getVertexIndex()][0] = Math.min(minFlow, e.getCap()-e.getFlow()+backFlow);
                        minFlowMaxBound[e.getTo().getVertexIndex()][1] = Math.max(maxBound, e.getLowerBound()-e.getFlow());
                    } else {
                        minFlowMaxBound[e.getTo().getVertexIndex()][0] = Math.min(minFlow, minFlow);
                        minFlowMaxBound[e.getTo().getVertexIndex()][1] = maxBound;
                        backFlows[e.getTo().getVertexIndex()] = e.getCap()-minFlow;
                    }
                }
                if (nodesVisited[t.getVertexIndex()]) return true;
            }
        }
        return false;
    }

    public boolean checkLowerBoundConditions(Edge e, int minFlow, int maxBound, int backFlow) {
        minFlow = Math.min(minFlow, e.getCap()-e.getFlow());
        maxBound = Math.max(maxBound, e.getLowerBound()-e.getFlow());
        if (e.getType() == 0) {
            return backFlow + minFlow >= maxBound;
        }
        minFlow = Math.min(minFlow, e.getCap());
        return (e.getCounterpart().getFlow() - minFlow) >= maxBound || ((e.getCounterpart().getFlow() - minFlow) == 0);
    }

    // Edmonds-Karp for max flow
    // TODO: need to implement the backwards flow as well
    public int edmondsKarpV2(Vertex s, Vertex t) {
        Edge[] parentEdges = new Edge[s.getOutGoing().get(0).getTotalEdges()];
        int totalFlow = 0;
        boolean[] nodesVisited = new boolean[s.getTotalVertices()];
        int[] backFlows = new int[s.getTotalVertices()];

        while (bfsV2(s, t, parentEdges, nodesVisited)) {
            int bottleFlow = Integer.MAX_VALUE;
            Vertex node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                bottleFlow = Math.min(bottleFlow, edge.getCap()-edge.getFlow()+backFlows[node.getVertexIndex()]); // I think there should be special circumstances for the backwards edges
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            totalFlow += bottleFlow;

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                edge.addFlow(bottleFlow); 
                node = parentEdges[node.getVertexIndex()].getFrm();
            }
            // System.out.println("currently at " + ( (totalFlow/9224.0)*100.0) + "%");
            markEmployeeShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        }
        return totalFlow;
    }

    // Edmonds-Karp for max flow
    public int edmondsKarp(Vertex s, Vertex t) {
        Edge[] parentEdges = new Edge[s.getOutGoing().get(0).getTotalEdges()];
        int totalFlow = 0;
        boolean[] nodesVisited = new boolean[s.getTotalVertices()];

        while (bfsV1(s, t, parentEdges, nodesVisited)) {
            int bottleFlow = Integer.MAX_VALUE;
            Vertex node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                bottleFlow = Math.min(bottleFlow, edge.getCap()-edge.getFlow());
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            totalFlow += bottleFlow;

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                edge.addFlow(bottleFlow);
                node = parentEdges[node.getVertexIndex()].getFrm();
            }
            // System.out.println("currently at " + ( (totalFlow/9224.0)*100.0) + "%");
            markEmployeeShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        }
        return totalFlow;
    }




    // Successive shortest path algorithm: V1 (Original)
    public void shortestPathsV1(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
        }

        dist[s.getVertexIndex()] = 0;
        boolean[] inQ = new boolean[n];

        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            for (Edge e : node.getOutGoing()) {
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight()) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;
                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        
        }
    }

    // Successive Shortest Path Algorithm: V2 (with lower bounds)
    public void shortestPathsV2(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[][] minFlowMaxBound = new int[n][2];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlowMaxBound[i][0] = Integer.MAX_VALUE;
            minFlowMaxBound[i][1] = 0;
        }

        dist[s.getVertexIndex()] = 0;
        boolean[] inQ = new boolean[n];
        
        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            for (Edge e : node.getOutGoing()) {
                // System.out.println("Searching: " + e);
                int minFlow = minFlowMaxBound[node.getVertexIndex()][0];
                int maxBound = minFlowMaxBound[node.getVertexIndex()][1];
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight() && checkConditions(e, minFlow, maxBound)) {
                    // System.out.println("Passed the conditions");
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;

                    if (e.getType() == 0) {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap()-e.getFlow());
                        minFlowMaxBound[toNode.getVertexIndex()][1] = Math.max(maxBound, e.getLowerBound()-e.getFlow());
                    } else {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap()+minFlow);
                        minFlowMaxBound[toNode.getVertexIndex()][1] = maxBound;
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        
        }
    }

    public boolean checkConditions(Edge e, int minFlow, int maxBound) {
        minFlow = Math.min(minFlow, e.getCap()-e.getFlow());
        maxBound = Math.max(maxBound, e.getLowerBound()-e.getFlow());
        if (e.getType() == 0) {
            // System.out.println("Checking whether: " + minFlow + " >= " + maxBound);
            return minFlow >= maxBound;
        }
        minFlow = Math.min(minFlow, e.getCap());
        // System.out.println("Checking whether: " + (e.getCounterpart().getFlow() - minFlow) + " >= " + maxBound + " || " + (e.getCounterpart().getFlow() - minFlow) + " == 0");
        // System.out.println("returning: " + ((e.getCounterpart().getFlow() - minFlow) >= maxBound || ((e.getCounterpart().getFlow() - minFlow) == 0)));
        return (e.getCounterpart().getFlow() - minFlow) >= maxBound || ((e.getCounterpart().getFlow() - minFlow) == 0);
    }

    // Successive Shortest Path Algorithm: V3 (with 11-hour breaks between shifts)
    public void shortestPathsV3(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[][] minFlowMaxBound = new int[n][2];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlowMaxBound[i][0] = Integer.MAX_VALUE;
            minFlowMaxBound[i][1] = 0;
        }

        dist[s.getVertexIndex()] = 0;
        boolean[] inQ = new boolean[n];
        
        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        Employee[] employees = new Employee[n];
        int[] days = new int[n];
        Shift[] shifts = new Shift[n];

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            
            for (Edge e : node.getOutGoing()) {
                // initialize values to keep lowerbounds handled
                int minFlow = minFlowMaxBound[node.getVertexIndex()][0];
                int maxBound = minFlowMaxBound[node.getVertexIndex()][1];
                
                // initialize values for the 11-hour check
                Employee emp = employees[node.getVertexIndex()];
                int day = days[node.getVertexIndex()];
                Shift shift = shifts[node.getVertexIndex()];
                // define the node, which the current edge goes to
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight() && 
                checkConditions(e, minFlow, maxBound) && checkConditions11Hr(e, emp, day, shift)) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;

                    // for the lowerbound criteria
                    if (e.getType() == 0) {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap()-e.getFlow());
                        minFlowMaxBound[toNode.getVertexIndex()][1] = Math.max(maxBound, e.getLowerBound()-e.getFlow());
                    } else {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap());
                        minFlowMaxBound[toNode.getVertexIndex()][1] = maxBound;
                    }

                    // for the 11-hour criteria
                    if (node.getPurpose() == 1) {
                        employees[toNode.getVertexIndex()] = node.getEmp();
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                    } else if (node.getPurpose() == 2) {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = node.getDay();
                        shifts[toNode.getVertexIndex()] = shift;
                    } else if (node.getPurpose() == 3) {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = node.getShift();
                    } else {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        }
    }

    public boolean checkConditions11Hr(Edge e, Employee emp, int day, Shift shift) {
        if (e.getType() == 0) {
            if (emp == null || day < 1 || shift == null) return true;
            // for the day before
            boolean elevenRule = true;
            int endTime;
            int startTime;
            if (employeeShifts[emp.getEmpIndex()][day-1] != null) {
                elevenRule = true;
                endTime = employeeShifts[emp.getEmpIndex()][day-1].getEndTime();
                startTime = shift.getStartTime();
                if (endTime == 7) {
                    elevenRule = (startTime-endTime) >= 11;
                } else {
                    elevenRule = (24 - endTime + startTime) >= 11;
                }
            }
            // for the day after
            if (day >= employeeShifts[emp.getEmpIndex()].length-1 || employeeShifts[emp.getEmpIndex()][day+1] == null) return elevenRule;
            endTime = shift.getEndTime();
            startTime = employeeShifts[emp.getEmpIndex()][day+1].getStartTime();
            if (endTime == 7) {
                return elevenRule && (startTime-endTime) >= 11;
            } else {
                return elevenRule && (24 - endTime + startTime) >= 11;
            }
        }
        return true;
    }

    // Successive shortest path algorithm V4 (Makes sure there is a min-flow of 12 if a new 12-hour shift is selected) and that the department is the same
    public void shortestPathsV4(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[][] minFlowMaxBound = new int[n][2];
        Employee[] employees = new Employee[n];
        int[] days = new int[n];
        Shift[] shifts = new Shift[n];
        int[] deps = new int[n];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlowMaxBound[i][0] = Integer.MAX_VALUE;
            minFlowMaxBound[i][1] = 0;
            deps[i] = -1;
        }

        dist[s.getVertexIndex()] = 0;
        boolean[] inQ = new boolean[n];
        
        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            
            for (Edge e : node.getOutGoing()) {
                // initialize values to keep lowerbounds handled
                int minFlow = minFlowMaxBound[node.getVertexIndex()][0];
                int maxBound = minFlowMaxBound[node.getVertexIndex()][1];
                
                // initialize values for the 11-hour check
                Employee emp = employees[node.getVertexIndex()];
                int day = days[node.getVertexIndex()];
                Shift shift = shifts[node.getVertexIndex()];
                int dep = deps[node.getVertexIndex()];
                // define the node, which the current edge goes to
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight() && 
                checkConditions(e, minFlow, maxBound) && checkConditions11Hr(e, emp, day, shift)) { //&& checkConditions12Hr(e, Math.min(minFlow, e.getCap()-e.getFlow()), shift, dep)) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;

                    // for the lowerbound criteria
                    if (e.getType() == 0) {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap()-e.getFlow());
                        minFlowMaxBound[toNode.getVertexIndex()][1] = Math.max(maxBound, e.getLowerBound()-e.getFlow());
                    } else {
                        minFlowMaxBound[toNode.getVertexIndex()][0] = Math.min(minFlow, e.getCap());
                        minFlowMaxBound[toNode.getVertexIndex()][1] = maxBound;
                    }

                    // for the 11-hour and dep criteria 
                    if (node.getPurpose() == 1) {
                        employees[toNode.getVertexIndex()] = node.getEmp();
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                        deps[toNode.getVertexIndex()] = dep;
                    } else if (node.getPurpose() == 2) {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = node.getDay();
                        shifts[toNode.getVertexIndex()] = shift;
                        deps[toNode.getVertexIndex()] = dep;
                    } else if (node.getPurpose() == 3) {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = node.getShift();
                        deps[toNode.getVertexIndex()] = dep;
                    } else if (node.getPurpose() == 4) {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                        deps[toNode.getVertexIndex()] = node.getDep();
                    } else {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                        deps[toNode.getVertexIndex()] = dep;
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        }
    }

    // public boolean checkConditions12Hr(Edge e, int minFlow, Shift shift, int dep) {
    //     if (e.getTo().getPurpose() < 3 || e.getTo().getPurpose() > 3) return true;
    //     if (e.getTo().getShift().calcHours() != 12) return true;

    //     // if (dep == -1 || e.getTo().getPurpose() != 4 || e.getTo().getPurpose() != 5) return shift.calcHours() >= minFlow;
    //     // return shift.calcHours() >= minFlow && dep == e.getTo().getDep();
    // }

    public int[] minCostFlow(int n, int k, Vertex s, Vertex t) {
        int totalFlow = 0;
        int totalCost = 0;
        int[] dist = new int[n];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
        }

        Edge[] parentEdges = new Edge[n];

        while (totalFlow < k) {
            shortestPathsV1(n, s, dist, parentEdges);
            if (dist[t.getVertexIndex()] == Integer.MAX_VALUE) break;
            
            int bottleFlow = Integer.MAX_VALUE;
            Vertex node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                bottleFlow = Math.min(bottleFlow, edge.getCap()-edge.getFlow());
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            totalFlow += bottleFlow;
            totalCost += dist[t.getVertexIndex()];
            

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                edge.addFlow(bottleFlow);
                node = parentEdges[node.getVertexIndex()].getFrm();
            }
            // System.out.println("currently at " + ( totalFlow/((double) k)*100.0) + "%");
            markEmployeeShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        }
        int[] results = {totalFlow, totalCost};
        return results;

    }

    public void markEmployeeShifts(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }

        if (v == t) {
            Employee emp = null;
            int day = -1;
            Shift shift = null;
            for (Vertex node : path) {
                if (node.getPurpose() == 1) {
                    emp = node.getEmp();
                } else if (node.getPurpose() == 2) {
                    day = node.getDay();
                } else if (node.getPurpose() == 3) {
                    shift = node.getShift();
                }
            }
            // if (emp.getID().equals("BOTH27") && day == 20 && employeeShifts[emp.getEmpIndex()][day] == null) {
            //     System.out.println("Day " + (day-1) + ": " + employeeShifts[emp.getEmpIndex()][day-1]);
            //     System.out.println("Day " + (day) + ": " + employeeShifts[emp.getEmpIndex()][day]);
            //     System.out.println("Day " + (day+1) + ": " + employeeShifts[emp.getEmpIndex()][day+1]);
            // }
            employeeShifts[emp.getEmpIndex()][day] = shift;
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    markEmployeeShifts(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap());
                }
            }
        }

        path.remove(path.size()-1);
        if (flows.size() > 0) {
            flows.remove(flows.size()-1);
            capacities.remove(capacities.size()-1);
        }
        visited[v.getVertexIndex()] = false;

    }



    // Getters
    public Shift[][] getEmployeeShifts() {
        return employeeShifts;
    }


}
