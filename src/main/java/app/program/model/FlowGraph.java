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

    private int triWeeklyRequests = 0;
    private boolean triWeeklyUpdated = false;

    private Shift[] shifts;
    private Employee[] emps;
    private ArrayList<ArrayList<Edge>> invalidPaths = new ArrayList<>();

    int shiftViolations = 0;
    int assignedShiftErrors = 0;

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
                    Vertex layer5Node = new Vertex(5, (dep +"_"+time+"_"+day), dep, time, day);
                    // here it is linked to the sink
                    if (time == 0) {
                        addEdge(layer5Node, t, 6*8, baseEdgeWeight, 0);
                    } else {
                        addEdge(layer5Node, t, 4*8, baseEdgeWeight, 0);
                    }

                    for (int exp = 0; exp < 2; exp++) {
                        Vertex layer4Node = new Vertex(4, time+"_"+dep+"_"+(exp+1), dep, time, exp+1, day);
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
            if (triWeeklyUpdated) triWeeklyRequests++;
        
            // now I iterate through the days to make the relevant day nodes for the employee
            for (int day = 0; day < daysInPeriod; day++) {
                int weekday = date.getDayOfWeek().getValue();
                // now I check whether the employee has any preferences for shifts on this day
                boolean shiftPref = shiftPreferences[day].size() == dayPreferences[day].size();
                int w = setDayWeight(dayPreferences[day]);
                //  if the employee is required to not work this day, it is skipped and no edge is drawn
                if (w == Integer.MAX_VALUE && !shiftPref) {
                    date = date.plusDays(1); 
                    continue;
                }
                Vertex dayNode = new Vertex(2, weekdayToString(weekday)+"_"+day, e, day);
                Edge empToDay = null;
                // Edge fourHour;
                if (!shiftPref) {
                    // addEdge(empNode, dayNode, 12, w, 8);
                    // could split 12-hr edges up into two and add a (very) slight weight to the 4-hr part, thereby getting more 8 hours
                    empToDay = addEdge(empNode, dayNode, 8, w, 8);
                    // fourHour = addEdge(empNode, dayNode, 4, baseEdgeWeight+5, 8);
                } else {
                    // addEdge(empNode, dayNode, 12, baseEdgeWeight, 8);
                    // could split 12-hr edges up into two and add a (very) slight weight to the 4-hr part, thereby getting more 8 hours
                    empToDay = addEdge(empNode, dayNode, 8, baseEdgeWeight, 8);
                    // fourHour = addEdge(empNode, dayNode, 4, baseEdgeWeight+5, 8);
                }

                // ArrayList<Integer> twelveHourShiftWeights = new ArrayList<>();
                Edge dayEdge = null, nightEdge = null, twelveHourEdge = null;
                Vertex dayShiftNode = null, eveShiftNode = null, nightShiftNode = null;
                boolean twelveHourShifts = false;
                Vertex twelveHourNode = null;
                for (Shift shift : shifts) {
                    int ws = baseEdgeWeight;
                    // if (shiftPref) {
                    ws = setShiftWeight(shiftPreferences[day], shift);
                    if (ws == Integer.MAX_VALUE) continue;
                    // }
                    if (shift.calcHours() <= 8) {
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
                                dayShiftNode = new Vertex(3, shift.getStartTime() + "-" + shift.getEndTime(), e, day, shift);
                                dayEdge = addEdge(dayNode, dayShiftNode, shift.calcHours(), ws, 0);
                                for (int dep : e.getDepartments()) {
                                    addEdge(dayShiftNode, sharedNodes[day][6*dep+(expLvl-1)], 8, baseEdgeWeight, 8);
                                }
                                break;
                            case 15:
                                eveShiftNode = new Vertex(3, shift.getStartTime() + "-" + shift.getEndTime(), e, day, shift);
                                addEdge(dayNode, eveShiftNode, shift.calcHours(), ws, 0);
                                twelveHourEdge = addEdge(dayNode, eveShiftNode, 4, baseEdgeWeight+5, 4);
                                for (int dep : e.getDepartments()) {
                                    addEdge(eveShiftNode, sharedNodes[day][6*dep+(expLvl-1)+2], 8, baseEdgeWeight, 8);
                                }
                                break;
                            case 23:
                                nightShiftNode = new Vertex(3, shift.getStartTime() + "-" + shift.getEndTime(), e, day, shift);
                                nightEdge = addEdge(dayNode, nightShiftNode, shift.calcHours(), ws, 0);
                                for (int dep : e.getDepartments()) {
                                    addEdge(nightShiftNode, sharedNodes[day][6*dep+(expLvl-1)+4], 8, baseEdgeWeight, 8);
                                }
                                break;
                            default:
                                break;
                        }
                    } else { 
                        // Scenario with edge from employee node to eve shift node (or adding an extra 12-hr node for the day)
                        if (eveShiftNode == null) break;
                        if (twelveHourNode == null) {
                            twelveHourNode = new Vertex(2, "twelve_"+day, e, day);
                            addEdge(empNode, twelveHourNode, 4, empToDay.getWeight(), 4);
                        }
                        int twelveHourWeight = setShiftWeight(shiftPreferences[day], shift);
                        switch (shift.getStartTime()) {
                            case 7:
                                if (dayShiftNode == null) break;
                                if (twelveHourWeight == baseEdgeWeight) {
                                    twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, baseEdgeWeight+5, 4);
                                } else if (twelveHourWeight < dayEdge.getWeight()) {
                                    dayEdge.setWeight(twelveHourWeight);
                                    dayEdge.getCounterpart().setWeight(-dayEdge.getWeight());
                                    twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, twelveHourWeight, 4);
                                } else {
                                    twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, twelveHourWeight, 4);
                                }
                                break;
                            case 19:
                                if (nightShiftNode == null) break;
                                if (twelveHourWeight == baseEdgeWeight && twelveHourEdge != null) {
                                    break;
                                } else if (twelveHourWeight == baseEdgeWeight) {
                                    twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, baseEdgeWeight+5, 4);
                                } else if (twelveHourWeight < nightEdge.getWeight()) {
                                    nightEdge.setWeight(twelveHourWeight);
                                    nightEdge.getCounterpart().setWeight(-nightEdge.getWeight());
                                    if (twelveHourEdge == null) {
                                        twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, twelveHourWeight, 4);
                                    } else if (twelveHourWeight < twelveHourEdge.getWeight()) {
                                        twelveHourEdge.setWeight(twelveHourWeight);
                                        twelveHourEdge.getCounterpart().setWeight(-twelveHourEdge.getWeight());
                                    }
                                } else {
                                    if (twelveHourEdge == null) {
                                        twelveHourEdge = addEdge(twelveHourNode, eveShiftNode, 4, twelveHourWeight, 4);
                                    } else if (twelveHourWeight < twelveHourEdge.getWeight()) {
                                        twelveHourEdge.setWeight(twelveHourWeight);
                                        twelveHourEdge.getCounterpart().setWeight(-twelveHourEdge.getWeight());
                                    }
                                }

                                break;
                            default:
                                break;
                        }
                        //// Case where I add an edge from day to eveshift of cap 4 ////
                    //     if (eveShiftNode == null) break;
                    //     int twelveHourWeight = setShiftWeight(shiftPreferences[day], shift);
                    //     if (twelveHourWeight == Integer.MAX_VALUE) continue;
                    //     switch (shift.getStartTime()) {
                    //         case 7:
                    //             if (dayShiftNode == null) break;
                    //             if (dayEdge.getWeight() == baseEdgeWeight && twelveHourWeight < baseEdgeWeight) {
                    //                 dayEdge.setWeight(twelveHourWeight);
                    //                 dayEdge.getCounterpart().setWeight(-dayEdge.getWeight());
                    //                 twelveHourEdge.setWeight(twelveHourWeight);
                    //             } else {
                    //                 twelveHourEdge.setWeight(twelveHourWeight);
                    //             }
                    //             twelveHourShifts = true;
                    //             break;
                    //         case 19:
                    //             if (nightShiftNode == null) break;
                    //             if (twelveHourWeight == baseEdgeWeight && twelveHourEdge != null) {
                    //                 break;
                    //             } else if (nightEdge.getWeight() == baseEdgeWeight && twelveHourWeight < baseEdgeWeight) {
                    //                 nightEdge.setWeight(twelveHourWeight);
                    //                 nightEdge.getCounterpart().setWeight(-nightEdge.getWeight());
                    //                 if (twelveHourWeight < twelveHourEdge.getWeight()) {
                    //                     twelveHourEdge.setWeight(twelveHourWeight);
                    //                 }
                    //             } else {
                    //                 if (twelveHourWeight < twelveHourEdge.getWeight()) {
                    //                     twelveHourEdge.setWeight(twelveHourWeight);
                    //                 }
                    //             }
                    //             twelveHourShifts = true;
                    //             break;
                    //         default:
                    //             break;
                    //     }
                    }
                }
                // in order to mimic the weight required to reach the shiftnode, the weight for the day edge is added
                // if (twelveHourEdge != null) {
                //     twelveHourEdge.setWeight(twelveHourEdge.getWeight() + empToDay.getWeight());
                //     twelveHourEdge.getCounterpart().setWeight(-twelveHourEdge.getWeight());
                //     // System.out.println("Made the 12-hour edge: " + twelveHourEdge + ", for emp: " + e + " on day: " + day);
                // }
                // if (!twelveHourShifts) {
                //     twelveHourEdge = null;
                // } else if (twelveHourEdge != null) {
                //     twelveHourEdge.getCounterpart().setWeight(-twelveHourEdge.getWeight());
                // }
                date = date.plusDays(1);
            }

        
        }


    }


    // method for creating new edges
    public Edge addEdge(Vertex frm, Vertex to, int cap, int weight, int lowerBound) {
        Edge newEdge = new Edge(0, frm, to, cap, weight, lowerBound);
        frm.addOutGoing(newEdge);
        to.addInGoing(newEdge);
        
        // Now to create the backwards edge
        Edge counterEdge = new Edge(1, to, frm, 0, newEdge, -weight, lowerBound);
        to.addOutGoing(counterEdge);
        frm.addInGoing(counterEdge);
        newEdge.setCounterpart(counterEdge);
        return newEdge;
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
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 1 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                dayPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 3: // even weeks
                            date = date.plusDays(firstDay);
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 0 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                dayPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 4: // tri-weekly
                            for (int i = firstDay+7*(triWeeklyRequests%2); i < days; i += 21) {
                                dayPreferences[i].add(p);
                            }
                            // for (int i = firstDay+7*(triWeeklyRequests%2); i < days; i += 7) {
                            //     if ((i-(firstDay+7*(triWeeklyRequests%2)))%21 == 0) continue;
                            //     Preference alteredPref = p;
                            //     alteredPref.setWanted(false);
                            //     dayPreferences[i].add(alteredPref);
                            //     System.out.println("Emp: " + e + ", day " + i +", Just added preference: " + alteredPref);
                            //     System.out.println("Emp: " + e + ", day " + i +", Used to be: " + p);
                            // }
                            triWeeklyUpdated = true;
                            break;
                        case 5: // monthly
                            for (int i = firstDay; i < days; i += 28) {
                                dayPreferences[i].add(p);
                            }
                            break;
                        default:
                            break;
                    }
                }  else { // if a day is wanted, but no repetition specified the default is weekly
                    firstDay = firstDay(date, p);
                    for (int i = firstDay; i < days; i += 7) {
                        dayPreferences[i].add(p);
                    }
                } 
            }   

            if (p.getDate() != null) {
                int dayIndex = 0;
                for (int i = 0; i < days; i++) {
                    if (p.getDate().equals(date)) {
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
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

                if (p.getDay() != -1  && p.getRepeat() != -1) {
                    int firstDay = firstDay(date, p);
                    int startingWeek = 0;
                    switch (p.getRepeat()) {
                        case 1: // weekly
                            for (int i = firstDay; i < days; i += 7) {
                                shiftPreferences[i].add(p);
                            }
                            break;
                        case 2: // odd weeks
                            date = date.plusDays(firstDay);
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 1 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                shiftPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 3: // even weeks
                            date = date.plusDays(firstDay);
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 0 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                shiftPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 4: // tri-weekly
                            for (int i = firstDay+7*(triWeeklyRequests%2); i < days; i += 21) {
                                shiftPreferences[i].add(p);
                            }
                            triWeeklyUpdated = true;
                            break;
                        case 5: // monthly
                            for (int i = firstDay; i < days; i += 28) {
                                shiftPreferences[i].add(p);
                            }
                            break;
                        default:
                            break;
                    }  
                } else if (p.getDay() != -1 && p.getDate() == null && p.getRepeat() == -1) {
                    int firstDay = firstDay(date, p);
                    for (int i = firstDay; i < days; i += 7) {
                        shiftPreferences[i].add(p);
                    }  
                } else if (p.getDate() == null  && p.getRepeat() == -1) {
                    for (int i = 0; i < days; i++) {
                        shiftPreferences[i].add(p);
                    }  
                } else if (p.getRepeat() != -1) { // For shifts without a specific day, but weekly/monthly... prefs
                    int startingWeek = 0;
                    int firstDay = 0;
                    switch (p.getRepeat()) {
                        case 1: // weekly
                            for (int i = firstDay; i < days; i += 7) {
                                shiftPreferences[i].add(p);
                            }
                            break;
                        case 2: // odd weeks
                            date = date.plusDays(firstDay);
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 1 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                shiftPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 3: // even weeks
                            date = date.plusDays(firstDay);
                            week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            startingWeek = week%2 == 0 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                shiftPreferences[i].add(p);
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 4: // tri-weekly
                            for (int i = firstDay+7*(triWeeklyRequests%2); i < days; i += 21) {
                                shiftPreferences[i].add(p);
                            }
                            triWeeklyUpdated = true;
                            break;
                        case 5: // monthly
                            for (int i = firstDay; i < days; i += 28) {
                                shiftPreferences[i].add(p);
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    int dayIndex = 0;
                    for (int i = 0; i < days; i++) {
                        if (p.getDate().equals(date)) {
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
            if (p.getDay() != -1 && p.getShift() == null) {
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
                return p.getWanted() ? 5 : 1995;
            case 3:
                return p.getWanted() ? 750 : 1250;
            case 4:
                return p.getWanted() ? 950 : 1050;
            case 5:
                return p.getWanted() ? 995 : 1005;
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
                        // System.out.println("Emp: " + emps[i] + ", Is in violation of shift: " + emps[i].getShifts()[j-1].get(0) + " on day " + (j-1) + ", Shift: " + emps[i].getShifts()[j].get(0) + ", on day: " + j);
                        shiftViolations++;
                    } else if (24-endTime+startTime < 11) {
                        // System.out.println("Emp: " + emps[i] + ", Is in violation of shift: " + emps[i].getShifts()[j-1].get(0) + " on day " + (j-1) + ", Shift: " + emps[i].getShifts()[j].get(0) + ", on day: " + j);
                        shiftViolations++;
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
            Edge newEdge = null;
            for (int i = 0; i < path.size(); i++) {
                if (i == path.size()-1) {
                    edge += path.get(i);
                } else {
                    for (Edge e : path.get(i).getOutGoing()) {
                        if (e.getTo() == path.get(i+1) && flows.get(i) == e.getFlow()) {
                            newEdge = e;
                            break;
                        }
                    }
                    edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + ", w= " + newEdge.getWeight() + " -> ";
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
            Edge empToDayEdge = null;
            for (int i = 0; i < path.size(); i++) {
                if (i == path.size()-1) {
                    edge += path.get(i);
                } else {
                    edge += path.get(i) + " - " + flows.get(i) + "/" + capacities.get(i) + " -> ";
                }
                if (path.get(i).getPurpose() == 1) {
                    empToDayEdge = path.get(i).getOutGoing().get(0);
                    for (Edge e : path.get(i).getOutGoing()) {
                        if (e.getTo() == path.get(i+1)) empToDayEdge = e;
                    }
                    breaksRule = empToDayEdge.getFlow() != empToDayEdge.getCap();
                }
            }
            if (breaksRule) flowPath.add(edge);   
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    // if ((v.getPurpose() == 2 || v.getPurpose() == 3) && flow != e.getFlow() ) continue;
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

    public void updateInvalidPaths(Vertex v, Vertex t, boolean[] visited, ArrayList<Vertex> path, ArrayList<Integer> flows, int flow, ArrayList<Integer> capacities, int cap, int purpose) {
        visited[v.getVertexIndex()] = true;
        path.add(v);
        if (flow != 0) {
            flows.add(flow);
        }
        if (cap > 0) {
            capacities.add(cap);
        }
        
        if (v == t) {
            ArrayList<Edge> edges = new ArrayList<>();
            Edge edge = null;
            Employee emp = path.get(3).getEmp();
            int day = path.get(3).getDay();
            Shift shift = path.get(3).getShift();
            for (int i = 0; i < path.size()-1; i++) {
                edge = path.get(i).getOutGoing().get(0);
                for (Edge e : path.get(i).getOutGoing()) {
                    if (e.getTo() == path.get(i+1) && e.getFlow() != 0) {
                        edge = e;
                        break;
                    }
                }
                edges.add(edge);
            }
            
            int dayBeforeEndTime = -1;
            if (day != 0 && !emp.getShifts()[day-1].isEmpty()) dayBeforeEndTime = emp.getShifts()[day-1].get(0).getEndTime();
            boolean shiftRule = (dayBeforeEndTime == -1 || shift.validShift(shift.getStartTime(), dayBeforeEndTime));
            
            // handles cases where edges have been misused (e.g. 4/8 shift edge)
            if (edges.get(1).getFlow() != edges.get(1).getCap() && purpose == 1) {
                // System.out.println("found edge " + edges.get(2) + " to be invalid for employee: " + emp);
                invalidPaths.add(edges);
            } else if (!shiftRule && purpose == 2) { // handles if the break between shifts is not obeyed
                // System.out.println("found shift from edge " + edges.get(2) + " to be invalid for employee: " + emp);
                invalidPaths.add(edges);
            }

            
        } else {
            for (Edge e : v.getOutGoing()) {
                if (e.getFlow() > 0 && !visited[e.getTo().getVertexIndex()]) {
                    // if ((v.getPurpose() == 2 || v.getPurpose() == 3) && flow != e.getFlow() ) continue;
                    updateInvalidPaths(e.getTo(), t, visited, path, flows, e.getFlow(), capacities, e.getCap(), purpose);
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
                    // if ((v.getPurpose() == 2 || v.getPurpose() == 3) && flow != e.getFlow() ) continue;
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
            Employee emp = null;
            int day = -1;
            Shift shift = null;
            Edge edge = null;
            ArrayList<Edge> edges = new ArrayList<>();
            for (int i = 0; i < path.size()-1; i++) {
                Vertex node = path.get(i);
                if (node.getPurpose() == 3) {
                    shift = node.getShift();
                    emp = node.getEmp();
                    day = node.getDay();
                }
                edge = path.get(i).getOutGoing().get(0);
                for (Edge e : path.get(i).getOutGoing()) {
                    if (e.getTo() == path.get(i+1)) edge = e;
                }
                edges.add(edge);
            }
            if (emp.getShifts() == null) emp.createShifts(daysInPeriod);
            if (emp.getShifts()[day].isEmpty()) {
                emp.addShift(day, shift);
            } else {
                Shift firstShift = emp.getShifts()[day].remove(0);
                switch (firstShift.getStartTime()) {
                    case 7:
                        if (shift.getStartTime() != 15) {
                            // System.out.println("ERROR OCCURED! First shift: " + firstShift + ", new shift: " + shift + ". On day " + day + ", for emp: " + emp);
                            assignedShiftErrors++;
                            emp.addShift(day,firstShift);
                            emp.addShift(day, shift);
                            break;
                        }
                        emp.addShift(day, new Shift(firstShift.getStartTime(), shift.getEndTime()-4));
                        break;
                    case 15:
                        if (shift.getStartTime() == 7 && edges.get(2).getFlow() == 8) {
                            emp.addShift(day, new Shift(shift.getStartTime(), firstShift.getEndTime()-4));
                        } else if (shift.getStartTime() == 23 && edges.get(2).getFlow() == 8) {
                            emp.addShift(day, new Shift(firstShift.getStartTime()+4, shift.getEndTime()));
                        } else {
                            // System.out.println("ERROR OCCURED, Line 2, " + " First shift: " + firstShift + ", new shift: " + shift + ". On day " + day + ", for emp: " + emp);
                            assignedShiftErrors++;
                            emp.addShift(day, firstShift);
                            emp.addShift(day, shift);
                        }
                        break;
                    case 23:
                        if (shift.getStartTime() == 15) {
                            emp.addShift(day, new Shift(shift.getStartTime()+4, firstShift.getEndTime()));
                        } else {
                            // System.out.println("ERROR OCCURED, Line 3" + " First shift: " + firstShift + ", new shift: " + shift + ". On day " + day + ", for emp: " + emp);
                            assignedShiftErrors++;
                            emp.addShift(day, firstShift);
                            emp.addShift(day, shift);
                        }
                        break;
                    default:
                        // System.out.println("Completely unexpected happenings");
                        assignedShiftErrors++;
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
    public ArrayList<ArrayList<Edge>> getInvalidPaths() {
        return invalidPaths;
    }

    public void clearInvalidPaths() {
        invalidPaths = new ArrayList<>();
    }
     
    public int getShiftViolations() {
        return shiftViolations;
    }

    public int getAssignedShiftErrors() {
        return assignedShiftErrors;
    }
}
