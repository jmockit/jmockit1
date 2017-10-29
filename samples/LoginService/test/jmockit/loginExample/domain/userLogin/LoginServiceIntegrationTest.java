/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
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
   public void setUpOneAccountToBeFound()
   {
      userId = "john";
      userPassword = "password";
      account = new UserAccount(userId, userPassword);

      new Expectations(UserAccount.class) {{ UserAccount.find(userId); result = account; minTimes = 0; }};
   }

   @Test
   public void setAccountToLoggedInWhenPasswordMatches() throws Exception
   {
      // Failed login attempts are inconsequential, provided they don't exceed the maximum number of attempts.
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");
      service.login(userId, userPassword);
      service.login(userId, "wrong password");

      assertTrue(account.isLoggedIn());
      assertFalse(account.isRevoked());
   }

   @Test
   public void setAccountToRevokedAfterThreeFailedLoginAttempts() throws Exception
   {
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");
      service.login(userId, "wrong password");

      assertFalse(account.isLoggedIn());
      assertTrue(account.isRevoked());
   }

   @Test
   public void notRevokeSecondAccountAfterTwoFailedAttemptsOnFirstAccount() throws Exception
   {
      UserAccount secondAccount = new UserAccount("roger", "password");
      new Expectations() {{ UserAccount.find("roger"); result = secondAccount; }};

      service.login(account.getId(), "wrong password");
      service.login(account.getId(), "wrong password");
      service.login(secondAccount.getId(), "wrong password");

      assertFalse(secondAccount.isRevoked());
   }

   @Test(expectedExceptions = AccountLoginLimitReachedException.class)
   public void disallowConcurrentLogins() throws Exception
   {
      account.setLoggedIn(true);

      service.login(userId, userPassword);
   }

   @Test(expectedExceptions = UserAccountNotFoundException.class)
   public void throwExceptionIfAccountNotFound() throws Exception
   {
      new Expectations() {{ UserAccount.find("roger"); result = null; }};

      service.login("roger", "password");
   }

   @Test(expectedExceptions = UserAccountRevokedException.class)
   public void disallowLoggingIntoRevokedAccount() throws Exception
   {
      account.setRevoked(true);

      service.login(userId, userPassword);
   }
}
