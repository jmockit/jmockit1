package jmockit.loginExample.domain.userLogin;

import org.testng.annotations.*;

import static org.testng.Assert.*;

import mockit.*;

import jmockit.loginExample.domain.userAccount.*;

/**
 * Equivalent to {@link LoginServiceNGTest}, but with minimal mocking and no redundant tests.
 */
public final class LoginServiceIntegrationTest
{
   @Tested LoginService service;
   String userId;
   String userPassword;
   UserAccount account;

   @BeforeMethod
   public void setUpOneAccountToBeFound() {
      userId = "john";
      userPassword = "password";
      account = new UserAccount(userId, userPassword);

      new MockUp<UserAccount>() { @Mock UserAccount find(String accountId) { return account; } };
   }

   @Test
   public void setAccountToLoggedInWhenPasswordMatches() throws Exception {
      // Failed login attempts are inconsequential, provided they don't exceed the maximum number of attempts.
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");
      service.login(userId, userPassword);
      service.login(userId, "wrong password");

      assertTrue(account.isLoggedIn());
      assertFalse(account.isRevoked());
   }

   @Test
   public void setAccountToRevokedAfterThreeFailedLoginAttempts() throws Exception {
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");

      assertFalse(account.isLoggedIn());
      assertTrue(account.isRevoked());
   }

   @Test
   public void notRevokeSecondAccountAfterTwoFailedAttemptsOnFirstAccount() throws Exception {
      UserAccount secondAccount = new UserAccount("roger", "password");
      String accountId = account.getId();
      account = secondAccount;

      service.login(accountId, "wrong password");
      service.login(accountId, "wrong password");
      service.login(secondAccount.getId(), "wrong password");

      assertFalse(secondAccount.isRevoked());
   }

   @Test(expectedExceptions = AccountLoginLimitReachedException.class)
   public void disallowConcurrentLogins() throws Exception {
      account.setLoggedIn(true);

      service.login(userId, userPassword);
   }

   @Test(expectedExceptions = UserAccountNotFoundException.class)
   public void throwExceptionIfAccountNotFound() throws Exception {
      account = null;

      service.login("roger", "password");
   }

   @Test(expectedExceptions = UserAccountRevokedException.class)
   public void disallowLoggingIntoRevokedAccount() throws Exception {
      account.setRevoked(true);

      service.login(userId, userPassword);
   }
}
