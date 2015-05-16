package mockit.integration.junit4.github173;

import java.util.ArrayList;
import java.util.Date;

import mockit.Delegate;
import mockit.Injectable;
import mockit.Invocation;
import mockit.NonStrictExpectations;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;

@RunWith(JMockit.class)
public class GITHUB_ISSUE_173_parameter_names_not_available_Test {

    @Injectable
    private DatabaseRemote mAARSdb;

    @Injectable
    private HttpClient httpClient;

    @Injectable
    private AccountWhatever reminderServiceAccount;

    @Injectable
    private Clock clock;

    @Injectable
    private long minimumSleepDurationInMs;

    @Injectable
    private boolean testModeSettingValue;

    @Injectable
    private Environment currentEnvironment;

    @Tested
    private AccountTwillio accountTwillio;

    public GITHUB_ISSUE_173_parameter_names_not_available_Test() {
    }


    @Test
    public void test() throws Exception {


    }


}
