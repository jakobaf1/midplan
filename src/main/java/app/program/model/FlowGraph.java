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

    private Shift[] shifts;
    private Employee[] emps;

    public FlowGraph(int periodInWeeks, Shift[] shifts, Employee[] emps) {
        this.periodInWeeks = periodInWeeks;
        this.shifts = shifts;
        this.emps = emps;
    }
    
    public void generateGraph(LocalDate startDate) {
        // Initialize the date for which the roster begins
        LocalDate date = startDate;
        // Used to bind edges
        Vertex[][] sharedNodes = new Vertex[daysInPeriod][2*3*2]; // 2 is the amount of departments, 3 times of day and 2 exp levels in the model

        for (int day = 0; day < daysInPeriod; day++) {

            for (int dep = 0; dep < 2; dep++) {
                for (int time = 0; time < 3; time++) {
                    // this node signifies the dep_time node (6th layer)
                    Vertex layer5Node = new Vertex(5, (dep +"_"+time+"_"+day), dep, time);
                    // here it is linked to the sink
                    if (time == 0) {
                        addEdge(layer5Node, t, 6*8, 0, 0);
                    } else {
                        addEdge(layer5Node, t, 4*8, 0, 0);
                    }

                    for (int exp = 0; exp < 2; exp++) {
                        Vertex layer4Node = new Vertex(4, time+"_"+dep+"_"+exp, dep, time, exp+1);
                        if (time == 0) {
                            addEdge(layer4Node, layer5Node, (6-exp)*8, 0, 0);
                        } else {
                            addEdge(layer4Node, layer5Node, (4-exp)*8, 0, 0);
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
            addEdge(s, empNode, daysInPeriod*e.getWeeklyHrs(), 0, 0);

            // I define the array containing the preferences of the employee
            List<Preference>[] dayPreferences = prefDays(e, date, daysInPeriod);
            List<Preference>[] shiftPreferences = prefShifts(e, date, daysInPeriod);
        
            // now I iterate through the days to make the relevant day nodes for the employee
            for (int day = 0; day < daysInPeriod; day++) {
                int weekday = date.getDayOfWeek().getValue();
                // now I check whether the employee has any preferences for shifts on this day
                boolean shiftPref = shiftPreferences[day].isEmpty();

                int w = setDayWeight(dayPreferences[day]);
                //  if the employee is required to not work this day, it is skipped and no edge is drawn
                if (w == 10000) {
                    date = date.plusDays(1); 
                    continue;
                }
                Vertex dayNode = new Vertex(2, weekdayToString(weekday), day);
                if (!shiftPref) {
                    addEdge(empNode, dayNode, 12, w, 8);
                } else {
                    addEdge(empNode, dayNode, 12, 0, 8);
                }

                for (Shift shift : shifts) {
                    int ws = 0;
                    if (shiftPref) {
                        ws = setShiftWeight(shiftPreferences[day], shift);
                        if (ws == 10000) continue;
                    }
                    
                    Vertex shiftNode = new Vertex(3, shift.getStartTime() + "-" + shift.getEndTime(), shift);
                    addEdge(dayNode, shiftNode, shift.calcHours(), ws, 8);

                    int expLvl = e.getExpLvl();
                    // switch case to connect the correct the right nodes
                    switch (shift.getStartTime()) {
                        case 0:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1], 8, 0, 8);
                            }
                            break;
                        case 7:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1], 8, 0, 8);
                                if (shift.calcHours() == 12) addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1+2], weekday, w, w);
                            }
                            break;
                        case 15:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1+2], 8, 0, 8);
                            }
                        case 19:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1+4], 8, 0, 8);
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1+2], 4, 0, 4);
                            }
                            break;
                        case 23:
                            for (int dep : e.getDepartments()) {
                                addEdge(shiftNode, sharedNodes[day][6*dep+expLvl-1+4], 8, 0, 8);
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
        List<Preference>[] dayPreferences = new ArrayList[56];
        for (int i = 0; i < dayPreferences.length; i++) {
            dayPreferences[i] = new ArrayList<>();
        }


        for (Preference p : e.getPref()) {
            if (p.getDay() != -1) {
                int firstDay = 0;
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int weekDay = date.getDayOfWeek().getValue()-1;

                // find out what day of the week the schefule starts
                if (p.getDay() == weekDay) {
                    firstDay = 0;
                } else if (p.getDay() < weekDay) {
                    firstDay = p.getDay()+7-weekDay;
                } else {
                    firstDay = p.getDay()-weekDay;
                }

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

        for (Preference p : e.getPref()) {
            if (p.getShift() != null) {
                if (p.getDay() != -1) {
                    int firstDay = 0;
                    int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    int weekDay = date.getDayOfWeek().getValue()-1;

                    // find out what day of the week the schefule starts
                    if (p.getDay() == weekDay) {
                        firstDay = 0;
                    } else if (p.getDay() < weekDay) {
                        firstDay = p.getDay()+7-weekDay;
                    } else {
                        firstDay = p.getDay()-weekDay;
                    }

                    // TODO: Dunno if the following will be relevant
                    // if (p.getRepeat() != -1) {
                    //     int startingWeek = 0;
                        // switch (p.getRepeat()) {
                            // case 0: // daily
                            //      for (int i = 0; i < days, i++) {
                            //          dayPreferences.get(i).add(p);
                            //      }
                            // case 1: // weekly
                                for (int i = firstDay; i < days; i += 7) {
                                    shiftPreferences[i].add(p);
                                }
                            //     break;
                            // case 2: // odd weeks
                            //     date = date.plusDays(firstDay);
                            //     startingWeek = week%2 == 1 ? 0 : 1;
                            //     for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                            //         dayPreferences.get(i).add(p);
                            //     }
                            //     date = date.minusDays(firstDay);
                            //     break;
                            // case 3: // even weeks
                            //     date = date.plusDays(firstDay);
                            //     startingWeek = week%2 == 0 ? 0 : 1;
                            //     for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                            //         dayPreferences.get(i).add(p);
                            //     }
                            //     date = date.minusDays(firstDay);
                            //     break;
                            // case 4: // tri-weekly
                            //     // TODO
                            //     break;
                            // case 5: // monthly
                            //     for (int i = firstDay; i < days; i += 28) {
                            //         dayPreferences.get(i).add(p);
                            //     }
                            //     break;
                            // default:
                            //     break;
                        // }
                    // }   
                } 
            }

            if (p.getDate() != null) {
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

        return shiftPreferences;
    }

    public int setDayWeight(List<Preference> dailyPref) {
        for (Preference p : dailyPref) {
            if (p.getDay() != -1) {
                switch (p.getPrefLvl()) {
                    case 1:
                        return p.getWanted() ? 0 : 10000;
                    case 2:
                        return p.getWanted() ? 0 : 2000;
                    case 3:
                        return p.getWanted() ? 750 : 1250;
                    case 4:
                        return p.getWanted() ? 950 : 1050;
                    case 5:
                        return p.getWanted() ? 995 : 1005;
                    default:
                        return 0;
                }
            }
        }
        return 0;
    }

    public int setShiftWeight(List<Preference> dailyPref, Shift shift) {
        for (Preference p : dailyPref) {
            if (Shift.sameShift(p.getShift(), shift)) {
                switch (p.getPrefLvl()) {
                    case 1:
                        return p.getWanted() ? 0 : 10000;
                    case 2:
                        return p.getWanted() ? 0 : 2000;
                    case 3:
                        return p.getWanted() ? 750 : 1250;
                    case 4:
                        return p.getWanted() ? 950 : 1050;
                    case 5:
                        return p.getWanted() ? 995 : 1005;
                    default:
                        return 0;
                }
            }
        }
        return 0;
    }

    public String weekdayToString(int day) {
        switch (day) {
            case 0:
                return "monday";
            case 1:
                return "tuesday";
            case 2:
                return "wednesday";
            case 3:
                return "thursday";
            case 4:
                return "friday";
            case 5:
                return "saturday";
            case 6:
                return "sunday";
            case 7:
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

    // getters/setters
    public int getDaysInPeriod() {
        return daysInPeriod;
    }

    public Employee[] getEmps() {
        return emps;
    }
        
}
