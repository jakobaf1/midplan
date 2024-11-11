package app.program.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Algorithms {
    private FlowGraph fg;
    private Shift[][] employeeShifts;
    private int[][] deps;
    private List<Shift>[][] assignedShifts;

    public Algorithms(FlowGraph fg) {
        this.fg = fg;
        this.employeeShifts = new Shift[fg.getEmps().length][fg.getDaysInPeriod()];
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

    // Bellman-Ford
    public void bellmanFord(FlowGraph fg, Vertex s) {
        int[] dist = new int[s.getTotalVertices()];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
        }
        dist[s.getVertexIndex()] = 0;
        
    }

    // Breadth-First-Search version 1 (standard)
    public boolean bfs(Vertex s, Vertex t, Edge[] parentEdges, boolean[] nodesVisited) {
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

        while (bfs(s, t, parentEdges, nodesVisited)) {
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

            // flow allowed to pass through current path
            int minFlow = minFlows[node.getVertexIndex()];
            
            for (Edge e : node.getOutGoing()) {
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
                        if (e.getType() == 1 && minFlow >= e.getCap()) {
                            if (assignedShifts[emp.getEmpIndex()][day].size() > 1) {
                                for (int i = 0; i < assignedShifts[emp.getEmpIndex()][day].size(); i++) {
                                    if (assignedShifts[emp.getEmpIndex()][day].get(i).equals(node.getShift())) assignedShifts[emp.getEmpIndex()][day].remove(i);
                                }
                            } else if (!assignedShifts[emp.getEmpIndex()][day].isEmpty()) {
                                assignedShifts[emp.getEmpIndex()][day].remove(0);
                            }
                            // employeeShifts[emp.getEmpIndex()][day] = null;
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
                // TODO: make this look nicer (and make sure it's correct)
                // The min-flow condition makes sure that a new shift can't be chosen unless it will form an accurate shift (e.g. 8/8 in 15-23 and 4/8 in 23-7 is not valid)
                for (Shift s : fg.getShifts()) {
                    if (currentStart == shift.getEndTime()) { // cases where new shift is before old shift (new shift can be 7-15 or 15-23)
                        if (shift.getStartTime() == s.getStartTime() && currentEnd-4 == s.getEndTime() && minFlow == 8) { // When new shift is 7-15 and old shift is 15-23 (resulting in a 7-19 shift)
                            validShift = true;
                            break;
                        } else if (shift.getStartTime()+4 == s.getStartTime() && currentEnd == s.getEndTime()) { // When new shift is 15-23 and old is 23-7 (resulting in a 19-7 shift)
                            validShift = true;
                            break;
                        }
                        
                    } else if (currentEnd == shift.getStartTime()) { // cases where new shift is after old shift (new shift can be 15-23 or 23-7)
                        if (currentStart == s.getStartTime() && shift.getEndTime()-4 == s.getEndTime()) { // When new shift is 15-23 and old is 7-15 (resulting in 7-19)
                            validShift = true;
                            break;
                        } else if (currentStart+4 == s.getStartTime() && shift.getEndTime() == s.getEndTime()  && minFlow == 8) { // When new shift is 23-7 and old os 15-23 (resulting in a 19-7)
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

    public int[] minCostFlow(int n, int k, Vertex s, Vertex t) {
        int totalFlow = 0;
        int totalCost = 0;
        int prefsDenied = 0;
        int prefsFulfilled = 0;
        int[] dist = new int[n];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
        }

        Edge[] parentEdges = new Edge[n];

        while (totalFlow < k) {
            shortestPaths11Hr(n, s, dist, parentEdges); 
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
            // if (dist[t.getVertexIndex()] > 6000) prefsDenied++;
            // if (dist[t.getVertexIndex()] < 6000) prefsFulfilled++;

            node = t;
            while (node != s) {
                Edge edge = parentEdges[node.getVertexIndex()];
                // The following only adds cost from edges which aren't "basic"
                if (Math.abs(edge.getWeight()) >= 0 && Math.abs(edge.getWeight()) < fg.getBaseEdgeWeight()) {
                    totalCost -= (1000 - edge.getWeight()); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    prefsFulfilled++;
                } else if (edge.getWeight() != fg.getBaseEdgeWeight() && edge.getWeight() != -fg.getBaseEdgeWeight()) {
                    totalCost += edge.getWeight();
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
            
            assignedShifts[emp.getEmpIndex()][day].add(shift);
            deps[emp.getEmpIndex()][day] = dep;
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
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
