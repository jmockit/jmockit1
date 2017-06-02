package mockit.integration.junit4.github173;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractService extends AbstractReminderService {

    private final Logger LOGGER = Logger.getLogger(getClass().getName());
    protected boolean isTestMode;
    protected Environment currentEnvironment;

    public AbstractService(long minimumSleepDurationInMs, boolean testModeSettingValue, Environment currentEnvironment, Long period) {
        super(minimumSleepDurationInMs, period);
        this.isTestMode         = testModeSettingValue;
        this.currentEnvironment = currentEnvironment;
    }


}
