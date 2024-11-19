package app.program.model;

import java.util.ArrayList;
import java.util.List;

public class TabuAlgorithms {
    private int[] iniEmpHoursLeft;
    private int[][][] iniMatHoursLeft; // 3d to express days/experience level/time of day
    private int[][][] iniLabHoursLeft; // 3d to express days/experience level/time of day
    private DistTuple iniTuple;

    private Employee[] emps;
    private Shift[][] initialShiftAssignments;
    private int initialObjVal;
    private int hardConstraintPenalty = 100000;
    private int softConstraintPenalty = 250;

    private FlowGraph fg;
    private ArrayList<ArrayList<Edge>> invalidPaths;

    public TabuAlgorithms(FlowGraph fg, ArrayList<ArrayList<Edge>> invalidPaths) {
        this.fg = fg;
        this.invalidPaths = invalidPaths;
        System.out.println("invalid Shifts: " + invalidPaths.size());
        this.iniEmpHoursLeft = new int[fg.getEmps().length];
        this.iniMatHoursLeft = new int[fg.getDaysInPeriod()][2][3];
        this.iniLabHoursLeft = new int[fg.getDaysInPeriod()][2][3];

        this.emps = fg.getEmps();
        this.initialShiftAssignments = new Shift[emps.length][fg.getDaysInPeriod()];
        this.initialObjVal = 0;
        gatherInformation();
        this.iniTuple = new DistTuple(initialShiftAssignments, iniEmpHoursLeft, iniLabHoursLeft, iniMatHoursLeft);
    }


    //// Tabu for distributing ////
    public void gatherInformation() {
        // translating the invalid paths to hours needed
        for (ArrayList<Edge> le : invalidPaths) {
            int minFlow = le.get(2).getFlow();
            Employee emp = le.get(2).getTo().getEmp();
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
                if (!emps[emp].getShifts()[day].isEmpty()) {
                    initialShiftAssignments[emp][day] = emps[emp].getShifts()[day].get(0);
                }
            }
        }

        // define initial objective value
        // initialObjVal = objectiveFunctionDist(initialShiftAssignments, iniEmpHoursLeft);
        initialObjVal = objectiveFunctionSpread(initialShiftAssignments);
        System.out.println("Initial objective value: " + initialObjVal);
    }

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public int objectiveFunctionDist(Shift[][] assignments, int[] empHoursLeft) {
        int objectiveVal = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            if (empHoursLeft[emp] > 0) objectiveVal += empHoursLeft[emp]*(hardConstraintPenalty/8); // apply penalty to objective value if hours are unassigned
        }
        objectiveVal += calcPreferencePenalty(assignments);
        return objectiveVal;
    }

    public int calcPreferencePenalty(Shift[][] assignments) {
        int penalty = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            List<Preference>[] empDayPrefs = fg.prefDays(emps[emp], fg.getStartDate(), assignments[0].length);
            List<Preference>[] empShiftPrefs = fg.prefShifts(emps[emp], fg.getStartDate(), assignments[0].length);
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] == null) continue;
                int dayBeforeEndTime = -1;
                if (day != 0 && assignments[emp][day-1] != null) dayBeforeEndTime = assignments[emp][day-1].getEndTime();
                boolean shiftRule = (dayBeforeEndTime == -1 || assignments[emp][day].validShift(assignments[emp][day].getStartTime(), dayBeforeEndTime));
                if (!shiftRule) penalty += hardConstraintPenalty;
                if (empDayPrefs[day].isEmpty() && empShiftPrefs[day].isEmpty()) continue;
                if (!empDayPrefs[day].isEmpty() && empShiftPrefs[day].isEmpty()) { // For preferences regarding days
                    
                    // since the employee must have a shift assignment on this day, the weight should be added
                    int w = fg.setDayWeight(empDayPrefs[day]);
                    if (w == Integer.MAX_VALUE) {
                        penalty += hardConstraintPenalty;
                    } else if (Math.abs(w) >= 0 && Math.abs(w) < fg.getBaseEdgeWeight()) {
                        penalty -= (1000 - w); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    } else if (w != fg.getBaseEdgeWeight() && w != -fg.getBaseEdgeWeight()) {
                        penalty += (w-fg.getBaseEdgeWeight());
                    }
                } else {
                    // check the preferences vs. the given shift assignment
                    int ws = fg.setShiftWeight(empShiftPrefs[day], assignments[emp][day]);
                    if (ws == Integer.MAX_VALUE) {
                        penalty += hardConstraintPenalty;
                    } else if (Math.abs(ws) >= 0 && Math.abs(ws) < fg.getBaseEdgeWeight()) {
                        penalty -= (1000 - ws); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                    } else if (ws != fg.getBaseEdgeWeight() && ws != -fg.getBaseEdgeWeight()) {
                        penalty += (ws-fg.getBaseEdgeWeight());
                    }
                }
            }
        }
        return penalty;
    }
    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public ArrayList<DistTuple> findNeighborDist(DistTuple values) {
        ArrayList<DistTuple> neighbors = new ArrayList<>();
        DistTuple neighbor = copyTuple(values);
        for (int emp = 0; emp < emps.length; emp++) {
            if (neighbor.empHoursLeft[emp] == 0) continue;
            for (int day = 0; day < values.assignments[0].length; day++) {
                // TODO: wanna find out where the most "null" shifts in a row are to switch those
                for (int newEmp = 0; newEmp < emps.length; newEmp++) {
                    if (neighbor.empHoursLeft[emp] == 0) continue;
                    for (int newDay = 0; newDay < values.assignments[0].length; newDay++) {
                        if (newDay == day) continue;
                        
        
                        
                    }
                }
                
            }





            // TODO: IDEA! this can be done after exchanging shifts between employees who have shifts the same days
            for (int day = 0; day < values.assignments[0].length; day++) {
                ArrayList<Shift> validShifts = findValidShifts(emp, day, neighbor);
                if (validShifts.isEmpty()) {
                    // TODO: if validShifts is empty no addition can be made. Then I should try and distribute a 12-hr and the remaining hours into two 8-hrs

                } else {
                    Shift addedShift = validShifts.get(0);
                    // for (Shift s : findValidShifts(emp, day, neighbor)) {
                    //     // TODO: check for the best shift here through preferences
                    // }
                    if (neighbor.assignments[emp][day] != null) neighbor.empHoursLeft[emp] += neighbor.assignments[emp][day].calcHours();
                    neighbor.assignments[emp][day] = addedShift;
                    System.out.println("Added shift: " + addedShift + ", for emp " + (emp+1) + ", on day " + day);
                    neighbor.empHoursLeft[emp] -= addedShift.calcHours();
                }
                
            }
        }
        neighbors.add(neighbor);

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
            // check whether an 8-hr shift can be inserted
            if (values.assignments[emp][day] == null && values.empHoursLeft[emp] >= 8) { // check for possible new shifts
                boolean needForShift = needForShift(emp, day, 8, values.labHoursLeft, values.matHoursLeft);
                boolean possibleShift = false; // check whether the employee can take any shifts on the given day considering the 11-hr rule
                possibleShift = (dayBeforeEndTime == -1 || s.validShift(s.getStartTime(), dayBeforeEndTime) && 
                                (dayAfterStartTime == -1 || s.validShift(dayAfterStartTime, s.getEndTime())));

                if (needForShift && possibleShift) validShifts.add(s);
            } else if (values.assignments[emp][day] != null && values.assignments[emp][day].calcHours() == 8) { // check whether an existing shift can be extended to a 12 hour
                if (s.calcHours() == 8) continue;
                boolean needForShift = needForShift(emp, day, 4, values.labHoursLeft, values.matHoursLeft);
                boolean possibleShift = false; // check whether the employee can take any shifts on the given day considering the 11-hr rule
                possibleShift = ((dayBeforeEndTime == -1 || s.validShift(s.getStartTime(), dayBeforeEndTime)) && (dayAfterStartTime == -1 || s.validShift(dayAfterStartTime, s.getEndTime())));
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
    public Shift[][] searchDist() {
        DistTuple bestSolution = iniTuple;
        DistTuple currentSolution = iniTuple;
        List<DistTuple> tabuList = new ArrayList<>();

        // Should set a field called "maxIterations" instead of 1000 here
        for (int i = 0; i < 10; i++) {
            ArrayList<DistTuple> neighbors = findNeighborDist(currentSolution);
            DistTuple bestNeighbor = null;
            int bestObjectiveValue = initialObjVal;

            for (DistTuple neighbor : neighbors) {
                // TODO: need another way of checking this since copying makes the object a new one (probably just a method to check whether all assignments are the same)
                // System.out.println("contains: " + tabuList.contains(neighbor));
                if (!tabuList.contains(neighbor)) {
                    int newObjVal = objectiveFunctionDist(neighbor.assignments, neighbor.empHoursLeft);
                    if (newObjVal < bestObjectiveValue) {
                        bestNeighbor = neighbor; //copyTuple(neighbor);
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
        System.out.println("Final objective value: " + objectiveFunctionDist(bestSolution.assignments, bestSolution.empHoursLeft));
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
    public int objectiveFunctionSpread(Shift[][] assignments) {
        int objectiveVal = 0;
        // penalty which penalizes working more or less in a week than one is hired for
        for (int emp = 0; emp < assignments.length; emp++) {
            objectiveVal += calcEmpWeeklyPenalty(assignments, emp);
        }

        // the following adds the preference weights as well as the general constraint break penalties
        objectiveVal += calcPreferencePenalty(assignments);        

        // TODO: Constraints for not having hours left is done here, since no hours are distributed in the spreading. They should probably be merged
        // for (int emp = 0; emp < assignments.length; emp++) {
        //     if (empHoursLeft[emp] > 0) objectiveVal += empHoursLeft[emp]*(hardConstraintPenalty/8); // apply penalty to objective value if hours are unassigned
        // }
        return objectiveVal;
    }

    public int calcEmpWeeklyPenalty(Shift[][] assignments, int emp) {
        int penalty = 0;
        // penalty which penalizes working more or less in a week than one is hired for
            int weeklyHrs = emps[emp].getWeeklyHrs();
            int givenWeeklyHrs = 0;
            // LocalDate date = fg.getStartDate();
            // int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            for (int day = 0; day < assignments[0].length; day++) {
                if (day % 7 == 0) {
                    penalty += softConstraintPenalty*Math.abs(weeklyHrs-givenWeeklyHrs);
                    givenWeeklyHrs = 0;
                }
                if (assignments[emp][day] != null) givenWeeklyHrs += assignments[emp][day].calcHours();
            }
        return penalty;
    }
    
    // belongs to the tabu search related to spreading out the work days over the entire period
    public ArrayList<Shift[][]> findNeighborSpread(Shift[][] assignments) {
        // Find neighbors by exchanging shifts on different days between employees
        // TODO: might need a list: ArrayList<Shift>[][] tracking which employees have a shift on a certain day
        ArrayList<Shift[][]> neighbors = new ArrayList<>();
        int maxSize = 100;
        Shift[][] neighbor = copyMatrix(assignments);
        Employee[] sortedEmployees = getEmployeesSortedByPenalties(assignments);
        for (int i = 0; i < sortedEmployees.length; i++) {
            int emp = sortedEmployees[i].getEmpIndex();
            for (int day = 0; day < neighbor[0].length; day++) {
                if (neighbor[emp][day] == null) continue;
                for (int newEmp = 0; newEmp < emps.length; newEmp++) {
                    if (emp == newEmp) continue;
                    for (int newDay = 0; newDay < neighbor[0].length; newDay++) {
                        if (day == newDay) continue;
                        if (neighbor[newEmp][newDay] != null && neighbor[emp][newDay] == null && neighbor[newEmp][day] == null &&  neighbor[emp][day].equals(neighbor[newEmp][newDay])) {
                            neighbor[emp][newDay] = neighbor[emp][day];
                            neighbor[emp][day] = null;

                            neighbor[newEmp][day] = neighbor[newEmp][newDay];
                            neighbor[newEmp][newDay] = null;
                            neighbors.add(neighbor);

                            if (neighbors.size() == maxSize) return neighbors;
                            neighbor = copyMatrix(assignments);
                        }
                    }
                }
            }

        }
        return neighbors;
    }

    public Employee[] getEmployeesSortedByPenalties(Shift[][] assignments) {
        Employee[] employees = new Employee[emps.length];
        int[] penalties = new int[emps.length];
        for (Employee emp : emps) {
            int penalty = calcEmpWeeklyPenalty(assignments, emp.getEmpIndex());
            int[] newPenalties = new int[penalties.length];
            Employee[] newEmployees = new Employee[emps.length];
            boolean replaced = false;
            for (int i = 0; i < employees.length; i++) {
                if (penalties[i] <= penalty && !replaced) {
                    newPenalties[i] = penalty;
                    newEmployees[i] = emp;
                    newPenalties[i+1] = penalties[i];
                    newEmployees[i+1] = employees[i];
                    if ((i+1) == employees.length-1) break;
                    replaced = true;
                } else if (replaced && i != employees.length-1) {
                    newPenalties[i+1] = penalties[i];
                    newEmployees[i+1] = employees[i];
                    if ((i+1) == employees.length-1) break;
                } else {
                    newPenalties[i] = penalties[i];
                    newEmployees[i] = employees[i];
                }
            }
            employees = newEmployees;
            penalties = newPenalties;
        }
        // for (int i = 0; i < penalties.length; i++) {
        //     System.out.println("last penalty: " + penalties[i]);
        // }
        
        return employees;
    }

    // belongs to the tabu search related to spreading out the work days over the entire period
    public Shift[][] searchSpread() {
        Shift[][] bestSolution = copyMatrix(initialShiftAssignments);
        Shift[][] currentSolution = copyMatrix(bestSolution);
        ArrayList<Shift[][]> tabuList = new ArrayList<>();

        // Should set a field called "maxIterations" instead of 1000 here
        for (int i = 0; i < 10; i++) {
            ArrayList<Shift[][]> neighbors = findNeighborSpread(currentSolution);
            Shift[][] bestNeighbor = null;
            int bestObjectiveValue = Integer.MAX_VALUE;

            // TODO: Currently spends the majority of the runs on going back and forh on the same values (1325000 and 1324000)
            for (Shift[][] neighbor : neighbors) {
                if (!includes(tabuList, neighbor)) {
                    int newObjVal = objectiveFunctionSpread(neighbor);
                    if (newObjVal < bestObjectiveValue) {
                        bestNeighbor = copyMatrix(neighbor);
                        bestObjectiveValue = newObjVal;
                        System.out.println("new best objective val: " + bestObjectiveValue);
                    }
                }
            }

            if (bestNeighbor == null) {
                break;
            }

            currentSolution = copyMatrix(bestSolution);
            tabuList.add(bestNeighbor);

            // again I need a field "maxTabuSize" for maximum size of tabuList instead of "10000"
            if (tabuList.size() > 10000) {
                tabuList.remove(0);
            }

            if (objectiveFunctionSpread(bestNeighbor) < objectiveFunctionSpread(bestSolution)) {
                bestSolution = copyMatrix(bestNeighbor);
            }
        }
        System.out.println("Final objective value: " + objectiveFunctionSpread(bestSolution));
        return bestSolution;
    }

    public boolean includes(ArrayList<Shift[][]> tabuList, Shift[][] assignments) {
        if (tabuList.isEmpty()) return false;
        for (Shift[][] s : tabuList) {
            for (int i = 0; i < s.length; i++) {
                for (int j = 0; j < s[0].length; j++) {
                    if (s[i][j] != assignments[i][j]) {//(s[i][j] == null && assignments[i][j] != null) || (s[i][j] != null && assignments[i][j] == null) || !s[i][j].equals(assignments[i][j])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Shift[][] copyMatrix(Shift[][] assignments) {
        Shift[][] copy = new Shift[assignments.length][assignments[0].length];
        for (int i = 0; i < assignments.length; i++) {
            for (int j = 0; j < assignments[0].length; j++) {
                copy[i][j] = assignments[i][j];
            }
        }
        return copy;
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

