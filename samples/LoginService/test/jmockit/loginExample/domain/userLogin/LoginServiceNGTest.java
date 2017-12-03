/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.loginExample.domain.userLogin;

import org.testng.annotations.*;

import mockit.*;

import jmockit.loginExample.domain.userAccount.*;

/**
 * A small TestNG test suite for a single class (<code>LoginService</code>), based on
 * <a href="http://schuchert.wikispaces.com/Mockito.LoginServiceExample">this article</a>.
 */
public final class LoginServiceNGTest
{
   @Tested LoginService service;
   @Mocked UserAccount account;

   /**
    * This test is redundant, as it exercises the same path as the last test.
    * It cannot simply be removed, because the last test does not perform the "account.setLoggedIn(true)" verification;
    * if said verification is added there, however, then this test could be removed without weakening the test suite.
    */
   @Test
   public void setAccountToLoggedInWhenPasswordMatches() throws Exception
   {
      willMatchPassword(true);

      service.login("john", "password");

      new Verifications() {{ account.setLoggedIn(true); }};
   }

   void willMatchPassword(boolean... matches)
   {
      new Expectations() {{ account.passwordMatches(anyString); result = matches; }};
   }

   @Test
   public void setAccountToRevokedAfterThreeFailedLoginAttempts() throws Exception
   {
      willMatchPassword(false);

      for (int i = 0; i < 3; i++) {
         service.login("john", "password");
      }

      new Verifications() {{ account.setRevoked(true); }};
   }

   /**
    * This test is also redundant, as it exercises the same path as the previous one.
    * Again, it cannot simply be removed since the previous test does not verify that "account.setLoggedIn(true)" is
    * never called; if said verification is added there, however, this test could safely be removed.
    */
   @Test
   public void notSetAccountLoggedInIfPasswordDoesNotMatch() throws Exception
   {
      willMatchPassword(false);

      service.login("john", "password");

      new Verifications() {{ account.setLoggedIn(true); times = 0; }};
   }

   @Test
   public void notRevokeSecondAccountAfterTwoFailedAttemptsOnFirstAccount(@Mocked UserAccount secondAccount)
      throws Exception
   {
      new Expectations() {{
         UserAccount.find("roger"); result = secondAccount;
         secondAccount.passwordMatches(anyString); result = false;
      }};

      service.login("john", "password");
      service.login("john", "password");
      service.login("roger", "password");

      new Verifications() {{ secondAccount.setRevoked(true); times = 0; }};
   }

   @Test(expectedExceptions = AccountLoginLimitReachedException.class)
   public void disallowConcurrentLogins() throws Exception
   {
      willMatchPassword(true);

      new Expectations() {{ account.isLoggedIn(); result = true; }};

      service.login("john", "password");
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
      willMatchPassword(true);

      new Expectations() {{ account.isRevoked(); result = true; }};

      service.login("john", "password");
   }

   @Test
   public void resetBackToInitialStateAfterSuccessfulLogin() throws Exception
   {
      willMatchPassword(false, false, true, false);

      service.login("john", "password");
      service.login("john", "password");
      service.login("john", "password");
      service.login("john", "password");

      new Verifications() {{ account.setRevoked(true); times = 0; }};
   }
}
