package app.program.model;
import java.time.LocalDate;

public class Preference {
    private boolean wanted;
    private int prefLvl;
    private LocalDate date = null;
    private int day = -1;
    private Shift shift;
    private int repeat = -1;
    private int repeatDuration = -1;

    public Preference() {

    }

    public Preference(boolean wanted, int prefLvl, LocalDate date, int day, Shift shift, int repeat, int repeatDuration) {
        this.wanted = wanted;
        this.prefLvl = prefLvl;
        this.date = date;
        this.day = day;
        this.shift = shift;
        this.repeat = repeat;
        this.repeatDuration = repeatDuration;
    }

    public boolean getWanted() {
        return wanted;
    }

    public int getPrefLvl() {
        return prefLvl;
    }

    public Shift getShift() {
        return shift;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getRepeat() {
        return repeat;
    }
    public int getDay() {
        return day;
    }

    public void setPrefLvl(int lvl) {
        this.prefLvl = lvl;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setWanted(boolean wanted) {
        this.wanted = wanted;
    }

    public String toString() {
        return "wanted: " + wanted + ", prefLvl: " + prefLvl + ", date: " + date + ", day: " + day + ", shift: " + shift + ", repeat: " + repeat;
    }

    
}
