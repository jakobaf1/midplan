package app.program.model;
import java.time.LocalDate;

public class Preference {
    private boolean wanted;
    private int prefLvl;
    private LocalDate date;
    private String day;
    private String shift;
    private int repeat;
    private int repeatDuration;

    public Preference(boolean wanted, int prefLvl, LocalDate date, String day, String shift, int repeat, int repeatDuration) {
        this.wanted = wanted;
        this.prefLvl = prefLvl;
        this.date = date;
        this.day = day;
        this.shift = shift;
        this.repeat = repeat;
        this.repeatDuration = repeatDuration;
    }

    public String toString() {
        return "wanted: " + wanted + ", prefLvl: " + prefLvl + ", date: " + date + ", day: " + day + ", shift: " + shift + ", repeat: " + repeat;
    }

    
}
