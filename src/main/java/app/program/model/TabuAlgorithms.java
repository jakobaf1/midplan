package app.program.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TabuAlgorithms {
    private int[] iniEmpHoursLeft;
    private int[][][] iniMatHoursLeft; // 3d to express days/experience/time of day
    private int[][][] iniLabHoursLeft; // 3d to express days/experience/time of day
    private DistTuple iniTuple;
    private int totalEmpHoursLeft;

    private int hardConstraintsBroken = 0;
    private int softConstraintsBroken = 0;
    private int prefsFulfilled = 0;

    private Employee[] emps;
    private Shift[][] initialShiftAssignments;
    private int initialObjVal;
    private int hardConstraintPenalty = 100000;
    private int softConstraintPenalty = 50;

    Random randomGenerator = new Random();

    private int impossibleHours;

    private FlowGraph fg;
    private ArrayList<ArrayList<Edge>> invalidPaths;

    boolean eightHour = false;

    private  List<List<Preference>[]> allEmpDayPrefs = new ArrayList<>();
    private List<List<Preference>[]> allEmpShiftPrefs = new ArrayList<>();

    public TabuAlgorithms(FlowGraph fg, ArrayList<ArrayList<Edge>> invalidPaths, boolean eightHour) {
        this.fg = fg;
        this.invalidPaths = invalidPaths;
        System.out.println("invalid Shifts: " + invalidPaths.size());
        this.iniEmpHoursLeft = new int[fg.getEmps().length];
        this.iniMatHoursLeft = new int[fg.getDaysInPeriod()][2][3];
        this.iniLabHoursLeft = new int[fg.getDaysInPeriod()][2][3];
        this.eightHour = eightHour;

        this.emps = fg.getEmps();
        this.initialShiftAssignments = new Shift[emps.length][fg.getDaysInPeriod()];
        this.initialObjVal = 0;
        for (int emp = 0; emp < emps.length; emp++) {
            allEmpDayPrefs.add(fg.prefDays(emps[emp], fg.getStartDate(), fg.getDaysInPeriod()));
            allEmpShiftPrefs.add(fg.prefShifts(emps[emp], fg.getStartDate(), fg.getDaysInPeriod()));
        }
        gatherInformation();
        this.iniTuple = new DistTuple(initialShiftAssignments, iniEmpHoursLeft, iniLabHoursLeft, iniMatHoursLeft);

        int hoursWanted = 0;
        int hoursAvailable = 0;
        for (Edge e : fg.getT().getInGoing()) {
            hoursWanted += e.getCap();
        }
        for (Edge e : fg.getS().getOutGoing()) {
            hoursAvailable += e.getCap();
        }
        impossibleHours = hoursWanted-hoursAvailable;
        System.out.println("Hours wanted: " + hoursWanted + ", Hours available: " + hoursAvailable + ", impossibleHours: " + impossibleHours);
    }


    //// Tabu for distributing ////

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public int objectiveFunctionDist(Shift[][] assignments, int[] empHoursLeft) {
        int objectiveVal = 0;
        objectiveVal += calcAssignedHoursPenalty(assignments);
        objectiveVal += calc11HrPenalty(assignments);
        objectiveVal += calcPreferencePenalty(assignments);
        objectiveVal += calcDepPenalty(assignments);
        return objectiveVal;
    }

    public ArrayList<DistTuple> findNeighborDist(DistTuple values) {
        ArrayList<DistTuple> neighbors = new ArrayList<>();
        int maxSize = 100;

        for (int i = 0; i < maxSize; i++) {
            DistTuple neighbor = copyTuple(values);
            int emp = randomGenerator.nextInt(emps.length);
            int runs = 0;
            while(values.empHoursLeft[emp] < 4) {emp = randomGenerator.nextInt(emps.length); runs++; if (runs > 300) break;}
            if (runs > 300) continue;
            Shift shift = fg.getShifts()[randomGenerator.nextInt(fg.getShifts().length)];
            int day = randomGenerator.nextInt(values.assignments[0].length);
            if (values.empHoursLeft[emp] <= 4) {
                runs = 0;
                while(values.assignments[emp][day] == null) {day = randomGenerator.nextInt(values.assignments[0].length);  runs++; if (runs > 400) break;}
                if (runs > 400) continue;
                if (values.assignments[emp][day].calcHours() == 8) {
                    while(shift.calcHours() != 12) {shift = fg.getShifts()[randomGenerator.nextInt(fg.getShifts().length)];}
                } else {
                    // TODO: look into whether this should also give an additional 8-hr assignment
                    while(shift.calcHours() != 8) {shift = fg.getShifts()[randomGenerator.nextInt(fg.getShifts().length)];}
                }
            } else {
                runs = 0;
                while(values.assignments[emp][day] != null) {day = randomGenerator.nextInt(values.assignments[0].length); runs++; if (runs > 300) break;}
                if (runs > 300) continue;
            }
           
            neighbor.assignments[emp][day] = shift;
            neighbors.add(neighbor);
        }
        return neighbors;
    }

    // belongs to the tabu search related to distributing the final 4 hours in invalid shifts
    public Shift[][] searchDist() {
        DistTuple bestSolution = iniTuple;
        DistTuple currentSolution = iniTuple;
        List<DistTuple> tabuList = new ArrayList<>();

        int maxIterations = 1000;
        for (int i = 0; i < maxIterations; i++) {
            ArrayList<DistTuple> neighbors = findNeighborDist(currentSolution);
            DistTuple bestNeighbor = null;
            int bestObjectiveValue = initialObjVal;

            for (DistTuple neighbor : neighbors) {
                if (tabuList.contains(neighbor)) System.out.println("CONTAINS NEIGHBOR");
                if (!tabuList.contains(neighbor)) {
                    int newObjVal = objectiveFunctionDist(neighbor.assignments, neighbor.empHoursLeft);
                    if (newObjVal < bestObjectiveValue) {
                        bestNeighbor = neighbor;
                        bestObjectiveValue = newObjVal;
                    }
                }
            }

            if (bestNeighbor == null) {
                System.out.println("broke DIST after " + i + " iterations");
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
        
        System.out.println("Objective value for distribution: " + objectiveFunctionDist(bestSolution.assignments, bestSolution.empHoursLeft));

        return bestSolution.assignments;
    }



    //// Tabu for spreading ////

    // belongs to the tabu search related to spreading out the work days over the entire period
    public int objectiveFunctionSpread(Shift[][] assignments) {
        int objectiveVal = 0;
        // penalty which penalizes working more or less in a week than one is hired for
        objectiveVal += calcEmpWeeklyHoursPenalty(assignments);
        // the following adds a penalty weight if the break between shifts is too short
        objectiveVal += calc11HrPenalty(assignments);
        // the following adds the preference weights as well as the general constraint break penalties
        objectiveVal += calcPreferencePenalty(assignments);        
        objectiveVal += calcAssignedHoursPenalty(assignments);
        objectiveVal += calcDepPenalty(assignments);
        return objectiveVal;
    }
    
    // belongs to the tabu search related to spreading out the work days over the entire period
    public ArrayList<Shift[][]> findNeighborSpread(Shift[][] assignments) {
        // Find neighbors by exchanging shifts on different days between employees
        ArrayList<Shift[][]> neighbors = new ArrayList<>();
        int maxSize = 40;
        Shift[][] neighbor = copyMatrix(assignments);

        for (int i = 0; i < maxSize; i++) {
            int emp1 = randomGenerator.nextInt(assignments.length);
            int emp2 = randomGenerator.nextInt(assignments.length);

            int day1 = randomGenerator.nextInt(assignments[0].length);
            int day2 = randomGenerator.nextInt(assignments[0].length);

            int runs = 0;
            while ((assignments[emp1][day1] == null && assignments[emp2][day2] == null) || (day1 == day2)) {
                if (assignments[emp1][day1] == null) day1 = randomGenerator.nextInt(assignments[0].length);
                if (assignments[emp2][day2] == null) day2 = randomGenerator.nextInt(assignments[0].length);
                runs++;
                if (runs > 250) break;
            }
            if (runs > 250) continue;
            Shift temp = neighbor[emp1][day1];
            neighbor[emp1][day1] = neighbor[emp2][day2];
            neighbor[emp2][day2] = temp;

            neighbors.add(neighbor);

            neighbor = copyMatrix(assignments);
        }
        return neighbors;
    }

    // belongs to the tabu search related to spreading out the work days over the entire period
    public Shift[][] searchSpread() {
        Shift[][] bestSolution = copyMatrix(initialShiftAssignments);
        Shift[][] currentSolution = copyMatrix(bestSolution);
        ArrayList<Shift[][]> tabuList = new ArrayList<>();

        int maxIterations = 3000;
        for (int i = 0; i < maxIterations; i++) {
            ArrayList<Shift[][]> neighbors = findNeighborSpread(currentSolution);
            Shift[][] bestNeighbor = null;
            int bestObjectiveValue = Integer.MAX_VALUE;

            for (Shift[][] neighbor : neighbors) {
                if (!tabuList.contains(neighbor)) {
                    int newObjVal = objectiveFunctionSpread(neighbor);
                    if (newObjVal < bestObjectiveValue) {
                        bestNeighbor = copyMatrix(neighbor);
                        bestObjectiveValue = newObjVal;
                        // System.out.println("new best objective val: " + bestObjectiveValue);
                    }
                }
            }

            if (bestNeighbor == null) {
                System.out.println("broke SPREAD after " + i + " iterations");
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
        System.out.println("Final objective value for spread: " + objectiveFunctionSpread(bestSolution));
        int weeklyPenalty = calcEmpWeeklyHoursPenalty(bestSolution);
        
        System.out.println("The penalty for spread is: " + weeklyPenalty + ", and preference penalty is: " + calcPreferencePenalty(bestSolution));
        return bestSolution;
    }

    // public int objectiveFunctionSwap(Shift[][] assignments) {
    //     return calc11HrPenalty(assignments) + calcEmpWeeklyHoursPenalty(assignments) + calcPreferencePenalty(assignments); 
    // }

    // public ArrayList<Shift[][]> findNeighborSwap(Shift[][] assignments) {
    //     // Find neighbors by swapping shifts on different days between employees
    //     ArrayList<Shift[][]> neighbors = new ArrayList<>();
    //     int maxSize = 60;
    //     Shift[][] neighbor = copyMatrix(assignments);

    //     for (int i = 0; i < maxSize; i++) {
    //         int emp1 = randomGenerator.nextInt(assignments.length);
    //         int emp2 = randomGenerator.nextInt(assignments.length);
    //         while (emps[emp2].getDepartments() != emps[emp1].getDepartments()) {emp2 = randomGenerator.nextInt(assignments.length);}

    //         int day1 = randomGenerator.nextInt(assignments[0].length);
    //         int day2 = randomGenerator.nextInt(assignments[0].length);

    //         int runs = 0;
    //         while ((assignments[emp1][day1] == null || assignments[emp2][day2] == null) || (day1 == day2)) {
    //             if (assignments[emp1][day1] == null) day1 = randomGenerator.nextInt(assignments[0].length);
    //             if (assignments[emp2][day2] == null) day2 = randomGenerator.nextInt(assignments[0].length);
    //             runs++;
    //             if (runs > 250) break;
    //         }
    //         if (runs > 250) continue;
    //         Shift temp = neighbor[emp1][day1];
    //         neighbor[emp1][day1] = neighbor[emp2][day2];
    //         neighbor[emp2][day2] = temp;

    //         neighbors.add(neighbor);

    //         neighbor = copyMatrix(assignments);
    //     }
    //     return neighbors;
    // }

    // public Shift[][] searchSwap() {
    //     Shift[][] bestSolution = copyMatrix(initialShiftAssignments);
    //     Shift[][] currentSolution = copyMatrix(bestSolution);
    //     ArrayList<Shift[][]> tabuList = new ArrayList<>();

    //     int maxIterations = 4000;
    //     for (int i = 0; i < maxIterations; i++) {
    //         ArrayList<Shift[][]> neighbors = findNeighborSwap(currentSolution);
    //         Shift[][] bestNeighbor = null;
    //         int bestObjectiveValue = Integer.MAX_VALUE;

    //         for (Shift[][] neighbor : neighbors) {
    //             if (!tabuList.contains(neighbor)) {
    //                 int newObjVal = objectiveFunctionSwap(neighbor);
    //                 if (newObjVal < bestObjectiveValue) {
    //                     bestNeighbor = copyMatrix(neighbor);
    //                     bestObjectiveValue = newObjVal;
    //                     // System.out.println("new best objective val: " + bestObjectiveValue);
    //                 }
    //             }
    //         }

    //         if (bestNeighbor == null) {
    //             System.out.println("broke SWAP after " + i + " iterations");
    //             break;
    //         }

    //         currentSolution = copyMatrix(bestSolution);
    //         tabuList.add(bestNeighbor);

    //         // again I need a field "maxTabuSize" for maximum size of tabuList instead of "10000"
    //         if (tabuList.size() > 10000) {
    //             tabuList.remove(0);
    //         }

    //         if (objectiveFunctionSwap(bestNeighbor) < objectiveFunctionSwap(bestSolution)) {
    //             bestSolution = copyMatrix(bestNeighbor);
    //         }
    //     }
    //     System.out.println("Final objective value for swap: " + objectiveFunctionSwap(bestSolution));
    //     int weeklyPenalty = calcEmpWeeklyHoursPenalty(bestSolution);
        
    //     System.out.println("The penalty for spread is: " + weeklyPenalty + ", and preference penalty is: " + calcPreferencePenalty(bestSolution));
    //     return bestSolution;
    // }

    public int[][] assignDepartments(Shift[][] assignments) {
        int[][] deps = new int[assignments.length][assignments[0].length];
        // int hoursNeededDay = 48;
        // int hoursNeededElse = 32;
        for (int emp = 0; emp < assignments.length; emp++) {
            for (int day = 0; day < assignments[0].length; day++) {
                deps[emp][day] = -1;
            }
        }
        // int[][][] givenHours = new int[assignments[0].length][2][3]; // Specifies day/department/timeOfDay
        int[][][] neededHours = new int[assignments[0].length][2][3];
        for (int day = 0; day < assignments[0].length; day++) {
            ArrayList<int[]> hours = getDepHours(assignments, day);
            int[] labHours = hours.get(0);
            neededHours[day][0][0] = 48-labHours[0];
            neededHours[day][0][1] = 32-labHours[1];
            neededHours[day][0][2] = 32-labHours[2];

            int[] matHours = hours.get(1);
            neededHours[day][1][0] = 48-matHours[0];
            neededHours[day][1][1] = 32-matHours[1];
            neededHours[day][1][2] = 32-matHours[2];
            
        }
        for (int emp = 0; emp < assignments.length; emp++) {
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] != null) {
                    if (emps[emp].getDepartments().length > 1) {
                        // ArrayList<int[]> hours = getDepHours(assignments, day);
                        switch (assignments[emp][day].getStartTime()) {
                            case 7:
                                if (assignments[emp][day].calcHours() == 8) {
                                    if (neededHours[day][0][0] - assignments[emp][day].calcHours() >= 0 && neededHours[day][1][0] < neededHours[day][0][0]) {
                                        deps[emp][day] = 0;
                                        // givenHours[day][0][0] += assignments[emp][day].calcHours();
                                        neededHours[day][0][0] -= assignments[emp][day].calcHours();
                                    } else if (neededHours[day][1][0] - assignments[emp][day].calcHours()>= 0) {
                                        deps[emp][day] = 1;
                                        // givenHours[day][1][0] += assignments[emp][day].calcHours();
                                        neededHours[day][1][0] -= assignments[emp][day].calcHours();
                                    } 
                                } else {
                                    if (neededHours[day][0][0] - 8 >= 0 && neededHours[day][0][1] - 4 >= 0 && neededHours[day][1][0] < neededHours[day][0][0]) {
                                        deps[emp][day] = 0;
                                        // givenHours[day][0][0] += 8;
                                        // givenHours[day][0][1] += 4;
                                        neededHours[day][0][0] -= 8;
                                        neededHours[day][0][1] -= 4;
                                    } else if (neededHours[day][1][0] - 8 >= 0 && neededHours[day][1][1] - 4 >= 0) {
                                        deps[emp][day] = 1;
                                        // givenHours[day][1][0] += 8;
                                        // givenHours[day][1][1] += 4;
                                        neededHours[day][1][0] -= 8;
                                        neededHours[day][1][1] -= 4;
                                    } 
                                }
                                break;
                            case 15:
                                if (neededHours[day][0][1] - assignments[emp][day].calcHours() >= 0 && neededHours[day][1][1] < neededHours[day][0][1]) {
                                    deps[emp][day] = 0;
                                    neededHours[day][0][1] -= assignments[emp][day].calcHours();
                                } else if (neededHours[day][1][1] - assignments[emp][day].calcHours() >= 0) {
                                    deps[emp][day] = 1;
                                    neededHours[day][1][1] -= assignments[emp][day].calcHours();
                                }
                                break;
                            case 19:
                                if (neededHours[day][0][2] - 8 >= 0 && neededHours[day][0][1] - 4 >= 0 && neededHours[day][1][1] < neededHours[day][0][1]) {
                                    deps[emp][day] = 0;
                                    // givenHours[day][0][1] += 4;
                                    // givenHours[day][0][2] += 8;
                                    neededHours[day][0][1] -= 4;
                                    neededHours[day][0][2] -= 8;

                                } else if (neededHours[day][1][2] - 8 >= 0 && neededHours[day][1][1] - 4 >= 0) {
                                    deps[emp][day] = 1;
                                    // givenHours[day][1][1] += 4;
                                    // givenHours[day][1][2] += 8;
                                    neededHours[day][1][1] -= 4;
                                    neededHours[day][1][2] -= 8;
                                } 
                                break;
                            case 23:
                                if (neededHours[day][0][2] - assignments[emp][day].calcHours() >= 0 && neededHours[day][1][2] < neededHours[day][0][2]) {
                                    deps[emp][day] = 0;
                                    // givenHours[day][0][2] += assignments[emp][day].calcHours();
                                    neededHours[day][0][2] -= assignments[emp][day].calcHours();
                                } else if (neededHours[day][1][2] - assignments[emp][day].calcHours() >= 0) {
                                    deps[emp][day] = 1;
                                    // givenHours[day][1][2] += assignments[emp][day].calcHours();
                                    neededHours[day][1][2] -= assignments[emp][day].calcHours();
                                }
                                break;
                        
                            default:
                                break;
                        }
                    } else {
                        deps[emp][day] = emps[emp].getDepartments()[0];
                    }
                }
            }
        }
        return deps;
    }

    //////////// Helper functions //////////////
    public int calcEmpWeeklyHoursPenalty(Shift[][] assignments) {
        int penalty = 0;
        // penalty which penalizes working more or less in a week than one is hired for
        for (int emp = 0; emp < assignments.length; emp++) {
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
        }
            
        return penalty;
    }

    public int calcPreferencePenalty(Shift[][] assignments) {
        int penalty = 0;
        int hardConstraints = 0;
        int softConstraints = 0;
        int fulfilled = 0;

        for (int emp = 0; emp < assignments.length; emp++) {
            List<Preference>[] empDayPrefs = allEmpDayPrefs.get(emp);
            List<Preference>[] empShiftPrefs = allEmpShiftPrefs.get(emp);
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] == null) continue;
                if (empDayPrefs[day].isEmpty() && empShiftPrefs[day].isEmpty()) continue;
                for (Preference dailyPref : empDayPrefs[day]) {
                    if (dailyPref.getShift() == null) {
                        // since the employee must have a shift assignment on this day, the weight should be added
                        int w = fg.setDayWeight(empDayPrefs[day]);
                        if (w == Integer.MAX_VALUE) {
                            if (w == Integer.MAX_VALUE) penalty += hardConstraintPenalty;
                            // else penalty -= hardConstraintPenalty/5;
                            hardConstraints++;
                        } else if (Math.abs(w) >= 0 && Math.abs(w) < fg.getBaseEdgeWeight()) {
                            penalty -= (1000 - w); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                            fulfilled++;
                        } else if (w != fg.getBaseEdgeWeight() && w != -fg.getBaseEdgeWeight()) {
                            penalty += (w-fg.getBaseEdgeWeight());
                            softConstraints++;
                        }
                    } else if (assignments[emp][day].equals(dailyPref.getShift())) {
                        int ws = fg.setShiftWeight(empShiftPrefs[day], assignments[emp][day]);
                        if (ws == Integer.MAX_VALUE) {
                            if (ws == Integer.MAX_VALUE) penalty += hardConstraintPenalty;
                            // else penalty -= hardConstraintPenalty/5;
                            hardConstraints++;
                        } else if (Math.abs(ws) >= 0 && Math.abs(ws) < fg.getBaseEdgeWeight()) {
                            penalty -= (1000 - ws); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                            fulfilled++;
                        } else if (ws != fg.getBaseEdgeWeight() && ws != -fg.getBaseEdgeWeight()) {
                            penalty += (ws-fg.getBaseEdgeWeight());
                            softConstraints++;
                        }
                    }
                }
                for (Preference shiftPref : empShiftPrefs[day]) {
                    if (shiftPref.getDay() == -1 && shiftPref.getShift().equals(assignments[emp][day])) {
                        // check the preferences vs. the given shift assignment
                        int ws = fg.setShiftWeight(empShiftPrefs[day], assignments[emp][day]);
                        if (ws == Integer.MAX_VALUE) {
                            if (ws == Integer.MAX_VALUE) penalty += hardConstraintPenalty;
                            // else penalty -= hardConstraintPenalty/5;
                            hardConstraints++;
                        } else if (Math.abs(ws) >= 0 && Math.abs(ws) < fg.getBaseEdgeWeight()) {
                            penalty -= (1000 - ws); // reflects that prefLvl 1 = 0 represents -1000 in weight and prefLvl 5 = 5 represents -5
                            fulfilled++;
                        } else if (ws != fg.getBaseEdgeWeight() && ws != -fg.getBaseEdgeWeight()) {
                            penalty += (ws-fg.getBaseEdgeWeight());
                            softConstraints++;
                        }
                    }
                } 
                
            }
        }
        hardConstraintsBroken = hardConstraints;
        softConstraintsBroken = softConstraints;
        prefsFulfilled = fulfilled;
        return penalty; // Note: multiplying this by a factor makes the employee penalty have more focus, but more hours are mismatched
    }

    public int calc11HrPenalty(Shift[][] assignments) {
        int penalty = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            for (int day = 0; day < assignments[0].length; day++) {
                // int dayBeforeEndTime = -1;
                // if (day != 0 && assignments[emp][day-1] != null) dayBeforeEndTime = assignments[emp][day-1].getEndTime();
                // boolean shiftRule = (dayBeforeEndTime == -1 || assignments[emp][day].validShift(assignments[emp][day].getStartTime(), dayBeforeEndTime));
                // if (!shiftRule) penalty += hardConstraintPenalty;
                if (day > 0) {
                    int endTime = assignments[emp][day-1] != null ? assignments[emp][day-1].getEndTime() : -1;
                    int startTime = assignments[emp][day] != null ? assignments[emp][day].getStartTime() : -1;
                    if (endTime == -1 || startTime == -1) continue;
                    if (!assignments[emp][day].validShift(startTime, endTime)) penalty += hardConstraintPenalty;
                }
            }
        }
        return penalty;
    }

    public int calcAssignedHoursPenalty(Shift[][] assignments) {
        int penalty = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            int empHoursLeft = emps[emp].getWeeklyHrs()*(fg.getDaysInPeriod()/7);
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] != null) empHoursLeft -= assignments[emp][day].calcHours();
            }
            if (empHoursLeft > 0) penalty += empHoursLeft*(hardConstraintPenalty/60); // apply penalty to objective value if hours are unassigned
            if (empHoursLeft < 0) penalty += (-empHoursLeft)*(hardConstraintPenalty/8); // apply penalty to objective value if too many hours are assigned
            if (empHoursLeft > 8 || -empHoursLeft > 8) penalty += hardConstraintPenalty;
        }
        return penalty;
    }

    public int calcDepPenalty(Shift[][] assignments) {
        int penalty = 0;
        int hoursNeededDay = 48;
        int hoursNeededElse = 32;
        for (int day = 0; day < assignments[0].length; day++) {
            ArrayList<int[]> hours = getDepHours(assignments, day);
            int[] labHours = hours.get(0);
            int[] matHours = hours.get(1);
            int[] mixHours = hours.get(2);
            if (labHours[0] > hoursNeededDay) penalty += hardConstraintPenalty;
            if (labHours[1] > hoursNeededElse) penalty += hardConstraintPenalty;
            if (labHours[2] > hoursNeededElse) penalty += hardConstraintPenalty;
            if (matHours[0] > hoursNeededDay) penalty += hardConstraintPenalty;
            if (matHours[1] > hoursNeededElse) penalty += hardConstraintPenalty;
            if (matHours[2] > hoursNeededElse) penalty += hardConstraintPenalty;

            if (labHours[0] + mixHours[0] < hoursNeededDay) penalty += hardConstraintPenalty/25;
            if (matHours[0] + (mixHours[0]-labHours[0]) < hoursNeededDay) penalty += hardConstraintPenalty/25;

            if (labHours[1] + mixHours[1] < hoursNeededElse) penalty += hardConstraintPenalty/25;
            if (matHours[1] + (mixHours[1]-labHours[1]) < hoursNeededElse) penalty += hardConstraintPenalty/25;

            if (labHours[2] + mixHours[2] < hoursNeededElse) penalty += hardConstraintPenalty/25;
            if (matHours[2] + (mixHours[2]-labHours[2]) < hoursNeededElse) penalty += hardConstraintPenalty/25;

        }
        return penalty;
    }

    public ArrayList<int[]> getDepHours(Shift[][] assignments, int day) {
        int[] labHours = new int[3]; // each element corresponds to a time of day (day,eve,night)
        int[] matHours = new int[3];
        int[] mixHours = new int[3];
        for (int emp = 0; emp < assignments.length; emp++) {
            if (assignments[emp][day] == null) continue;
            switch (assignments[emp][day].getStartTime()) {
                case 7:
                    if (emps[emp].getDepartments().length > 1) {
                        mixHours[0] += 8;
                        if (assignments[emp][day].calcHours() == 12) mixHours[1] += 4;
                    } else if (emps[emp].getDepartments()[0] == 0) { // lab
                        labHours[0] += 8;
                        if (assignments[emp][day].calcHours() == 12) labHours[1] += 4;
                    } else { // mat
                        matHours[0] += 8;
                        if (assignments[emp][day].calcHours() == 12) matHours[1] += 4;
                    }
                    break;
                case 15:
                    if (emps[emp].getDepartments().length > 1) {
                        mixHours[1] += 8;
                    } else if (emps[emp].getDepartments()[0] == 0) { // lab
                        labHours[1] += 8;
                    } else { // mat
                        matHours[1] += 8;
                    }
                    break;
                case 19:
                    if (emps[emp].getDepartments().length > 1) {
                        mixHours[1] += 4;
                        mixHours[2] += 8;
                    } else if (emps[emp].getDepartments()[0] == 0) { // lab
                        labHours[1] += 4;
                        labHours[2] += 8;
                    } else { // mat
                        matHours[1] += 4;
                        matHours[2] += 8;
                    }
                    break;
                case 23:
                    if (emps[emp].getDepartments().length > 1) {
                        mixHours[2] += 8;
                    } else if (emps[emp].getDepartments()[0] == 0) { // lab
                        labHours[2] += 8;
                    } else { // mat
                        matHours[2] += 8;
                    }
                    break;
                default:
                    break;
            }
        }
        ArrayList<int[]> hours = new ArrayList<>();
        hours.add(labHours);
        hours.add(matHours);
        hours.add(mixHours);
        return hours;
    }

    // public int calcExpPenalty(Shift[][] assignments) {
    //     int penalty = 0;
        
    //     return penalty;
    // }
    // set the initial values/conditions based on the given flow model
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
            // TODO: If I change the flowgraph, I could also just read this in the end from the flow available to the employee
            iniEmpHoursLeft[emp.getEmpIndex()] += minFlow;
            
            // update flowgraph
            for (Edge e : le) {
                e.addFlow(-minFlow);
            }
        }

        // update which paths are invalid (shift version)
        fg.clearInvalidPaths();
        if (!eightHour) fg.updateInvalidPaths(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0, 2);
        invalidPaths = fg.getInvalidPaths();
        System.out.println("new size of invalidPaths: " + invalidPaths.size());

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
            // TODO: If I change the flowgraph, I could also just read this in the end from the flow available to the employee
            iniEmpHoursLeft[emp.getEmpIndex()] += minFlow;
            
            // update flowgraph
            for (Edge e : le) {
                e.addFlow(-minFlow);
            }
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

        for (int i = 0; i < iniEmpHoursLeft.length; i++) {
            totalEmpHoursLeft += iniEmpHoursLeft[i];
        }

        // define initial objective value
        // initialObjVal = objectiveFunctionDist(initialShiftAssignments, iniEmpHoursLeft);
        initialObjVal = objectiveFunctionSpread(initialShiftAssignments);
        System.out.println("Initial objective value (spread): " + initialObjVal);
        System.out.println("Initial objective value (dist): " + objectiveFunctionDist(initialShiftAssignments, iniEmpHoursLeft));
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

    public Shift[][] copyMatrix(Shift[][] assignments) {
            Shift[][] copy = new Shift[assignments.length][assignments[0].length];
            for (int i = 0; i < assignments.length; i++) {
                for (int j = 0; j < assignments[0].length; j++) {
                    copy[i][j] = assignments[i][j];
                }
            }
            return copy;
        }

    // may be removed
    
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

    // public Employee[] getEmployeesSortedByPenalties(Shift[][] assignments) {
    //     Employee[] employees = new Employee[emps.length];
    //     int[] penalties = new int[emps.length];
    //     for (Employee emp : emps) {
    //         int penalty = calcEmpWeeklyHoursPenalty(assignments, emp.getEmpIndex());
    //         int[] newPenalties = new int[penalties.length];
    //         Employee[] newEmployees = new Employee[emps.length];
    //         boolean replaced = false;
    //         for (int i = 0; i < employees.length; i++) {
    //             if (penalties[i] <= penalty && !replaced) {
    //                 newPenalties[i] = penalty;
    //                 newEmployees[i] = emp;
    //                 newPenalties[i+1] = penalties[i];
    //                 newEmployees[i+1] = employees[i];
    //                 if ((i+1) == employees.length-1) break;
    //                 replaced = true;
    //             } else if (replaced && i != employees.length-1) {
    //                 newPenalties[i+1] = penalties[i];
    //                 newEmployees[i+1] = employees[i];
    //                 if ((i+1) == employees.length-1) break;
    //             } else {
    //                 newPenalties[i] = penalties[i];
    //                 newEmployees[i] = employees[i];
    //             }
    //         }
    //         employees = newEmployees;
    //         penalties = newPenalties;
    //     }
    //     // for (int i = 0; i < penalties.length; i++) {
    //     //     System.out.println("last penalty: " + penalties[i]);
    //     // }
        
    //     return employees;
    // }

    

    // print statements
    public void printShiftAssignments(Shift[][] assignments) {
        int mismatchedHours = 0;
        int hoursLacking = 0;
        int hoursInAbundance = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            int empHoursLeft = emps[emp].getWeeklyHrs()*(fg.getDaysInPeriod()/7);
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] != null) empHoursLeft -= assignments[emp][day].calcHours();
            }
            if (empHoursLeft > 0) mismatchedHours += empHoursLeft; // apply penalty to objective value if hours are unassigned
            if (empHoursLeft < 0) mismatchedHours += (-empHoursLeft); // apply penalty to objective value if too many hours are assigned
            if (empHoursLeft > 0 && empHoursLeft > hoursLacking) hoursLacking = empHoursLeft;
            if (empHoursLeft < 0 && empHoursLeft < hoursInAbundance) hoursInAbundance = empHoursLeft;
        }
        System.out.println("Total hours mismatched: " + mismatchedHours + ", largest lackinghours: " + hoursLacking + " hours, largest hours in abundance: " + (-hoursInAbundance));

        for (int emp = 0; emp < assignments.length; emp++) {
            int totalHours = 0;
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] != null) totalHours += assignments[emp][day].calcHours();
            }
            System.out.println(emps[emp] + ": " + totalHours + "/" + emps[emp].getWeeklyHrs()*(fg.getDaysInPeriod()/7));
            LocalDate date = fg.getStartDate();
            int week = -1;
            for (int day = 0; day < assignments[0].length; day++) {
                if (week == -1 || week != fg.getStartDate().plusDays(day).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) {
                    week = fg.getStartDate().plusDays(day).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    System.out.println("\tWeek " + week + ":");
                }
                if (assignments[emp][day] != null) {
                    date = fg.getStartDate().plusDays(day);
                    String dayOfWeek = fg.weekdayToString(date.getDayOfWeek().getValue());

                    System.out.println("\tday " + day + ", " + dayOfWeek + ": " + assignments[emp][day]);
                    
                }
            }
        }

    }

    public void saveShiftAssignmentsToFile(Shift[][] assignments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("shift_assignments.txt"))) {
            int mismatchedHours = 0;
            int hoursLacking = 0;
            int hoursInAbundance = 0;
            int[][] deps = assignDepartments(assignments);
            // Calculate mismatched hours and max mismatch
            for (int emp = 0; emp < assignments.length; emp++) {
                int empHoursLeft = emps[emp].getWeeklyHrs() * (fg.getDaysInPeriod() / 7);
                for (int day = 0; day < assignments[0].length; day++) {
                    if (assignments[emp][day] != null) empHoursLeft -= assignments[emp][day].calcHours();
                }
                if (empHoursLeft > 0) mismatchedHours += empHoursLeft;
                if (empHoursLeft < 0) mismatchedHours += (-empHoursLeft);
                if (empHoursLeft > 0 && empHoursLeft > hoursLacking) hoursLacking = empHoursLeft;
                if (empHoursLeft < 0 && empHoursLeft < hoursInAbundance) hoursInAbundance = empHoursLeft;
            }

            writer.write("Total hours mismatched: " + mismatchedHours + ", largest lackinghours: " + hoursLacking + " hours, largest hours in abundance: " + (-hoursInAbundance) + "\n");

            // Write details for each employee
            for (int emp = 0; emp < assignments.length; emp++) {
                int totalHours = 0;
                for (int day = 0; day < assignments[0].length; day++) {
                    if (assignments[emp][day] != null) totalHours += assignments[emp][day].calcHours();
                }
                writer.write(emps[emp] + ": " + totalHours + "/" + emps[emp].getWeeklyHrs() * (fg.getDaysInPeriod() / 7) + "\n");

                LocalDate date = fg.getStartDate();
                int week = -1;
                for (int day = 0; day < assignments[0].length; day++) {
                    if (week == -1 || week != fg.getStartDate().plusDays(day).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) {
                        week = fg.getStartDate().plusDays(day).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                        writer.write("\tWeek " + week + ":\n");
                    }
                    if (assignments[emp][day] != null) {
                        date = fg.getStartDate().plusDays(day);
                        String dayOfWeek = fg.weekdayToString(date.getDayOfWeek().getValue());
                        String department = "-1";
                        if (deps[emp][day] != -1) department = deps[emp][day] == 0 ? "Labor" : "Maternity";
                        writer.write("\tday " + day + ", " + dayOfWeek + ": " + assignments[emp][day] + ", " + department + "\n");
                    }
                }
            }
            int[][][] depHoursAssigned = new int[assignments[0].length][2][3];
            for (int emp = 0; emp < assignments.length; emp++) {
                for (int day = 0; day < assignments[0].length; day++) {
                    if (assignments[emp][day] != null) {
                        switch (assignments[emp][day].getStartTime()) {
                            case 7:
                                if (assignments[emp][day].calcHours() == 8) {
                                    if (deps[emp][day] == 0) {
                                        depHoursAssigned[day][0][0] += 8;
                                    } else if (deps[emp][day] == 1) {
                                        depHoursAssigned[day][1][0] += 8;
                                    }
                                } else {
                                    if (deps[emp][day] == 0) {
                                        depHoursAssigned[day][0][0] += 8;
                                        depHoursAssigned[day][0][1] += 4;
                                    } else if (deps[emp][day] == 1) {
                                        depHoursAssigned[day][1][0] += 8;
                                        depHoursAssigned[day][1][1] += 4;
                                    }
                                }
                                break;
                            case 15:
                                if (deps[emp][day] == 0) {
                                    depHoursAssigned[day][0][1] += 8;
                                } else if (deps[emp][day] == 1) {
                                    depHoursAssigned[day][1][1] += 8;
                                }
                                break;
                            case 19:
                                if (deps[emp][day] == 0) {
                                    depHoursAssigned[day][0][1] += 4;
                                    depHoursAssigned[day][0][2] += 8;
                                } else if (deps[emp][day] == 1) {
                                    depHoursAssigned[day][1][1] += 4;
                                    depHoursAssigned[day][1][2] += 8;
                                }
                                break;
                            case 23:
                                if (deps[emp][day] == 0) {
                                    depHoursAssigned[day][0][2] += 8;
                                } else if (deps[emp][day] == 1) {
                                    depHoursAssigned[day][1][2] += 8;
                                }
                                break;
                        
                            default:
                                break;
                        }
                    }
                }
            }
            for (int day = 0; day < assignments[0].length; day++) {
                writer.write("day " + day + ", Labor:  day = " + depHoursAssigned[day][0][0] + "/48, eve = " + depHoursAssigned[day][0][1] + "/32, night = " + depHoursAssigned[day][0][2] + "/32\n");
                writer.write("day " + day + ", Maternity:  day = " + depHoursAssigned[day][1][0] + "/48, eve = " + depHoursAssigned[day][1][1] + "/32, night = " + depHoursAssigned[day][1][2] + "/32\n");
            }
            
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }




    // Getters
    public Shift[][] getInitialShiftAssignments() {
        return initialShiftAssignments;
    }

    public int getHardConstraintPenalty() {
        return hardConstraintPenalty;
    }

    public int[] getPrefData() {
        return new int[] {hardConstraintsBroken, softConstraintsBroken, prefsFulfilled};
    }

    // Setters
    public void setAssignments(Shift[][] assignments) {
        this.initialShiftAssignments = assignments;
        this.iniTuple.assignments = assignments;
    }
}



class DistTuple {

    public Shift[][] assignments;
    public int[] empHoursLeft;
    public int[][][] matHoursLeft; // 3d to express days/experience level/time of day
    public int[][][] labHoursLeft; // 3d to express days/experience level/time of day
    public int totalEmpHoursLeft;
    

    public DistTuple(Shift[][] assignments, int[] empHoursLeft, int[][][] labHoursLeft, int[][][] matHoursLeft) {
        this.assignments = assignments;
        this.empHoursLeft = empHoursLeft;
        this.matHoursLeft = matHoursLeft;
        this.labHoursLeft = labHoursLeft;
        for (int i = 0; i < empHoursLeft.length; i++) {
            totalEmpHoursLeft += empHoursLeft[i];
        }
    }
    
    // @Override
    // public boolean equals(Object values) {
    //     for (int i = 0; i < assignments.length; i++) {
    //         if (this.empHoursLeft[i] != values.empHoursLeft[i]) return false;
    //         for (int j = 0; j < assignments[0].length; j++) {

    //         }
    //     }

    //     return true;
    // }

}

