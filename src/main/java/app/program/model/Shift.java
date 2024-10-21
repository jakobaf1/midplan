package app.program.model;

public class Shift {
    private int startTime;
    private int endTime;

    public Shift(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static boolean sameShift(Shift shift1, Shift shift2) {
        return (shift1.getStartTime() == shift2.getStartTime()) && (shift1.getEndTime() == shift2.getEndTime());
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

    public String toString() {
        return startTime + "-" + endTime;
    }
}
