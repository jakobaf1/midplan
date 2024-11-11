package app.program.model;

public class Shift {
    private int startTime;
    private int endTime;
    private int hours;

    public Shift(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Shift(int startTime, int endTime, int hours) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.hours = hours;
    }


    public boolean validShift(int startTime, int endTime) {
        if (endTime == 7) {
            return (startTime-endTime) >= 11;
        } else {
            return (24 - endTime + startTime) >= 11;
        }
    }

    public int calcHours() {
        if (endTime > startTime) {
            return endTime - startTime;
        } else {
            return 24-startTime + endTime;
        }
    }

    public int getEndTime() {
        return endTime;
    }

    public int getStartTime() {
        return startTime;
    }
    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        if (startTime == 7 && this.hours != 0)  System.out.println("current hours: " + this.hours + " new hours: " + hours);
        this.hours = hours;
    }

    public boolean equals(Shift shift1) {
            return (shift1.getStartTime() == this.startTime) && (shift1.getEndTime() == this.endTime);
    }

    public String toString() {
        return startTime + "-" + endTime;
    }
}
