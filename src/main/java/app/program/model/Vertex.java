package app.program.model;

import java.util.ArrayList;

public class Vertex {

    private static int totalVertices = 0;
    private int vertexIndex = -1;
    private ArrayList<Edge> inGoing;
    private ArrayList<Edge> outGoing;
    private int purpose;
    private int day = -1;
    private Employee emp = null;
    private Shift shift = null;
    private int timeOfDay;
    private int dep;
    private int expLvl;
    private String name;
    private int potential = 0;

    public Vertex(int purpose, String name) {
        this.purpose = purpose;
        this.name = name;
        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public Vertex(int purpose, String name, Employee emp) {
        this.purpose = purpose;
        this.emp = emp;
        this.name = name;

        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public Vertex(int purpose, String name, Employee emp, int day) {
        this.purpose = purpose;
        this.emp = emp;
        this.day = day;
        this.name = name;
        
        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public Vertex(int purpose, String name, Employee emp, int day, Shift shift) {
        this.purpose = purpose;
        this.emp = emp;
        this.day = day;
        this.shift = shift;
        this.name = name;

        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public Vertex(int purpose, String name, int dep, int timeOfDay, int day) {
        this.purpose = purpose;
        this.name = name;
        this.dep = dep;
        this.timeOfDay = timeOfDay;
        this.day = day;

        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public Vertex(int purpose, String name, int dep, int timeOfDay, int exp, int day) {
        this.purpose = purpose;
        this.name = name;
        this.dep = dep;
        this.timeOfDay = timeOfDay;
        this.expLvl = exp;
        this.day = day;

        this.inGoing = new ArrayList<>();
        this.outGoing = new ArrayList<>();
        totalVertices++;
        this.vertexIndex = totalVertices-1;
    }

    public void addInGoing(Edge e) {
            this.inGoing.add(e);
        }

    public void addOutGoing(Edge e) {
        this.outGoing.add(e);
    }

    public void setPotential(int potential) {
        this.potential = potential;
    }


    public int getPotential() {
        return potential;
    }
    public ArrayList<Edge> getInGoing() {
        return inGoing;
    }

    public ArrayList<Edge> getOutGoing() {
        return outGoing;
    }

    public int getTotalVertices() {
        return totalVertices;
    }

    public int getVertexIndex() {
        return vertexIndex;
    }

    public Employee getEmp() {
        return emp;
    }
    public Shift getShift() {
        return shift;
    }
    public int getDay() {
        return day;
    }
    public int getPurpose() {
        return purpose;
    }
    public int getDep() {
        return dep;
    }
    public int getExpLvl() {
        return expLvl;
    }

    public int getTimeOfDay() {
        return timeOfDay;
    }
    

    public String toString() {
        if (this.purpose == 4) {
            switch (this.name) {
                case "0_0_1":
                    return "( day_lab_1 )";
                case "0_0_2":
                    return "( day_lab_2 )";
                case "0_1_1":
                    return "( day_mat_1 )";
                case "0_1_2":
                    return "( day_mat_2 )";
                case "1_0_1":
                    return "( eve_lab_1 )";
                case "1_0_2":
                    return "( eve_lab_2 )";
                case "1_1_1":
                    return "( eve_mat_1 )";
                case "1_1_2":
                    return "( eve_mat_2 )";
                case "2_0_1":
                    return "( night_lab_1 )";
                case "2_0_2":
                    return "( night_lab_2 )";
                case "2_1_1":
                    return "( night_mat_1 )";
                case "2_1_2":
                    return "( night_mat_2 )";
            }
        } else if (this.purpose == 5) {
            if (this.name.substring(0,3).equals("0_0")) {
                return "( lab_day"+this.name.substring(3)+" )";
            }
            if (this.name.substring(0,3).equals("0_1")) {
                return "( lab_eve"+this.name.substring(3)+" )";
            }
            if (this.name.substring(0,3).equals("0_2")) {
                return "( lab_night"+this.name.substring(3)+" )";
            }
            if (this.name.substring(0,3).equals("1_0")) {
                return "( mat_day"+this.name.substring(3)+" )";
            }
            if (this.name.substring(0,3).equals("1_1")) {
                return "( mat_eve"+this.name.substring(3)+" )";
            }
            if (this.name.substring(0,3).equals("1_2")) {
                return "( mat_night"+this.name.substring(3)+" )";
            }
        }
        return "( "+ this.name + " )";
    }
}

