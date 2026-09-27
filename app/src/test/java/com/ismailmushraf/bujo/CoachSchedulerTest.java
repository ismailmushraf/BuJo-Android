package com.ismailmushraf.bujo;

import com.ismailmushraf.bujo.coach.CoachScheduler;
import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class CoachSchedulerTest {
    private long at(int day,int hour,int minute) {
        Calendar c=Calendar.getInstance(); c.clear(); c.set(2026,Calendar.SEPTEMBER,day,hour,minute);
        return c.getTimeInMillis();
    }
    @Test public void morningFourHourSlotsAndEvening() {
        assertEquals(at(27,8,0),CoachScheduler.nextTime(at(27,7,0),8,21));
        assertEquals(at(27,12,0),CoachScheduler.nextTime(at(27,8,0),8,21));
        assertEquals(at(27,16,0),CoachScheduler.nextTime(at(27,13,20),8,21));
        assertEquals(at(27,21,0),CoachScheduler.nextTime(at(27,20,0),8,21));
        assertEquals(at(28,8,0),CoachScheduler.nextTime(at(27,21,0),8,21));
    }
    @Test public void narrowWakingWindow() {
        assertEquals(at(27,10,0),CoachScheduler.nextTime(at(27,9,0),8,10));
        assertEquals(at(28,8,0),CoachScheduler.nextTime(at(27,23,59),8,10));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidHoursRejected() {
        CoachScheduler.nextTime(at(27,12,0),20,8);
    }
    @Test public void daylightSavingStillUsesLocalMorning() {
        TimeZone previous=TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Calendar c=Calendar.getInstance(); c.clear(); c.set(2026,Calendar.OCTOBER,31,22,0);
            long next=CoachScheduler.nextTime(c.getTimeInMillis(),8,20);
            c.setTimeInMillis(next);
            assertEquals(1,c.get(Calendar.DAY_OF_MONTH));
            assertEquals(8,c.get(Calendar.HOUR_OF_DAY));
        } finally { TimeZone.setDefault(previous); }
    }
}
