package mockit.integration.junit4.github173;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountTwillio extends AbstractService {
    private static final Logger LOGGER = Logger.getLogger(AccountTwillio.class.getName());
    private DatabaseRemote maarsDb = null;
    private AccountWhatever reminderServiceAccount = null;
    private HttpClient httpClient = null;
    private Clock clock = null;

    public AccountTwillio(DatabaseRemote mAARSdb, HttpClient httpClient, AccountWhatever reminderServiceAccount,
            Clock clock, long minimumSleepDurationInMs, boolean testModeSettingValue, Environment currentEnvironment) {

        super(minimumSleepDurationInMs, testModeSettingValue, currentEnvironment, 1000L );
        this.clock                  = clock;
        this.maarsDb                = mAARSdb;
        this.reminderServiceAccount = reminderServiceAccount;
        this.httpClient             = httpClient;
    }



}
