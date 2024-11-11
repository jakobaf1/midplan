package app.program.model;

import java.util.ArrayList;
import java.util.List;

public class TabuAlgorithms {
    private int[] iniEmpHoursLeft;
    private int[][][] iniMatHoursLeft; // 3d to express days/experience level/time of day
    private int[][][] iniLabHoursLeft; // 3d to express days/experience level/time of day

    private Employee[] emps;
    private Shift[][] initialShiftAssignments;
    private int initialObjVal;
    private int hardConstraintPenalty = 100000;

    private FlowGraph fg;
    private ArrayList<ArrayList<Edge>> invalidPaths;

    public TabuAlgorithms(FlowGraph fg, ArrayList<ArrayList<Edge>> invalidPaths) {
        this.fg = fg;
        this.invalidPaths = invalidPaths;
        this.iniEmpHoursLeft = new int[fg.getEmps().length];
        this.iniMatHoursLeft = new int[fg.getDaysInPeriod()][2][3];
        this.iniLabHoursLeft = new int[fg.getDaysInPeriod()][2][3];

        this.emps = fg.getEmps();
        this.initialShiftAssignments = new Shift[emps.length][fg.getDaysInPeriod()];
        this.initialObjVal = 0;
    }


    //// Tabu for distributing ////
    public void gatherInformation() {
        // translating the invalid paths to hours needed
        for (ArrayList<Edge> le : invalidPaths) {
            int minFlow = le.get(2).getFlow();
            Employee emp = le.get(0).getTo().getEmp();
            // remove the shift from the employee
            for (Shift s : emp.getShifts()[le.get(2).getTo().getDay()]) {
                if (s.equals(le.get(2).getTo().getShift())) {
                    emp.getShifts()[le.get(2).getTo().getDay()].remove(s);
                    break;
                }
            }
            iniEmpHoursLeft[emp.getEmpIndex()] += minFlow;
        }


        // finding the amount of maternity and labor hours left to give
        Vertex t = fg.getT();
        for (Edge depDay : t.getInGoing()) {
            Vertex depNode = depDay.getFrm();
            int dep = depNode.getDep();
            int day = depNode.getDay();
            int time = depNode.getTimeOfDay();
            // if the edge going to the sink is fully saturated the need for hours is 0
            if (depDay.getCap()-depDay.getFlow() == 0) continue;

            for (Edge depExp : depNode.getInGoing()) {
                if (depExp.getType() == 1) continue;
                int expLvl = depExp.getFrm().getExpLvl();
                if (expLvl == 1 && depExp.getFlow() >= 8) continue;  
                if (expLvl == 1 && dep == 0) {
                    // System.out.println("Adding " + (8-depExp.getFlow()) + " flow from edge " + depExp);
                    iniLabHoursLeft[day][0][time] = 8-depExp.getFlow();
                } else if (expLvl == 1 && dep == 1) {
                    // System.out.println("Adding " + (8-depExp.getFlow()) + " flow from edge " + depExp);
                    iniMatHoursLeft[day][0][time] = 8-depExp.getFlow();
                }
                if (dep == 0) {
                    iniLabHoursLeft[day][1][time] = (depDay.getCap()-depDay.getFlow()) - iniLabHoursLeft[day][0][time];
                } else {
                    iniMatHoursLeft[day][1][time] = (depDay.getCap()-depDay.getFlow()) - iniMatHoursLeft[day][0][time];
                }
            }
        }

        // Seemingly correct for now
        // for (int day = 0; day < labHoursLeft.length; day++) {
        //     for (int exp = 0; exp < labHoursLeft[0].length; exp++) {
        //         for (int time = 0; time < labHoursLeft[0][0].length; time++) {
        //             if (labHoursLeft[day][exp][time] != 0) System.out.println("need " + labHoursLeft[day][exp][time] + " lab hours on day " + day + ", expLvl " + (exp+1) + ", and time of day " + time);
        //             if (matHoursLeft[day][exp][time] != 0) System.out.println("need " + matHoursLeft[day][exp][time] + " mat hours on day " + day + ", expLvl " + (exp+1) + ", and time of day " + time);
        //         }
        //     }
        // }

        // Define the shifts given into a matrix
        for (int emp = 0; emp < emps.length; emp++) {
            for (int day = 0; day < initialShiftAssignments[0].length; day++) {
                if (!emps[emp].getShifts()[day].isEmpty()) initialShiftAssignments[emp][day] = emps[emp].getShifts()[day].get(0);
            }
        }

        // define initial objective value
        initialObjVal += objectiveFunctionDist(initialShiftAssignments, iniEmpHoursLeft);
        System.out.println("Initial objective value: " + initialObjVal);
    }

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public int objectiveFunctionDist(Shift[][] assignments, int[] empHoursLeft) {
        int objectiveVal = 0;

        for (int emp = 0; emp < assignments.length; emp++) {
            List<Preference>[] empDayPrefs = fg.prefDays(emps[emp], fg.getStartDate(), assignments[0].length);
            List<Preference>[] empShiftPrefs = fg.prefShifts(emps[emp], fg.getStartDate(), assignments[0].length);
            if (empHoursLeft[emp] > 0) objectiveVal += empHoursLeft[emp]*(hardConstraintPenalty/4); // apply penalty to objective value if hours are unassigned

            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] == null) continue;
                // Day before
                if (day > 0 && assignments[emp][day-1] != null) {
                    int startTime = assignments[emp][day].getStartTime();
                    int endTime = assignments[emp][day-1].getEndTime();
                    if (endTime == 7 && (startTime-endTime) >= 11) {
                        objectiveVal += hardConstraintPenalty;
                    } else if ((24 - endTime + startTime) >= 11) {
                        objectiveVal += hardConstraintPenalty;
                    }
                }
                // day after
                if (day < assignments[0].length-1 && assignments[emp][day+1] != null) {
                    int startTime = assignments[emp][day+1].getStartTime();
                    int endTime = assignments[emp][day].getEndTime();
                    if (endTime == 7 && (startTime-endTime) >= 11) {
                        objectiveVal += hardConstraintPenalty;
                    } else if ((24 - endTime + startTime) >= 11) {
                        objectiveVal += hardConstraintPenalty;
                    }
                }

                if (empDayPrefs[day].isEmpty() && empShiftPrefs[day].isEmpty()) continue;
                if (!empDayPrefs[day].isEmpty() && empShiftPrefs[day].isEmpty()) { // For preferences regarding days
                    
                    // since the employee must have a shift assignment on this day, the weight should be added
                    int w = fg.setDayWeight(empDayPrefs[day]);
                    if (Math.abs(w) >= 0 && Math.abs(w) < fg.getBaseEdgeWeight()) {
                        objectiveVal -= (1000 - w); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    } else if (w != fg.getBaseEdgeWeight() && w != -fg.getBaseEdgeWeight()) {
                        objectiveVal += (w-fg.getBaseEdgeWeight());
                    }
                } else {
                    // check the preferences vs. the given shift assignment
                    int ws = fg.setShiftWeight(empShiftPrefs[day], assignments[emp][day]);
                    if (Math.abs(ws) >= 0 && Math.abs(ws) < fg.getBaseEdgeWeight()) {
                        objectiveVal -= (1000 - ws); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    } else if (ws != fg.getBaseEdgeWeight() && ws != -fg.getBaseEdgeWeight()) {
                        objectiveVal += (ws-fg.getBaseEdgeWeight());
                    }
                }
            }
        }
        return objectiveVal;
    }

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public ArrayList<DistTuple> findNeighborDist(DistTuple values) {
        ArrayList<DistTuple> neighbors = new ArrayList<>();
        for (int emp = 0; emp < emps.length; emp++) {
            if (values.empHoursLeft[emp] == 0) continue;
            int expLvl = emps[emp].getExpLvl();
            for (int day = 0; day < initialShiftAssignments[0].length; day++) {
                // TODO: implement the shifting of shifts for each day


            }
        }

        return neighbors;
    }

    // method for finding valid shifts for an employee on a given day
    public ArrayList<Shift> findValidShifts(int emp, int day, DistTuple values) {
        ArrayList<Shift> validShifts = new ArrayList<>();
        int dayBeforeEndTime = -1;
        int dayAfterStartTime = -1;
        if (day != 0 && values.assignments[emp][day-1] != null) dayBeforeEndTime = values.assignments[emp][day-1].getEndTime();
        if (day < values.assignments[0].length-1 && values.assignments[emp][day+1] != null) dayAfterStartTime = values.assignments[emp][day+1].getStartTime();
        for (Shift s : fg.getShifts()) {
            // check whether an existing shift can be extended to a 12 hour
            if (values.assignments[emp][day] == null && values.empHoursLeft[emp] >= 8) { // check for possible new shifts
                boolean needForShift = needForShift(emp, day, 8, values.labHoursLeft, values.matHoursLeft);
                boolean possibleShift = false; // check whether the employee can take any shifts on the given day considering the 11-hr rule
                possibleShift = (dayBeforeEndTime == -1 || s.validShift(values.assignments[emp][day].getStartTime(), dayBeforeEndTime) && 
                                (dayAfterStartTime == -1 || s.validShift(dayAfterStartTime, values.assignments[emp][day].getEndTime())));

                if (needForShift && possibleShift) validShifts.add(s);
            } else if (values.assignments[emp][day] != null && values.assignments[emp][day].calcHours() == 8) {
                boolean needForShift = needForShift(emp, day, 4, values.labHoursLeft, values.matHoursLeft);
                boolean possibleShift = false; // check whether the employee can take any shifts on the given day considering the 11-hr rule
                possibleShift = (dayBeforeEndTime == -1 || s.validShift(values.assignments[emp][day].getStartTime(), dayBeforeEndTime) && 
                                (dayAfterStartTime == -1 || s.validShift(dayAfterStartTime, values.assignments[emp][day].getEndTime())));

                if (needForShift && possibleShift) validShifts.add(s);

            } else {
                return validShifts;
            }
        }

        return validShifts;
    }

    // check whether the employee can take shift based on department/daily needs
    public boolean needForShift(int emp, int day, int hours, int[][][] labHoursLeft, int[][][] matHoursLeft) {
        boolean needForShift = false; 
        if (emps[emp].getDepartments().length > 1) { // both deps
            for (int expLvl = 0;  expLvl < labHoursLeft[day].length; expLvl++) {
                if (expLvl+1 < emps[emp].getExpLvl()) continue;
                for (int time = 0; time < labHoursLeft[day][expLvl].length; time++) {
                    if (hours == 4 && time != 1) continue;
                    needForShift = labHoursLeft[day][expLvl][time] >= hours || matHoursLeft[day][expLvl][time] >= hours;
                    if (needForShift) return needForShift;
                }
                
            }
            
        } else if (emps[emp].getDepartments()[0] == 0) { // labor dep
            for (int expLvl = 0;  expLvl < labHoursLeft[day].length; expLvl++) {
                if (expLvl+1 < emps[emp].getExpLvl()) continue;
                for (int time = 0; time < labHoursLeft[day][expLvl].length; time++) {
                    if (hours == 4 && time != 1) continue;
                    needForShift = labHoursLeft[day][expLvl][time] >= hours;
                    if (needForShift) return needForShift;
                }
                
            }
        } else { // maternity dep
            for (int expLvl = 0;  expLvl < matHoursLeft[day].length; expLvl++) {
                if (expLvl+1 < emps[emp].getExpLvl()) continue;
                for (int time = 0; time < matHoursLeft[day][expLvl].length; time++) {
                    if (hours == 4 && time != 1) continue;
                    needForShift = matHoursLeft[day][expLvl][time] >= hours;
                    if (needForShift) return needForShift;
                }
                
            }
        }
        return needForShift;
    }

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public Shift[][] searchDist(Shift[][] assignments) {
        DistTuple bestSolution = new DistTuple(initialShiftAssignments, iniEmpHoursLeft, iniLabHoursLeft, iniMatHoursLeft);
        DistTuple currentSolution = new DistTuple(initialShiftAssignments, iniEmpHoursLeft, iniLabHoursLeft, iniMatHoursLeft);
        List<DistTuple> tabuList = new ArrayList<>();

        // Should set a field called "maxIterations" instead of 1000 here
        for (int i = 0; i < 1000; i++) {
            ArrayList<DistTuple> neighbors = findNeighborDist(currentSolution);
            DistTuple bestNeighbor = neighbors.get(0);
            int bestObjectiveValue = Integer.MAX_VALUE;

            for (DistTuple neighbor : neighbors) {
                if (!tabuList.contains(neighbor)) {
                    int newObjVal = objectiveFunctionDist(neighbor.assignments, neighbor.empHoursLeft);
                    if (newObjVal < bestObjectiveValue) {
                        bestNeighbor = copyTuple(neighbor);
                        bestObjectiveValue = newObjVal;
                    }
                }
            }

            if (bestNeighbor == null) {
                break;
            }

            currentSolution = copyTuple(bestNeighbor);
            tabuList.add(bestNeighbor);

            // again I need a field "maxTabuSize" for maximum size of tabuList instead of "10000"
            if (tabuList.size() > 10000) {
                tabuList.remove(0);
            }

            if (objectiveFunctionDist(bestNeighbor.assignments, bestNeighbor.empHoursLeft) < objectiveFunctionDist(bestSolution.assignments, bestSolution.empHoursLeft)) {
                bestSolution = copyTuple(bestNeighbor);
            }
        }

        return bestSolution.assignments;
    }


    public DistTuple copyTuple(DistTuple values) {
        if (values == null) return null;
        // assignments
        Shift[][] newAssignment = new Shift[values.assignments.length][values.assignments[0].length];
        for (int i = 0; i < values.assignments.length; i++) {
            for (int j = 0; j < values.assignments[0].length; j++) {
                newAssignment[i][j] = values.assignments[i][j];
            }
        }
        // empHoursLeft
        int[] newEmpHours = new int[values.empHoursLeft.length];
        for (int i = 0; i < values.empHoursLeft.length; i++) {
            newEmpHours[i] = values.empHoursLeft[i];
        }
        // labHoursLeft
        int[][][] newLabHours = new int[values.labHoursLeft.length][values.labHoursLeft[0].length][values.labHoursLeft[0][0].length];
        for (int i = 0; i < values.labHoursLeft.length; i++) {
            for (int j = 0; j < values.labHoursLeft[0].length; j++) {
                for (int k = 0; k < values.labHoursLeft[0][0].length; k++) {
                    newLabHours[i][j][k] = values.labHoursLeft[i][j][k];
                }
            }
        }

        // matHoursLeft
        int[][][] newMatHours = new int[values.matHoursLeft.length][values.matHoursLeft[0].length][values.matHoursLeft[0][0].length];
        for (int i = 0; i < values.matHoursLeft.length; i++) {
            for (int j = 0; j < values.matHoursLeft[0].length; j++) {
                for (int k = 0; k < values.matHoursLeft[0][0].length; k++) {
                    newMatHours[i][j][k] = values.matHoursLeft[i][j][k];
                }
            }
        }

        return new DistTuple(newAssignment, newEmpHours, newLabHours, newMatHours);
    }



    //// Tabu for spreading ////

    // belongs to the tabu search related to spreading out the work days over the entire period
    public void objectiveFunctionSpread() {

    }
    
    // belongs to the tabu search related to spreading out the work days over the entire period
    public void findNeighborSpread() {

    }

    // belongs to the tabu search related to spreading out the work days over the entire period
    public void searchSpread() {

    }

    // Getters
    public Shift[][] getInitialShiftAssignments() {
        return initialShiftAssignments;
    }
}

class DistTuple {

    public Shift[][] assignments;
    public int[] empHoursLeft;
    public int[][][] matHoursLeft; // 3d to express days/experience level/time of day
    public int[][][] labHoursLeft; // 3d to express days/experience level/time of day
    

    DistTuple(Shift[][] assignments, int[] empHoursLeft, int[][][] labHoursLeft, int[][][] matHoursLeft) {
        this.assignments = assignments;
        this.empHoursLeft = empHoursLeft;
        this.matHoursLeft = matHoursLeft;
        this.labHoursLeft = labHoursLeft;
    }
    
}