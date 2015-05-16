/**
 *
 */
package mockit.integration.junit4.github173;

import java.util.Date;


public abstract class AbstractReminderService implements IReminderService {
    private final Long period;
    private final long minimumSleepDurationInMs;

    public AbstractReminderService(long minimumSleepDurationInMs, Long period) {
        this.minimumSleepDurationInMs = minimumSleepDurationInMs;
        this.period                   = period;
    }


}
