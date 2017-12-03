/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.loginExample.domain.userLogin;

import org.junit.*;

import mockit.*;

import jmockit.loginExample.domain.userAccount.*;

/**
 * A small TestNG test suite for a single class (<code>LoginService</code>), based on
 * <a href="http://schuchert.wikispaces.com/Mockito.LoginServiceExample">this article</a>.
 */
public final class LoginServiceJUnitTest
{
   @Tested LoginService service;
   @Mocked UserAccount account;

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
      willMatchPassword(false);

      new Expectations() {{
         UserAccount.find("roger"); result = secondAccount;
         secondAccount.passwordMatches(anyString); result = false;
      }};

      service.login("john", "password");
      service.login("john", "password");
      service.login("roger", "password");

      new Verifications() {{ secondAccount.setRevoked(true); times = 0; }};
   }

   @Test(expected = AccountLoginLimitReachedException.class)
   public void disallowConcurrentLogins() throws Exception
   {
      willMatchPassword(true);

      new Expectations() {{ account.isLoggedIn(); result = true; }};

      service.login("john", "password");
   }

   @Test(expected = UserAccountNotFoundException.class)
   public void throwExceptionIfAccountNotFound() throws Exception
   {
      new Expectations() {{ UserAccount.find("roger"); result = null; }};

      new LoginService().login("roger", "password");
   }

   @Test(expected = UserAccountRevokedException.class)
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
