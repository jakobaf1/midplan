package app.program.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

public class FlowGraph {
    
    private int periodInWeeks = 8;
    private int daysInPeriod = periodInWeeks*7;
    private Vertex s = new Vertex(0, "s");
    private Vertex t = new Vertex(6, "t");
    private LocalDate startDate;
    private int baseEdgeWeight = 1000;

    private Shift[] shifts;
    private Employee[] emps;

    public FlowGraph(int periodInWeeks, Shift[] shifts, Employee[] emps) {
        this.periodInWeeks = periodInWeeks;
        this.shifts = shifts;
        this.emps = emps;
        this.daysInPeriod = periodInWeeks*7;
    }
    
    public void generateGraph(LocalDate startDate) {
        // Initialize the date for which the roster begins
        LocalDate date = startDate;
        this.startDate = startDate;
        // Used to bind edges
        Vertex[][] sharedNodes = new Vertex[daysInPeriod][2*3*2]; // 2 is the amount of departments, 3 times of day and 2 exp levels in the model

        for (int day = 0; day < daysInPeriod; day++) {

            for (int dep = 0; dep < 2; dep++) {
                for (int time = 0; time < 3; time++) {
                    // this node signifies the dep_time node (6th layer)
                    Vertex layer5Node = new Vertex(5, (dep +"_"+time+"_"+day), dep, time);
                    // here it is linked to the sink
                    if (time == 0) {
                        addEdge(layer5Node, t, 6*8, baseEdgeWeight, 0);
                    } else {
                        addEdge(layer5Node, t, 4*8, baseEdgeWeight, 0);
                    }

                    for (int exp = 0; exp < 2; exp++) {
                        Vertex layer4Node = new Vertex(4, time+"_"+dep+"_"+(exp+1), dep, time, exp+1);
                        if (time == 0) {
                            addEdge(layer4Node, layer5Node, (6-exp)*8, baseEdgeWeight, 0);
                        } else {
                            addEdge(layer4Node, layer5Node, (4-exp)*8, baseEdgeWeight, 0);
                        }
                        sharedNodes[day][(dep*6)+(time*2)+(exp)] = layer4Node;
                    }
                }
            }
        }

        // make employee nodes and connect edges
        for (Employee e : emps) {

            date = startDate;

            // creation and linking of employe enode to source
            Vertex empNode = new Vertex(1, e.getName(), e);
            addEdge(s, empNode, periodInWeeks*e.getWeeklyHrs(), baseEdgeWeight, 0);

            // I define the array containing the preferences of the employee
            List<Preference>[] dayPreferences = prefDays(e, date, daysInPeriod);
            List<Preference>[] shiftPreferences = prefShifts(e, date, daysInPeriod);
        
            // now I iterate through the days to make the relevant day nodes for the employee
            for (int day = 0; day < daysInPeriod; day++) {
                int weekday = date.getDayOfWeek().getValue();
                // now I check whether the employee has any preferences for shifts on this day
                boolean shiftPref = !shiftPreferences[day].isEmpty();
                int w = setDayWeight(dayPreferences[day]);
                //  if the employee is required to not work this day, it is skipped and no edge is drawn
                if (w == Integer.MAX_VALUE && !shiftPref) {
                    date = date.plusDays(1); 
                    continue;
                }
                Vertex dayNode = new Vertex(2, weekdayToString(weekday)+"_"+day, day);
                if (!shiftPref) {
                    addEdge(empNode, dayNode, 12, w, 8);
                } else {
                    addEdge(empNode, dayNode, 12, baseEdgeWeight, 8);
                }

                for (Shift shift : shifts) {
                    if (shift.calcHours() > 8) continue;
                    int ws = baseEdgeWeight;
                    if (shiftPref) {
                        ws = setShiftWeight(shiftPreferences[day], shift);
                        if (ws == Integer.MAX_VALUE) continue;
                    }
                    
                    Vertex shiftNode = new Vertex(3, shift.getStartTime() + "-" + shift.getEndTime(), shift);
                    addEdge(dayNode, shiftNode, shift.calcHours(), ws, 0);

                    int expLvl = e.getExpLvl();
                    // switch case to connect the correct the right nodes
                    switch (shift.getStartTime()) {
                        // case 0:
                        //     for (int dep : e.getDepartments()) {
                        //         addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)], 8, baseEdgeWeight, 8);
                        //         addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+2], 8, baseEdgeWeight, 8);
                        //         addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+4], 8, baseEdgeWeight, 8);
                        //     }
                        //     break;
                        case 7:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)], 8, baseEdgeWeight, 8);
                            }
                            break;
                        case 15:
                            for (int dep : e.getDepartments()) {
                                if (shift.calcHours() == 8) {
                                    addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+2], 8, baseEdgeWeight, 8);
                                } else {
                                    addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+2], 4, baseEdgeWeight, 4);
                                }
                            }
                            break;
                        case 19:
                            for (int dep : e.getDepartments()) {
                                // addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+4], 8, baseEdgeWeight, 8);
                                addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+2], 4, baseEdgeWeight, 4);
                            }
                            break;
                        case 23:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+(expLvl-1)+4], 8, baseEdgeWeight, 8);
                            }
                            break;
                        default:
                            break;
                    }
                }
                date = date.plusDays(1);
            }

        
        
        
        }


    }


    // method for creating new edges
    public void addEdge(Vertex frm, Vertex to, int cap, int weight, int lowerBound) {
        Edge newEdge = new Edge(0, frm, to, cap, weight, lowerBound);
        frm.addOutGoing(newEdge);
        to.addInGoing(newEdge);

        // Now to create the backwards edge
        Edge counterEdge = new Edge(1, to, frm, 0, newEdge, -weight, lowerBound);
        to.addOutGoing(counterEdge);
        frm.addInGoing(counterEdge);
        newEdge.setCounterpart(counterEdge);

    }

    // for storing the preferences of an employee based on day
    public List<Preference>[] prefDays(Employee e, LocalDate date, int days) {
        List<Preference>[] dayPreferences = new ArrayList[daysInPeriod];
        for (int i = 0; i < dayPreferences.length; i++) {
            dayPreferences[i] = new ArrayList<>();
        }

        if (e.getPref() == null) return dayPreferences;
        for (Preference p : e.getPref()) {
            if (p.getDay() != -1) {
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int firstDay = firstDay(date, p);

                if (p.getRepeat() != -1) {
                    int startingWeek = 0;
                    switch (p.getRepeat()) {
                        case 1: // weekly
                            for (int i = firstDay; i < days; i += 7) {
                                dayPreferences[i].add(p);
                            }
                            break;
                        case 2: // odd weeks
                            date = date.plusDays(firstDay);
                            startingWeek = week%2 == 1 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                dayPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 3: // even weeks
                            date = date.plusDays(firstDay);
                            startingWeek = week%2 == 0 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                dayPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 4: // tri-weekly
                            // TODO
                            break;
                        case 5: // monthly
                            for (int i = firstDay; i < days; i += 28) {
                                dayPreferences[i].add(p);
                            }
                            break;
                        default:
                            break;
                    }
                }    
            }

            if (p.getDate() != null) {
                int dayIndex = 0;
                for (int i = 0; i < days; i++) {
                    if (p.getDate() == date) {
                        dayPreferences[dayIndex].add(p);
                        date = date.minusDays(dayIndex);
                        break;
                    }
                    date = date.plusDays(1);
                    dayIndex++;
                }
            }
        }

        return dayPreferences;
    }

    // for storing the preferences of an employee based on shift
    public List<Preference>[] prefShifts(Employee e, LocalDate date, int days) {
        List<Preference>[] shiftPreferences = new ArrayList[days];
        for (int i = 0; i < shiftPreferences.length; i++) {
            shiftPreferences[i] = new ArrayList<>();
        }

        if (e.getPref() == null) return shiftPreferences; 
        for (Preference p : e.getPref()) {
            if (p.getShift() != null) {
                if (p.getDay() != -1 && p.getDate() == null) {
                    int firstDay = firstDay(date, p);
                    for (int i = firstDay; i < days; i += 7) {
                        shiftPreferences[i].add(p);
                    }  
                } else if (p.getDate() == null) {
                    for (int i = 0; i < days; i ++) {
                        shiftPreferences[i].add(p);
                    }  
                } else {
                    int dayIndex = 0;
                    for (int i = 0; i < days; i++) {
                        if (p.getDate() == date) {
                            shiftPreferences[dayIndex].add(p);
                            date = date.minusDays(dayIndex);
                            break;
                        }
                        date = date.plusDays(1);
                        dayIndex++;
                    }
                }
            } 
        }

        return shiftPreferences;
    }

    public int firstDay(LocalDate date, Preference p) {
        int firstDay = 0;
        int weekDay = date.getDayOfWeek().getValue();

        // find out what day of the week the schedule starts
        if (p.getDay() == weekDay) {
            firstDay = 0;
        } else if (p.getDay() < weekDay) {
            firstDay = p.getDay()+7-weekDay;
        } else {
            firstDay = p.getDay()-weekDay;
        }
        return firstDay;
    }
        
    public int setDayWeight(List<Preference> dailyPref) {
        for (Preference p : dailyPref) {
            if (p.getDay() != -1) {
                return findWeight(p);
            }
        }
        return baseEdgeWeight;
    }

    public int setShiftWeight(List<Preference> dailyPref, Shift shift) {
        for (Preference p : dailyPref) {
            if (shift.equals(p.getShift())) {
                return findWeight(p);
            }
        }
        return baseEdgeWeight;
    }

    public int findWeight(Preference p) {
        // switch (p.getPrefLvl()) {
        //     case 1:
        //         return Integer.MAX_VALUE;
        //     case 2:
        //         return 1000;
        //     case 3:
        //         return 250;
        //     case 4:
        //         return 50;
        //     case 5:
        //         return 5;
        //     default:
        //         return baseEdgeWeight;
        // }
        switch (p.getPrefLvl()) {
            case 1:
                return p.getWanted() ? 0 : Integer.MAX_VALUE;
            case 2:
                return p.getWanted() ? 100 : 1900;
            case 3:
                return p.getWanted() ? 500 : 1500;
            case 4:
                return p.getWanted() ? 850 : 1150;
            case 5:
                return p.getWanted() ? 950 : 1050;
            default:
                return baseEdgeWeight;
        }
    }

    public String weekdayToString(int day) {
        switch (day) {
            case 1:
                return "monday";
            case 2:
                return "tuesday";
            case 3:
                return "wednesday";
            case 4:
                return "thursday";
            case 5:
                return "friday";
            case 6:
                return "saturday";
            case 7:
                return "sunday";
            case 8:
                return "monday";
            default:
                return "__";
        }    
    }

    public Vertex[] makeExperimentalGraph() {
        Vertex s = new Vertex(0, "source");
        Vertex t = new Vertex(6, "sink");
        // employee nodes
        Vertex e1 = new Vertex(1, "E1", new Employee("E1", "E1", null, 16, 0, null));
        Vertex e2 = new Vertex(1, "E2", new Employee("E2", "E2", null, 16, 0, null));
        addEdge(s, e1, e1.getEmp().getWeeklyHrs(), 0, 8);
        // addEdge(s, e2, e2.getEmp().getWeeklyHrs(), 0, 8);
        // day nodes emp1
        Vertex d1e1 = new Vertex(2, "day1", 1);
        Vertex d2e1 = new Vertex(2, "day2", 2);
        addEdge(e1, d1e1, 12, 0, 0);
        addEdge(e1, d2e1, 12, 0, 0);
        // day nodes emp2
        Vertex d1e2 = new Vertex(2, "day1", 1);
        Vertex d2e2 = new Vertex(2, "day2", 2);
        addEdge(e2, d1e2, 12, 0, 0);
        addEdge(e2, d2e2, 12, 0, 0);

        // shift nodes
        // day 1, emp 1
        Vertex s1 = new Vertex(3, "7-19", new Shift(7, 19));
        Vertex s2 = new Vertex(3, "7-15", new Shift(7, 15));
        Vertex s3 = new Vertex(3, "15-23", new Shift(15, 23));
        addEdge(d1e1, s1, s1.getShift().calcHours(), 0, 8);
        addEdge(d1e1, s2, s2.getShift().calcHours(), 0, 8);
        addEdge(d1e1, s3, s3.getShift().calcHours(), 0, 8);
        // day 2, emp 1
        Vertex s12 = new Vertex(3, "7-19", new Shift(7, 19));
        Vertex s22 = new Vertex(3, "7-15", new Shift(7, 15));
        Vertex s32 = new Vertex(3, "15-23", new Shift(15, 23));
        addEdge(d2e1, s12, s12.getShift().calcHours(), 0, 8);
        addEdge(d2e1, s22, s22.getShift().calcHours(), 0, 8);
        addEdge(d2e1, s32, s32.getShift().calcHours(), 0, 8);
        
        // day 1, emp 2
        Vertex s4 = new Vertex(3, "7-19", new Shift(7, 19));
        Vertex s5 = new Vertex(3, "7-15", new Shift(7, 15));
        Vertex s6 = new Vertex(3, "15-23", new Shift(15, 23));
        addEdge(d1e2, s4, s1.getShift().calcHours(), 0, 8);
        addEdge(d1e2, s5, s2.getShift().calcHours(), 0, 8);
        addEdge(d1e2, s6, s3.getShift().calcHours(), 0, 8);

        // day 2, emp 2
        Vertex s42 = new Vertex(3, "7-19", new Shift(7, 19));
        Vertex s52 = new Vertex(3, "7-15", new Shift(7, 15));
        Vertex s62 = new Vertex(3, "15-23", new Shift(15, 23));
        addEdge(d2e2, s42, s42.getShift().calcHours(), 0, 8);
        addEdge(d2e2, s52, s52.getShift().calcHours(), 0, 8);
        addEdge(d2e2, s62, s62.getShift().calcHours(), 0, 8);

        // time of day nodes emp 1
        Vertex d1t1 = new Vertex(5, "0_0", 0, 0);
        Vertex d1t2 = new Vertex(5, "0_1", 0, 1);
        Vertex d2t1 = new Vertex(5, "0_0", 0, 0);
        Vertex d2t2 = new Vertex(5, "0_1", 0, 1);
        addEdge(s1, d1t1, 8, 0, 8);
        addEdge(s1, d1t2, 4, 0, 4);
        addEdge(s2, d1t1, 8, 0, 8);
        addEdge(s3, d1t2, 8, 0, 8);
        addEdge(s12, d2t1, 8, 0, 8);
        addEdge(s12, d2t2, 4, 0, 4);
        addEdge(s22, d2t1, 8, 0, 8);
        addEdge(s32, d2t2, 8, 0, 8);

        // time of day nodes emp 2
        Vertex d1t3 = new Vertex(5, "0_0", 0, 0);
        Vertex d1t4 = new Vertex(5, "0_1", 0, 1);
        Vertex d2t3 = new Vertex(5, "0_0", 0, 0);
        Vertex d2t4 = new Vertex(5, "0_1", 0, 1);
        addEdge(s4, d1t3, 8, 0, 8);
        addEdge(s4, d1t4, 4, 0, 4);
        addEdge(s5, d1t3, 8, 0, 8);
        addEdge(s6, d1t4, 8, 0, 8);
        addEdge(s42, d2t3, 8, 0, 8);
        addEdge(s42, d2t4, 4, 0, 4);
        addEdge(s52, d2t3, 8, 0, 8);
        addEdge(s62, d2t4, 8, 0, 8);

        // link time of day to sink
        addEdge(d1t1, t, 12, 0, 0);
        addEdge(d1t2, t, 12, 0, 0);
        addEdge(d1t3, t, 12, 0, 0);
        addEdge(d1t4, t, 12, 0, 0);
        addEdge(d2t1, t, 12, 0, 0);
        addEdge(d2t2, t, 12, 0, 0);
        addEdge(d2t3, t, 12, 0, 0);
        addEdge(d2t4, t, 12, 0, 0);

        Vertex[] v = {s,t};
        return v;
    }

    // printing statements
    public void printGraphSink() {
        String text = "\t";
        for (Edge e : t.getInGoing()) {
            text += e.toString();
            text += "\n\t";
        }
        System.out.println(text);
    }

    public void printPathsWithFlow(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }

        if (v == t) {
            String edge = "";
            for (int i = 0; i < path.size(); i++) {
                if (i == path.size()-1) {
                    edge += path.get(i);
                } else {
                    edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + " -> ";
                }
            }
            System.out.println(edge);
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    printPathsWithFlow(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap());
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

    public void printRuleViolations() {
        // getAssignedShifts(s, t, new boolean[s.getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        for (int i = 0; i < emps.length; i++) {
            // System.out.println("Employee " + emps[i].getID() + ":");
            for (int j = 1; j < emps[i].getShifts().length; j++) {
                if (!emps[i].getShifts()[j].isEmpty() && !emps[i].getShifts()[j-1].isEmpty()) {
                    int endTime = emps[i].getShifts()[j-1].get(0).getEndTime();
                    int startTime = emps[i].getShifts()[j].get(0).getStartTime();
                    if (endTime == 7 && startTime-endTime < 11) {
                        System.out.println("Emp: " + emps[i] + ", Is in violation of shift: " + emps[i].getShifts()[j-1].get(0) + " on day " + (j-1) + ", Shift: " + emps[i].getShifts()[j].get(0) + ", on day: " + j);
                    } else if (24-endTime+startTime < 11) {
                        System.out.println("Emp: " + emps[i] + ", Is in violation of shift: " + emps[i].getShifts()[j-1].get(0) + " on day " + (j-1) + ", Shift: " + emps[i].getShifts()[j].get(0) + ", on day: " + j);
                    }
                }
            }

        }

    }

    public void getPathsWithFlow(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap, ArrayList<String> flowPath) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }

        if (v == t) {
            String edge = "";
            for (int i = 0; i < path.size(); i++) {
                if (i == path.size()-1) {
                    edge += path.get(i);
                } else {
                    edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + " -> ";
                }
            }
            flowPath.add(edge);
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    getPathsWithFlow(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap(), flowPath);
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

    public void getRuleBreakingShift(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap, ArrayList<String> flowPath) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }
        
        if (v == t) {
            String edge = "";
            boolean breaksRule = false;
            Edge empToShiftEdge = null;
            for (int i = 0; i < path.size(); i++) {
                if (i == path.size()-1) {
                    edge += path.get(i);
                } else {
                    edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + " -> ";
                }
                if (path.get(i).getPurpose() == 1) {
                    empToShiftEdge = path.get(i).getOutGoing().get(0);
                    for (Edge e : path.get(i).getOutGoing()) {
                        if (e.getTo() == path.get(i+1)) empToShiftEdge = e;
                    }
                    breaksRule = empToShiftEdge.getFlow() == 4;
                }
            }
            if (breaksRule) flowPath.add(edge);   
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    getRuleBreakingShift(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap(), flowPath);
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

    public void getRuleBreakingDep(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap, ArrayList<String> flowPath) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }
        
        if (v == t) {
            String edge = "";
            if (path.get(4).getDep() != path.get(5).getDep()) {
                for (int i = 0; i < path.size(); i++) {
                    if (i == path.size()-1) {
                        edge += path.get(i);
                    } else {
                        edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + " -> ";
                    }
                }
                flowPath.add(edge);
            }
               
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    getRuleBreakingDep(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap(), flowPath);
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

    public void clearAssignedShifts() {
        for (Employee emp : emps) {
            emp.createShifts(daysInPeriod);
        }
    }

    public void getAssignedShifts(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }
        
        if (v == t) {
            String edge = "";
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
            if (emp.getShifts() == null) emp.createShifts(daysInPeriod);
            if (emp.getShifts()[day].isEmpty()) {
                emp.addShift(day, shift); 
            } else {
                Shift firstShift = emp.getShifts()[day].remove(0);
                switch (firstShift.getStartTime()) {
                    case 7:
                        if (shift.getStartTime() != 15) System.out.println("ERROR OCCURED! First shift: " + firstShift + ", new shift: " + shift + ". On day " + day + ", for emp: " + emp);
                        emp.addShift(day, new Shift(firstShift.getStartTime(), shift.getEndTime()-4));
                        break;
                    case 15:
                        if (shift.getStartTime() == 7) {
                            emp.addShift(day, new Shift(shift.getStartTime(), firstShift.getEndTime()-4));
                        } else if (shift.getStartTime() == 23) {
                            emp.addShift(day, new Shift(firstShift.getStartTime()+4, shift.getEndTime()));
                        } else {
                            System.out.println("ERROR OCCURED, Line 2");
                        }
                        break;
                    case 23:
                        if (shift.getStartTime() == 15) {
                            emp.addShift(day, new Shift(shift.getStartTime()+4, firstShift.getEndTime()));
                        } else {
                            System.out.println("ERROR OCCURED, Line 3");
                        }
                        break;
                    default:
                        System.out.println("Completely unexpected happenings");
                        break;
                }
            }
            
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    getAssignedShifts(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap());
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


    public void printShiftAssignments(Shift[][] shifts) {
        for (int i = 0; i < shifts.length; i++) {
            System.out.println("Employee: " + emps[i].getID());
            for (int j = 0; j < shifts[0].length; j++) {
                if (shifts[i][j] != null) System.out.println("\tDay: " + j + ", Shift: " + shifts[i][j]);
            }
        }
    }

    public void getInvalidShiftAssignments(Shift[][] shifts) {
        for (int i = 0; i < shifts.length; i++) {
            System.out.println("Employee: " + emps[i].getID());
            for (int j = 0; j < shifts[0].length; j++) {
                if (shifts[i][j] != null) System.out.println("\tDay: " + j + ", Shift: " + shifts[i][j]);
            }
        }
    }


    // getters/setters
    public Vertex getS() {
        return s;
    }
    public Vertex getT() {
        return t;
    }
    public int getDaysInPeriod() {
        return daysInPeriod;
    }

    public Employee[] getEmps() {
        return emps;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
    public int getBaseEdgeWeight() {
        return baseEdgeWeight;
    }
    public Shift[] getShifts() {
        return shifts;
    }
        
}
