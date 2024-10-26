package app.program.model;

public class Shift {
    private int startTime;
    private int endTime;

    public Shift(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
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

    public boolean equals(Shift shift1) {
            return (shift1.getStartTime() == this.startTime) && (shift1.getEndTime() == this.endTime);
    }

    public String toString() {
        return startTime + "-" + endTime;
    }
}
