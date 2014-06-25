package android.text.format;

import com.google.caliper.SimpleBenchmark;

/**
 * Created by nfuller on 6/25/14.
 */
public class TimeBenchmark extends SimpleBenchmark {

    private Time time;

    @Override
    protected void setUp() {
        time = new Time();
        time.monthDay = 22;
        time.month = 6;
        time.year = 1977;
        time.hour = 8;
        time.minute = 20;
        time.second = 59;
        time.normalize(true /* ignoreDst */);
    }

    public void timeConstructor_TimeZone_UTC(int reps) {
        for (int i = 0; i < reps; i++) {
            new Time("UTC");
        }
    }

    public void timeConstructor_TimeZone_Berlin(int reps) {
        for (int i = 0; i < reps; i++) {
            new Time("Europe/Berlin");
        }
    }

    public void timeConstructor(int reps) {
        for (int i = 0; i < reps; i++) {
            new Time();
        }
    }

    public void timeConstructor_Copy(int reps) {
        Time toCopy = time;

        for (int i = 0; i < reps; i++) {
            new Time(toCopy);
        }
    }

    public void timeNormalize_false(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.normalize(false);
        }
    }

    public void timeNormalize_true(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.normalize(false);
        }
    }

    public void timeToString(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.toString();
        }
    }

    public void timeFormat2445(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.format2445();
        }
    }

    // TODO format()

    public void timeParse(int reps) {
        for (int i = 0; i < reps; i++) {
            time.parse("20081013T160000Z");
        }
    }

    public void timeParse3339(int reps) {
        for (int i = 0; i < reps; i++) {
            time.parse3339("2008-10-13T16:00:00.000-07:00");
        }
    }

    public void timeSwitchTimeZone(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.switchTimezone("Europe/London");
            time.switchTimezone("America/Los_Angeles");
        }
    }

    // TODO getActualMaximum

    public void timeCompare(int reps) {
        Time time = this.time;
        Time time2 = new Time(time);
        for (int i = 0; i < reps; i++) {
            Time.compare(time, time2);
        }
    }

    public void timeSetToNow(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.setToNow();
        }
    }

    public void timeToMillis_true(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.toMillis(true);
        }
    }

    public void timeToMillis_false(int reps) {
        Time time = this.time;
        for (int i = 0; i < reps; i++) {
            time.toMillis(false);
        }
    }

    public void timeSet(int reps) {
        long millis = System.currentTimeMillis();
        Time time = new Time();
        for (int i = 0; i < reps; i++) {
            time.set(millis);
        }
    }


}
