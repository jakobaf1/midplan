package app.program.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class FlowAlgorithms {
    private FlowGraph fg;
    private Shift[][] employeeShifts;
    private int[][] deps;
    private List<Shift>[][] assignedShifts;

    private boolean stop = false;
    private int totalCost = 0;
    private int prefsFulfilled = 0;
    private int prefsDenied = 0;

    public FlowAlgorithms(FlowGraph fg) {
        this.fg = fg;
        // this.employeeShifts = new Shift[fg.getEmps().length][fg.getDaysInPeriod()];
        this.assignedShifts = new ArrayList[fg.getEmps().length][fg.getDaysInPeriod()];
        this.deps = new int[fg.getEmps().length][fg.getDaysInPeriod()];
        for (int emp = 0; emp < this.assignedShifts.length; emp++) {
            for (int day = 0; day < this.assignedShifts[0].length; day++) {
                this.assignedShifts[emp][day] = new ArrayList<>();
                this.deps[emp][day] = -1;
            }
        }


        // TODO: The following is just for the sake of tests, and should probably find a better solution
        // this.assignedShifts = new ArrayList[fg.getEmps()[0].getTotalEmployees()][fg.getDaysInPeriod()];
        // this.deps = new int[fg.getEmps()[0].getTotalEmployees()][fg.getDaysInPeriod()];
        // for (int emp = 0; emp < this.assignedShifts.length; emp++) {
        //     for (int day = 0; day < this.assignedShifts[0].length; day++) {
        //         this.assignedShifts[emp][day] = new ArrayList<>();
        //         this.deps[emp][day] = -1;
        //     }
        // }
        // this.employeeShifts = new Shift[fg.getEmps()[0].getTotalEmployees()][fg.getDaysInPeriod()];
    }

    // Breadth-First-Search Version 2 (holds lower bounds)
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
    // public int edmondsKarp(Vertex s, Vertex t) {
    //     Edge[] parentEdges = new Edge[s.getOutGoing().get(0).getTotalEdges()];
    //     int totalFlow = 0;
    //     boolean[] nodesVisited = new boolean[s.getTotalVertices()];

    //     while (bfs(s, t, parentEdges, nodesVisited)) {
    //         int bottleFlow = Integer.MAX_VALUE;
    //         Vertex node = t;
    //         while (node != s) {
    //             Edge edge = parentEdges[node.getVertexIndex()];
    //             bottleFlow = Math.min(bottleFlow, edge.getCap()-edge.getFlow());
    //             node = parentEdges[node.getVertexIndex()].getFrm();
    //         }

    //         totalFlow += bottleFlow;

    //         node = t;
    //         while (node != s) {
    //             Edge edge = parentEdges[node.getVertexIndex()];
    //             edge.addFlow(bottleFlow);
    //             node = parentEdges[node.getVertexIndex()].getFrm();
    //         }
    //         // System.out.println("currently at " + ( (totalFlow/9224.0)*100.0) + "%");
    //         markEmployeeShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
    //     }
    //     return totalFlow;
    // }




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
    public void shortestPaths11Hr(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[] minFlows = new int[n];
        
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
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
                // flow allowed to pass through current path
                int minFlow = Math.min(minFlows[node.getVertexIndex()],e.getCap()-e.getFlow());
                // initialize values for the 11-hour check
                Employee emp = employees[node.getVertexIndex()];
                int day = days[node.getVertexIndex()];
                Shift shift = shifts[node.getVertexIndex()];
                // define the node, which the current edge goes to
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight() && 
                checkConditions11Hr(e, emp, day, shift, minFlow) && sameDep(e, emp, day)) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;

                    // for the 11-hour criteria
                    if (toNode.getPurpose() == 1) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                    } else if (toNode.getPurpose() == 2) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = toNode.getDay();
                        shifts[toNode.getVertexIndex()] = shift;
                    } else if (toNode.getPurpose() == 3) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = toNode.getDay();
                        shifts[toNode.getVertexIndex()] = toNode.getShift();
                        if (e.getType() == 1 && minFlow >= e.getCap()) {
                            if (assignedShifts[emp.getEmpIndex()][day].size() > 1) {
                                for (int i = 0; i < assignedShifts[emp.getEmpIndex()][day].size(); i++) {
                                    if (assignedShifts[emp.getEmpIndex()][day].get(i).equals(toNode.getShift())) {
                                        assignedShifts[emp.getEmpIndex()][day].remove(i);
                                    }
                                }
                            } else if (!assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
                                assignedShifts[emp.getEmpIndex()][day].remove(0);
                            }
                            deps[emp.getEmpIndex()][day] = -1;
                        }
                    } else {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                    }

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[toNode.getVertexIndex()] = Math.min(minFlow, e.getCap()-e.getFlow());
                    } else {
                        minFlows[toNode.getVertexIndex()] = Math.min(minFlow, e.getCap());
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        }
    }

    public boolean sameDep(Edge e, Employee emp, int day) {
        if (emp == null || day == -1 || e.getTo().getPurpose() != 4) return true;
        if (deps[emp.getEmpIndex()][day] == -1) return true;
        return e.getTo().getDep() ==  deps[emp.getEmpIndex()][day];
    }

    public boolean checkConditions11Hr(Edge e, Employee emp, int day, Shift shift, int minFlow) {
        if (e.getType() == 0) {
            if (emp == null || day == -1 || shift == null) return true;
            if (assignedShifts[emp.getEmpIndex()][day].size() > 1) return false;
            if (!assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
                int currentStart = assignedShifts[emp.getEmpIndex()][day].get(0).getStartTime();
                int currentEnd = assignedShifts[emp.getEmpIndex()][day].get(0).getEndTime();
                boolean validShift = false;
                // The min-flow condition makes sure that a new shift can't be chosen unless it will form an accurate shift (e.g. 8/8 in 15-23 and 4/8 in 23-7 is not valid)
                for (Shift s : fg.getShifts()) {
                    if (currentStart == shift.getEndTime()) { // cases where new shift is before old shift (new shift can be 7-15 or 15-23)
                        if (shift.getStartTime() == s.getStartTime() && currentEnd-4 == s.getEndTime() && minFlow == 8) { // When new shift is 7-15 and old shift is 15-23 (resulting in a 7-19 shift)
                            validShift = true;
                            break;
                        } else if (shift.getStartTime()+4 == s.getStartTime() && currentEnd == s.getEndTime() && minFlow == 4) { // When new shift is 15-23 and old is 23-7 (resulting in a 19-7 shift)
                            validShift = true;
                            break;
                        }
                        
                    } else if (currentEnd == shift.getStartTime()) { // cases where new shift is after old shift (new shift can be 15-23 or 23-7)
                        if (currentStart == s.getStartTime() && shift.getEndTime()-4 == s.getEndTime() && minFlow == 4) { // When new shift is 15-23 and old is 7-15 (resulting in 7-19)
                            validShift = true;
                            break;
                        } else if (currentStart+4 == s.getStartTime() && shift.getEndTime() == s.getEndTime()  && minFlow == 8) { // When new shift is 23-7 and old is 15-23 (resulting in a 19-7)
                            validShift = true;
                            break;
                        } 
                    }
                    
                }
                if (!validShift) return false;
            }
            // for the day before
            boolean elevenRule = true;
            int endTime;
            int startTime = shift.getStartTime();
            if (day != 0) {
                if (!assignedShifts[emp.getEmpIndex()][day-1].isEmpty()) {
                    endTime = assignedShifts[emp.getEmpIndex()][day-1].get(0).getEndTime();
                    elevenRule = true;
                    if (assignedShifts[emp.getEmpIndex()][day-1].size() > 1 ) {
                        for (Shift s : assignedShifts[emp.getEmpIndex()][day-1]) {
                            if (s.getEndTime() == 7) {
                                endTime = s.getEndTime();
                                break;
                            } else {
                                endTime = 19;
                            }
                        }
                    }
                    if (endTime == 7) {
                        elevenRule = (startTime-endTime) >= 11;
                    } else {
                        elevenRule = (24 - endTime + startTime) >= 11;
                    }
                }
            }
            // for the day after
            if (day >= assignedShifts[emp.getEmpIndex()].length-1 || assignedShifts[emp.getEmpIndex()][day+1].isEmpty()) return elevenRule;
            endTime = shift.getEndTime();
            startTime = assignedShifts[emp.getEmpIndex()][day+1].get(0).getStartTime();
            if (assignedShifts[emp.getEmpIndex()][day+1].size() > 1) {
                for (Shift s : assignedShifts[emp.getEmpIndex()][day+1]) {
                    if (s.getStartTime() == 7) {
                        startTime = s.getStartTime();
                        break;
                    } else {
                        startTime = 19;
                    }
                }
            }
            if (endTime == 7) {
                return elevenRule && (startTime-endTime) >= 11;
            } else {
                return elevenRule && (24 - endTime + startTime) >= 11;
            }
        }
        return true;
    }

    // Successive Shortest Path Algorithm: V3 (with 11-hour breaks between shifts)
    public void shortestPathsRestrictive(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[] minFlows = new int[n];
        int[] shiftCaps = new int[n];

        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
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
                // initialize values for the 11-hour check
                // flow allowed to pass through current path
                int minFlow = Math.min(minFlows[node.getVertexIndex()],e.getCap()-e.getFlow());
                Employee emp = employees[node.getVertexIndex()];
                int day = days[node.getVertexIndex()];
                Shift shift = shifts[node.getVertexIndex()];
                int shiftCap = shiftCaps[node.getVertexIndex()];
                // define the node, which the current edge goes to
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight() && 
                checkConditions11Hr(e, emp, day, shift, minFlow) && sameDep(e, emp, day) && minFlow >= shiftCap) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + e.getWeight();
                    parentEdges[toNode.getVertexIndex()] = e;

                    // for the 11-hour criteria
                    if (toNode.getPurpose() == 1) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                        shiftCaps[toNode.getVertexIndex()] = shiftCap;
                    } else if (toNode.getPurpose() == 2) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = toNode.getDay();
                        shifts[toNode.getVertexIndex()] = shift;
                        shiftCaps[toNode.getVertexIndex()] = shiftCap;
                    } else if (toNode.getPurpose() == 3) {
                        employees[toNode.getVertexIndex()] = toNode.getEmp();
                        days[toNode.getVertexIndex()] = toNode.getDay();
                        shifts[toNode.getVertexIndex()] = toNode.getShift();
                        shiftCaps[toNode.getVertexIndex()] = e.getCap()-e.getFlow();
                        if (e.getType() == 1 && minFlow >= e.getCap()) {
                            if (assignedShifts[emp.getEmpIndex()][day].size() > 1) {
                                for (int i = 0; i < assignedShifts[emp.getEmpIndex()][day].size(); i++) {
                                    if (assignedShifts[emp.getEmpIndex()][day].get(i).equals(toNode.getShift())) {
                                        assignedShifts[emp.getEmpIndex()][day].remove(i);
                                    }
                                }
                            } else if (!assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
                                assignedShifts[emp.getEmpIndex()][day].remove(0);
                            }
                            deps[emp.getEmpIndex()][day] = -1;
                            shiftCaps[toNode.getVertexIndex()] += e.getCap();
                        }
                    } else {
                        employees[toNode.getVertexIndex()] = emp;
                        days[toNode.getVertexIndex()] = day;
                        shifts[toNode.getVertexIndex()] = shift;
                        shiftCaps[toNode.getVertexIndex()] = shiftCap;
                    }

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[toNode.getVertexIndex()] = minFlow;
                    } else {
                        minFlows[toNode.getVertexIndex()] = minFlow;
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);
                    }
                }
            }
        }
    }


    // Breadth-First-Search version 1 (standard)
    public void bfs(Vertex s, Edge[] parentEdges, int[] dist) {
        Queue<Vertex> q = new LinkedList<>();
        boolean[] edgesVisited = new boolean[s.getOutGoing().get(0).getTotalEdges()];

        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            for (Edge e : node.getOutGoing()) {
                int u = e.getFrm().getVertexIndex();
                int v = e.getTo().getVertexIndex();
                if (!edgesVisited[e.getEdgeIndex()] && (e.getCap() - e.getFlow()) > 0 && dist[u] + e.getWeight() < dist[v]) {
                    q.add(e.getTo());
                    dist[v] = dist[u] + e.getWeight();
                    edgesVisited[e.getEdgeIndex()] = true;
                    parentEdges[v] = e;
                }
            }
        }
    }

    public int spfaDyn(int n, Vertex s, int[] dist, Edge[] parentEdges, int[] minFlows, int[] shiftWeights, int[] shiftCaps) {
        // attempt to hinder partial flow assignments using weights
        // this uses the bottleneck flow and the capacity of the shift assignment chosen to (maybe) add extra weight
        int[] relaxationCounts = new int[n];

        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
            shiftCaps[i] = -1;
            shiftWeights[i] = -1;
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
                
                int minFlow = Math.min(minFlows[node.getVertexIndex()], e.getCap()-e.getFlow());
                int weight = e.getWeight();
                if (toNode.getPurpose() == 3 && minFlow < (e.getCap()-e.getFlow())) {
                    weight += 4*weight;
                } else if (shiftCaps[node.getVertexIndex()] != -1 && shiftWeights[node.getVertexIndex()] != -1 && minFlow < (shiftCaps[node.getVertexIndex()]-shiftWeights[node.getVertexIndex()])) {
                    weight += 4*weight;
                }

                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + weight) {
                    dist[toNode.getVertexIndex()] = dist[node.getVertexIndex()] + weight;
                    parentEdges[toNode.getVertexIndex()] = e;

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[toNode.getVertexIndex()] = Math.min(minFlow, e.getCap()-e.getFlow());
                    } else {
                        minFlows[toNode.getVertexIndex()] = Math.min(minFlow, e.getCap());
                    }

                    // updates the weight and capacity for the shift used on the current path
                    if (toNode.getPurpose() == 3) {
                        shiftCaps[toNode.getVertexIndex()] = e.getCap()-e.getFlow();
                        shiftWeights[toNode.getVertexIndex()] = e.getWeight();
                    } else if (toNode.getPurpose() > 3) {
                        shiftCaps[toNode.getVertexIndex()] = shiftCaps[node.getVertexIndex()];
                        shiftWeights[toNode.getVertexIndex()] = shiftWeights[node.getVertexIndex()];
                    }

                    if (!inQ[toNode.getVertexIndex()]) {
                        inQ[toNode.getVertexIndex()] = true;
                        q.add(toNode);

                        relaxationCounts[toNode.getVertexIndex()]++;

                        if (relaxationCounts[toNode.getVertexIndex()] > s.getTotalVertices()) {
                            return toNode.getVertexIndex();
                        }
                    }
                }
            }
        
        }
        return 0;
    }

    public int spfaNegDetectionDyn(int n, Vertex s, int[] dist, Edge[] parentEdges, int[] minFlows, int[] shiftWeights, int[] shiftCaps) {
        int vertexInNegCycle = -1;
        boolean[] inQ = new boolean[n];

        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        while (!q.isEmpty() && vertexInNegCycle == -1) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            for (Edge e : node.getOutGoing()) {
                Vertex toNode = e.getTo();
                int minFlow = Math.min(minFlows[node.getVertexIndex()], e.getCap()-e.getFlow());
                int weight = e.getWeight();
                if (toNode.getPurpose() == 3 && minFlow < (e.getCap()-e.getFlow())) {
                    weight += 4*weight;
                } else if (shiftCaps[node.getVertexIndex()] != -1 && shiftWeights[node.getVertexIndex()] != -1 && minFlow < (shiftCaps[node.getVertexIndex()]-shiftWeights[node.getVertexIndex()])) {
                    weight += 4*weight;
                }

                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + weight) {
                    vertexInNegCycle = e.getTo().getVertexIndex();
                    break;
                }
            }
        
        }
        return vertexInNegCycle;
    }


    // Shortest paths faster algorithm
    public int spfa(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int[] relaxationCounts = new int[n];

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

                        relaxationCounts[toNode.getVertexIndex()]++;

                        if (relaxationCounts[toNode.getVertexIndex()] > s.getTotalVertices()) {
                            return toNode.getVertexIndex();
                        }
                    }

                }
            }
        
        }
        return -1;
    }

    public int spfaNegDetection(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        int vertexInNegCycle = -1;
        boolean[] inQ = new boolean[n];

        Queue<Vertex> q = new LinkedList<>();
        q.add(s);

        while (!q.isEmpty() && vertexInNegCycle == -1) {
            Vertex node = q.remove();
            inQ[node.getVertexIndex()] = false;
            for (Edge e : node.getOutGoing()) {
                Vertex toNode = e.getTo();
                if (e.getCap() - e.getFlow() > 0 && dist[toNode.getVertexIndex()] > dist[node.getVertexIndex()] + e.getWeight()) {
                    vertexInNegCycle = e.getTo().getVertexIndex();
                    break;
                }
            }
        
        }
        return vertexInNegCycle;
    }

    // Bellman-Ford
    public int bellmanFord(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        // relaxation using shortest paths faster algorithm:
        // int[] minFlows = new int[n];
        // int[] shiftCaps = new int[n];
        // int[] shiftWeights = new int[n];
        // return spfaDyn(n, s, dist, parentEdges, minFlows, shiftWeights, shiftCaps);
        return spfa(n, s, dist, parentEdges);
        // shortestPaths11Hr(n, s, dist, parentEdges);
        // if (dist[fg.getT().getVertexIndex()] == Integer.MAX_VALUE) {
        //     return spfa(n, s, dist, parentEdges);
        //     // return spfaDyn(n, s, dist, parentEdges, minFlows, shiftWeights, shiftCaps);
        // }
        // return -1;
        // negative cycle detection using spfa
        // return spfaNegDetectionDyn(n, s, dist, parentEdges, minFlows, shiftWeights, shiftCaps);
        // return spfaNegDetection(n, s, dist, parentEdges);
        // return vertexInNegCycle;
    }

    // A more direct non-optimized bellman ford
    public int bellmanFord2(int n, Vertex s, int[] dist, Edge[] parentEdges) {
        // relaxation using bfs:
        int[] minFlows = new int[n];
        int[] shiftCaps = new int[n];
        int[] shiftWeights = new int[n];

        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdges[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
            shiftCaps[i] = -1;
            shiftWeights[i] = -1;
        }
        Queue<Vertex> q = new LinkedList<>();

        // Initialize distances
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[s.getVertexIndex()] = 0;
        q.add(s);
        boolean[] inQueue = new boolean[n];
        inQueue[s.getVertexIndex()] = true;

        // Repeat the relaxation process for (numVertices - 1) iterations
        for (int i = 0; i < n - 1; i++) {
            int nodesInQueue = q.size();  // Track nodes in the queue for the current pass
            
            for (int j = 0; j < nodesInQueue; j++) {
                Vertex u = q.poll();
                inQueue[u.getVertexIndex()] = false;

                // Traverse all outgoing edges of u
                for (Edge e : u.getOutGoing()) {
                    Vertex v = e.getTo();
                    int uIdx = u.getVertexIndex();
                    int vIdx = v.getVertexIndex();
                    int weight = e.getWeight();

                    int minFlow = Math.min(minFlows[u.getVertexIndex()], e.getCap()-e.getFlow());
                    if (v.getPurpose() == 3 && minFlow < (e.getCap()-e.getFlow())) {
                        weight += 4*weight;
                    } else if (shiftCaps[u.getVertexIndex()] != -1 && shiftWeights[u.getVertexIndex()] != -1 && minFlow < (shiftCaps[u.getVertexIndex()]-shiftWeights[u.getVertexIndex()])) {
                        weight += 4*weight;
                    }
                    // Relax the edge (u, v)
                    if (e.getCap() - e.getFlow() > 0 && dist[uIdx] + weight < dist[vIdx]) {
                        dist[vIdx] = dist[uIdx] + weight;
                        parentEdges[vIdx] = e;
                        
                        // updates the amount of flow the path can take
                        if (e.getType() == 0) {
                            minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap()-e.getFlow());
                        } else {
                            minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap());
                        }

                        // updates the weight and capacity for the shift used on the current path
                        if (v.getPurpose() == 3) {
                            shiftCaps[v.getVertexIndex()] = e.getCap()-e.getFlow();
                            shiftWeights[v.getVertexIndex()] = e.getWeight();
                        } else if (v.getPurpose() > 3) {
                            shiftCaps[v.getVertexIndex()] = shiftCaps[u.getVertexIndex()];
                            shiftWeights[v.getVertexIndex()] = shiftWeights[u.getVertexIndex()];
                        }

                        // Only add v to the queue if it hasn't been processed in this round
                        if (!inQueue[vIdx]) {
                            q.add(v);
                            inQueue[vIdx] = true;
                        }
                    }
                }
            }
        }

        // Negative Cycle Check
        for (Vertex u : q) {
            for (Edge e : u.getOutGoing()) {
                int uIdx = e.getFrm().getVertexIndex();
                int vIdx = e.getTo().getVertexIndex();
                int weight = e.getWeight();

                // If we can still relax, we have a negative cycle
                if (dist[uIdx] + weight < dist[vIdx]) {
                    System.out.println("Negative cycle detected.");
                    return vIdx;
                }
            }
        }

        return -1;  // No negative cycle found
        // negative cycle detection using bfs
        // return vertexInNegCycle;
    }

    // Doubt this will ever work. Still here if pursued tho
    public void elevenHrDijkstra(int n, Vertex s, Vertex t, int[] dist, Edge[] parentEdge) {
        int[] minFlows = new int[n];
        Employee[] employees = new Employee[n];
        int[] days = new int[n];
        Shift[] shifts = new Shift[n];

        for (int i = 0; i < n; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdge[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
        }

        PriorityQueue<Vertex> minHeap = new PriorityQueue<>((v1, v2) -> 
            Integer.compare(dist[v1.getVertexIndex()], dist[v2.getVertexIndex()])
        );

        dist[s.getVertexIndex()] = 0;
        minHeap.add(s);

        while (!minHeap.isEmpty()) {
            Vertex u = minHeap.remove();

            for (Edge e : u.getOutGoing()) {
                Vertex v = e.getTo();
                // if (!addedToQ[v.getVertexIndex()]) {
                //     q.add(v);
                //     addedToQ[v.getVertexIndex()] = true;
                // } 
                // if (!q.contains(v)) continue;
                int minFlow = Math.min(minFlows[u.getVertexIndex()], e.getCap()-e.getFlow());
                Employee emp = employees[u.getVertexIndex()];
                int day = days[u.getVertexIndex()];
                Shift shift = shifts[u.getVertexIndex()];
                
                int reducedCost = dist[u.getVertexIndex()] + e.getWeight() + u.getPotential() - v.getPotential();
                if (reducedCost < dist[v.getVertexIndex()] && e.getCap() - e.getFlow() > 0 && checkConditions11Hr(e, emp, day, shift, minFlow) && sameDep(e, emp, day)) {
                    dist[v.getVertexIndex()] = reducedCost;
                    parentEdge[v.getVertexIndex()] = e;

                    // by placing the add to queue here I avoid adding unreachable/unfavorable vertices
                    minHeap.add(v);

                    // for the 11-hour criteria
                    if (v.getPurpose() == 1) {
                        employees[v.getVertexIndex()] = v.getEmp();
                        days[v.getVertexIndex()] = day;
                        shifts[v.getVertexIndex()] = shift;
                    } else if (v.getPurpose() == 2) {
                        employees[v.getVertexIndex()] = v.getEmp();
                        days[v.getVertexIndex()] = v.getDay();
                        shifts[v.getVertexIndex()] = shift;
                    } else if (v.getPurpose() == 3) {
                        employees[v.getVertexIndex()] = v.getEmp();
                        days[v.getVertexIndex()] = v.getDay();
                        shifts[v.getVertexIndex()] = v.getShift();
                        if (e.getType() == 1 && minFlow >= e.getCap()) {
                            if (assignedShifts[emp.getEmpIndex()][day].size() > 1) {
                                for (int i = 0; i < assignedShifts[emp.getEmpIndex()][day].size(); i++) {
                                    if (assignedShifts[emp.getEmpIndex()][day].get(i).equals(v.getShift())) {
                                        assignedShifts[emp.getEmpIndex()][day].remove(i);
                                    }
                                }
                            } else if (!assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
                                assignedShifts[emp.getEmpIndex()][day].remove(0);
                            }
                            deps[emp.getEmpIndex()][day] = -1;
                        }
                    } else {
                        employees[v.getVertexIndex()] = emp;
                        days[v.getVertexIndex()] = day;
                        shifts[v.getVertexIndex()] = shift;
                    }

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[v.getVertexIndex()] = minFlow;
                    } else {
                        minFlows[v.getVertexIndex()] = minFlow;
                    }

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap()-e.getFlow());
                    } else {
                        minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap());
                    }
                }
                
            }
        }

    }
    public void dijkstraDyn(int n, Vertex s, Vertex t, int[] dist, Edge[] parentEdge) {
        int[] minFlows = new int[n];
        int[] shiftCaps = new int[n];
        // int[] shiftWeights = new int[n];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdge[i] = null;
            minFlows[i] = Integer.MAX_VALUE;
            shiftCaps[i] = -1;
            // shiftWeights[i] = -1;
        }

        PriorityQueue<Vertex> minHeap = new PriorityQueue<>((v1, v2) -> 
            Integer.compare(dist[v1.getVertexIndex()], dist[v2.getVertexIndex()])
        );

        dist[s.getVertexIndex()] = 0;
        minHeap.add(s);

        while (!minHeap.isEmpty()) {
            Vertex u = minHeap.remove();

            for (Edge e : u.getOutGoing()) {
                Vertex v = e.getTo();
                // if (!addedToQ[v.getVertexIndex()]) {
                //     q.add(v);
                //     addedToQ[v.getVertexIndex()] = true;
                // } 
                // if (!q.contains(v)) continue;

                int minFlow = Math.min(minFlows[u.getVertexIndex()], e.getCap()-e.getFlow());
                int reducedCost = dist[u.getVertexIndex()] + e.getWeight() + u.getPotential() - v.getPotential();
                if (v.getPurpose() == 3 && minFlow < (e.getCap()-e.getFlow())) {
                    reducedCost += 10000;
                } else if (shiftCaps[u.getVertexIndex()] != -1 && minFlow < shiftCaps[u.getVertexIndex()]) {
                    reducedCost += 10000;
                }
                
                if (reducedCost < dist[v.getVertexIndex()] && e.getCap() - e.getFlow() > 0) {
                    dist[v.getVertexIndex()] = reducedCost;
                    parentEdge[v.getVertexIndex()] = e;

                    // by placing the add to queue here I avoid adding unreachable/unfavorable vertices
                    minHeap.add(v);

                    // updates the amount of flow the path can take
                    if (e.getType() == 0) {
                        minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap()-e.getFlow());
                    } else {
                        minFlows[v.getVertexIndex()] = Math.min(minFlow, e.getCap());
                    }

                    // updates the weight and capacity for the shift used on the current path
                    if (v.getPurpose() == 3) {
                        shiftCaps[v.getVertexIndex()] = e.getCap()-e.getFlow();
                        // shiftWeights[v.getVertexIndex()] = reducedCost;
                    } else if (v.getPurpose() > 3) {
                        shiftCaps[v.getVertexIndex()] = shiftCaps[u.getVertexIndex()];
                        // shiftWeights[v.getVertexIndex()] = shiftWeights[u.getVertexIndex()];
                    }

                }
                
            }
        }

    }

    public void dijkstra (int n, Vertex s, Vertex t, int[] dist, Edge[] parentEdge) {
        for (int i = 0; i < n; i++) {
            dist[i] = Integer.MAX_VALUE;
            parentEdge[i] = null;
        }

        
        // boolean[] addedToQ = new boolean[n];
        PriorityQueue<Vertex> minHeap = new PriorityQueue<>((v1, v2) -> 
            Integer.compare(dist[v1.getVertexIndex()], dist[v2.getVertexIndex()])
        );

        dist[s.getVertexIndex()] = 0;
        minHeap.add(s);

        while (!minHeap.isEmpty()) {
            Vertex u = minHeap.remove();

            for (Edge e : u.getOutGoing()) {
                Vertex v = e.getTo();
                
                int reducedCost = dist[u.getVertexIndex()] + e.getWeight() + u.getPotential() - v.getPotential();
                // if (v.toString().equals("( 15-23 )") && v.getEmp().getName().equals("Employee W") && v.getDay() == 28) {
                //     System.out.println("Reduced cost found to be: " + reducedCost + ", found from edge " + e);
                // }
                if (reducedCost < dist[v.getVertexIndex()] && e.getCap() - e.getFlow() > 0) {
                    if (reducedCost < 0) {
                        System.out.println("edge " + e + " has led to negative reducedCost: " + reducedCost);
                        System.out.println("calculation is: " + dist[u.getVertexIndex()] + "+" + e.getWeight() + "+" + u.getPotential() + "-" + v.getPotential());
                        stop = true;
                        // return;
                    }
                    if (parentEdge[v.getVertexIndex()] != null && parentEdge[v.getVertexIndex()].getFrm() == s) System.out.println("overwriting s: " + parentEdge[v.getVertexIndex()] + " with dist: " + dist[v.getVertexIndex()] + " with edge: " + e + " and new dist: " + reducedCost);
                    dist[v.getVertexIndex()] = reducedCost;
                    parentEdge[v.getVertexIndex()] = e;

                    // by placing the add to queue here I avoid adding unreachable/unfavorable vertices
                    // if (!addedToQ[v.getVertexIndex()]) {
                    // minHeap.remove(v);
                    minHeap.add(v);
                        // addedToQ[v.getVertexIndex()] = true;
                    // }
                }
                
            }
        }

    }

    // TODO: Optimizing this step could lead to far better runtimes
    public Vertex minDistInQ(Queue<Vertex> q, int[] dist) {
        int minDist = Integer.MAX_VALUE;
        Vertex minDistNode = null;
        for (Vertex v : q) {
            if (dist[v.getVertexIndex()] < minDist) {
                minDist = dist[v.getVertexIndex()];
                minDistNode = v;
            }
            if (minDist <= 0) return minDistNode;
        }
        return minDistNode;
    }

    public int[] successiveShortestPaths(int n, int b, Vertex s, Vertex t) {
        int totalCost = 0;
        int totalFlow = b;
        int[] dist = new int[n];
        Edge[] parentEdges = new Edge[n];

        boolean switchedAlg = false;

        while (b > 0) {

            if (!switchedAlg) shortestPaths11Hr(n, s, dist, parentEdges);
            if (switchedAlg || dist[t.getVertexIndex()] == Integer.MAX_VALUE) {
                if (!switchedAlg) {
                    switchedAlg = true;
                    System.out.println("switched to BF at " + (totalFlow-b) + " flow");
                }
                int vertexInNegCycle = bellmanFord(n, s, dist, parentEdges);
                if (vertexInNegCycle != -1 ) System.out.println("Neg cycle found at " + (totalFlow-b) + " flow");
                b -= augment(s, t, parentEdges, vertexInNegCycle);
            }
            if (!switchedAlg) b -= augment(s, t, parentEdges, -1);
            

            this.assignedShifts = new ArrayList[fg.getEmps().length][fg.getDaysInPeriod()];
            for (int emp = 0; emp < this.assignedShifts.length; emp++) {
                for (int day = 0; day < this.assignedShifts[0].length; day++) {
                    this.assignedShifts[emp][day] = new ArrayList<>();
                }
            }
            markAssignedShifts(s, t, new boolean[t.getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        }
        if (b != 0) totalFlow -= b;
        
        return new int[] {totalFlow, totalCost};
    }

    public int[] fasterSuccessiveShortestPaths(int n, int b, Vertex s, Vertex t) {
        totalCost = 0;
        int totalFlow = b;
        int[] dist = new int[n];
        Edge[] parentEdges = new Edge[n];

        ArrayList<Vertex> allVertices = new ArrayList<>();
        Queue<Vertex> q = new LinkedList<>();
        boolean[] nodesVisited = new boolean[s.getTotalVertices()];

        q.add(s);
        nodesVisited[s.getVertexIndex()] = true;

        // store all vertices in a list
        while (!q.isEmpty()) {
            Vertex node = q.remove();
            allVertices.add(node);

            for (Edge e : node.getOutGoing()) {
                int v = e.getTo().getVertexIndex();
                if (!nodesVisited[v]) {
                    nodesVisited[v] = true;
                    q.add(e.getTo());
                }
            }
        }

        while (b > 0) {
            
            // run shortest paths
            dijkstra(n, s, t, dist, parentEdges);
            if (dist[t.getVertexIndex()] == Integer.MAX_VALUE || -dist[t.getVertexIndex()] == Integer.MAX_VALUE) break;

            // update vertex potentials
            for (int i = 0; i < allVertices.size(); i++) {
                Vertex node = allVertices.get(i);
                node.setPotential(dist[node.getVertexIndex()]);
            }
            
            // augment flow
            b -= augment(s, t, parentEdges, -1);
            
            // int val = augment(s, t, parentEdges, -1);
            // if (stop) break;
            // if (val != -100) {
            //     b -= val;
            // } else {
            //     break;
            // }
            // System.out.println("Flow is at: " + (totalFlow-b));
        }
        totalFlow -= b;

        markAssignedShifts(s, t, new boolean[t.getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        int[] results = {totalFlow, totalCost, prefsDenied, prefsFulfilled};
        return results;
    }

    public int augment(Vertex s, Vertex t, Edge[] parentEdges, int vertexInNegCycle) {
        int minFlow = Integer.MAX_VALUE;
        
        if (vertexInNegCycle == -1) {

            Vertex node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                minFlow = Math.min(minFlow, edge.getCap()-edge.getFlow());
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                // add weight to total
                if (Math.abs(edge.getWeight()) >= 0 && Math.abs(edge.getWeight()) < fg.getBaseEdgeWeight()) {
                    totalCost -= (1000 - edge.getWeight()); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    prefsFulfilled++;
                } else if (edge.getWeight() != fg.getBaseEdgeWeight() && edge.getWeight() != -fg.getBaseEdgeWeight()) {
                    totalCost += (edge.getWeight()-fg.getBaseEdgeWeight());
                    prefsDenied++;
                }

                edge.addFlow(minFlow);
                // if (stop) System.out.println("added flow to " + edge);
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            return minFlow;
        } else {
            ArrayList<Edge> path = new ArrayList<>();
            Edge edge = parentEdges[vertexInNegCycle];
            while (!path.contains(edge)) {
                path.add(edge);
                edge = parentEdges[edge.getFrm().getVertexIndex()];
            }

            for (Edge e : path) {
                minFlow = Math.min(minFlow, e.getCap()-e.getFlow());
            }

            System.out.println("neg cycle");
            for (Edge e : path) {
                e.addFlow(minFlow);
                // System.out.println(e);
            }
            return 0;
        }

    }

    public int[] minCostFlow(int n, int k, Vertex s, Vertex t) {
        int totalFlow = 0;
        totalCost = 0;
        prefsDenied = 0;
        prefsFulfilled = 0;
        int[] dist = new int[n];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
        }

        Edge[] parentEdges = new Edge[n];

        while (totalFlow < k) {
            // shortestPathsV1(n, s, dist, parentEdges);
            shortestPaths11Hr(n, s, dist, parentEdges); 
            // if (dist[t.getVertexIndex()] == Integer.MAX_VALUE) {
            //     for (int i = 0; i < dist.length; i++) {
            //         dist[i] = Integer.MAX_VALUE;
            //     }
            //     parentEdges = new Edge[n];
            //     shortestPathsV1(n, s, dist, parentEdges); // Changed search which can backtrack
            // }
            if (dist[t.getVertexIndex()] == Integer.MAX_VALUE) break; 
            int bottleFlow = Integer.MAX_VALUE;
            Vertex node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                bottleFlow = Math.min(bottleFlow, edge.getCap()-edge.getFlow());
                node = parentEdges[node.getVertexIndex()].getFrm();
            }

            totalFlow += bottleFlow;
            // totalCost += dist[t.getVertexIndex()];

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                // The following only adds cost from edges which aren't "basic"
                if (Math.abs(edge.getWeight()) >= 0 && Math.abs(edge.getWeight()) < fg.getBaseEdgeWeight()) {
                    totalCost -= (1000 - edge.getWeight()); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    prefsFulfilled++;
                } else if (edge.getWeight() != fg.getBaseEdgeWeight() && edge.getWeight() != -fg.getBaseEdgeWeight()) {
                    totalCost += (edge.getWeight()-fg.getBaseEdgeWeight());
                    prefsDenied++;
                }
                edge.addFlow(bottleFlow);
                node = parentEdges[node.getVertexIndex()].getFrm();
            }
            // System.out.println("currently at " + ( totalFlow/((double) k)*100.0) + "%");
            this.assignedShifts = new ArrayList[fg.getEmps().length][fg.getDaysInPeriod()];
            for (int emp = 0; emp < this.assignedShifts.length; emp++) {
                for (int day = 0; day < this.assignedShifts[0].length; day++) {
                    this.assignedShifts[emp][day] = new ArrayList<>();
                }
            }
            // TODO: Find alternative... only necessary for tests
            // this.assignedShifts = new ArrayList[fg.getEmps()[0].getTotalEmployees()][fg.getDaysInPeriod()];
            // for (int emp = 0; emp < this.assignedShifts.length; emp++) {
            //     for (int day = 0; day < this.assignedShifts[0].length; day++) {
            //         this.assignedShifts[emp][day] = new ArrayList<>();
            //         this.deps[emp][day] = -1;
            //     }
            // }
            // System.out.println("Flow: " + totalFlow);
            markAssignedShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        }
        int[] results = {totalFlow, totalCost, prefsDenied, prefsFulfilled};
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
            int dep = -1;
            for (Vertex node : path) {
                if (node.getPurpose() == 1) {
                    emp = node.getEmp();
                } else if (node.getPurpose() == 2) {
                    day = node.getDay();
                } else if (node.getPurpose() == 3) {
                    shift = node.getShift();
                } else if (node.getPurpose() == 4) {
                    dep = node.getDep();
                }
            }
            // if (emp.getID().equals("BOTH27") && day == 20 && employeeShifts[emp.getEmpIndex()][day] == null) {
            //     System.out.println("Day " + (day-1) + ": " + employeeShifts[emp.getEmpIndex()][day-1]);
            //     System.out.println("Day " + (day) + ": " + employeeShifts[emp.getEmpIndex()][day]);
            //     System.out.println("Day " + (day+1) + ": " + employeeShifts[emp.getEmpIndex()][day+1]);
            // }
            employeeShifts[emp.getEmpIndex()][day] = shift;
            deps[emp.getEmpIndex()][day] = dep;
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    // if (v.getPurpose() == 2 && flow != e.getFlow()) continue;
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

    public void markAssignedShifts(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap) {
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
            int dep = -1;
            for (int i = 0; i < path.size(); i++) {
                Vertex node = path.get(i);
                if (node.getPurpose() == 3) {
                    emp = node.getEmp();
                    day = node.getDay();
                    shift = node.getShift();
                } else if (node.getPurpose() == 4) {
                    dep = node.getDep();
                }
            }
            // if (emp.getName().equals("Employee 05") && day == 05 && assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
            //     System.out.println("Day " + (day-1) + ": " + assignedShifts[emp.getEmpIndex()][day-1]);
            //     System.out.println("Day " + (day) + ": " + assignedShifts[emp.getEmpIndex()][day]);
            //     System.out.println("Day " + (day+1) + ": " + assignedShifts[emp.getEmpIndex()][day+1]);
            // }
            // if (!assignedShifts[emp.getEmpIndex()][day].contains(shift)) assignedShifts[emp.getEmpIndex()][day].add(shift);
            assignedShifts[emp.getEmpIndex()][day].add(shift);
            deps[emp.getEmpIndex()][day] = dep;
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    // if (v.getPurpose() == 2 || v.getPurpose() == 3 && flow != e.getFlow()) continue;
                    markAssignedShifts(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap());
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

    public List<Shift>[][] getAssignedShifts() {
        return assignedShifts;
    }




}
