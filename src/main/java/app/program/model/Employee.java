package app.program.model;

import java.util.ArrayList;
import java.util.List;

public class Employee {
    private static int totalEmployees = 0;
    private int empIndex = -1;

    private String name;
    private String id;
    private int[] departments;
    private int weeklyHrs;
    private int expLvl;
    private Preference[] pref;
    private List<Shift>[] shifts;

    public Employee(String name, String id, int[] departments, int weeklyHrs, int expLvl, Preference[] pref) {
        this.name = name;
        this.id = id;
        this.departments = departments;
        this.weeklyHrs = weeklyHrs;
        this.expLvl = expLvl;
        this.pref = pref;
        totalEmployees++;
        empIndex = totalEmployees -1;

    }

    public void addPref(Preference[] pref) {
        this.pref = pref;
    }

    public void addShift(int day, Shift shift) {
        if (!shifts[day].contains(shift)) shifts[day].add(shift);
    }

    public List<Shift>[] getShifts() {
        return shifts;
    }

    public void createShifts(int days) {
        this.shifts = new ArrayList[days];
        for (int i = 0; i < this.shifts.length; i++) {
            this.shifts[i] = new ArrayList<>();
        }
    }

    public int getExpLvl() {
        return expLvl;
    }
    public int[] getDepartments() {
        return departments;
    }
    public Preference[] getPref() {
        return pref;
    }
    public int getWeeklyHrs() {
        return weeklyHrs;
    }
    public String getName() {
        return name;
    }
    public int getEmpIndex() {
        return empIndex;
    }
    public String getID() {
        return id;
    }
    public int getTotalEmployees() {
        return totalEmployees;
    }

    public boolean equals(Employee e) {
        return this.name == e.getName() && this.id == e.getID() && this.departments == e.getDepartments() && 
        this.weeklyHrs == e.getWeeklyHrs() && this.expLvl == e.getExpLvl() && this.pref == e.getPref() && this.empIndex == e.getEmpIndex();
    }

//     public String toString() {
//         String s = "name: " + name + ", id: " + id + ", dep: [";
//         for (int i = 0; i < departments.length; i++) {
//             if (departments[i] == 0) {
//                 s += "labor";
//             } else if (departments[i] == 1) {
//                 s += "maternity";
//             }
//             if (i == departments.length-1) {
//                 s += "], ";
//             } else {
//                 s += ", ";
//             }
//         }
//         s += "weeklyHrs: " + weeklyHrs + ", expLvl: " + expLvl + "\n";
//         if (pref.length != 0) {
//             for (Preference p : pref) {
//                 s += "\t"+p+"\n";
//             }
//         }

//         return s;
//     }
    public String toString() {
        return name;
    }
}

