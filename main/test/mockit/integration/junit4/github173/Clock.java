package mockit.integration.junit4.github173;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.ejb.Stateless;

@Stateless
public class Clock {
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
