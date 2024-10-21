package app.program.model;

public class Employee {
    private static int totalEmployees = 0;
    private int empIndex = -1;

    private String name;
    private String id;
    private int[] departments;
    private int weeklyHrs;
    private int expLvl;
    private Preference[] pref;

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

    public String toString() {
        String s = "name: " + name + ", id: " + id + ", dep: [";
        for (int i = 0; i < departments.length; i++) {
            if (departments[i] == 0) {
                s += "labor";
            } else if (departments[i] == 1) {
                s += "maternity";
            }
            if (i == departments.length-1) {
                s += "], ";
            } else {
                s += ", ";
            }
        }
        s += "weeklyHrs: " + weeklyHrs + ", expLvl: " + expLvl + "\n";
        if (pref.length != 0) {
            for (Preference p : pref) {
                s += "\t"+p+"\n";
            }
        }

        return s;
    }
}
