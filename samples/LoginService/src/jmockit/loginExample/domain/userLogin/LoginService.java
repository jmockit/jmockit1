/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.loginExample.domain.userLogin;

import jmockit.loginExample.domain.userAccount.*;

public final class LoginService
{
   private static final int MAX_LOGIN_ATTEMPTS = 3;

   private int loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
   private String previousAccountId;
   private UserAccount account;

   public void login(String accountId, String password)
      throws UserAccountNotFoundException, UserAccountRevokedException, AccountLoginLimitReachedException
   {
      account = UserAccount.find(accountId);

      if (account == null) {
         throw new UserAccountNotFoundException();
      }

      if (account.passwordMatches(password)) {
         registerNewLogin();
      }
      else {
         handleFailedLoginAttempt(accountId);
      }
   }

   private void registerNewLogin() throws AccountLoginLimitReachedException, UserAccountRevokedException
   {
      if (account.isLoggedIn()) {
         throw new AccountLoginLimitReachedException();
      }

      if (account.isRevoked()) {
         throw new UserAccountRevokedException();
      }

      account.setLoggedIn(true);
      loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
   }

   private void handleFailedLoginAttempt(String accountId)
   {
      if (previousAccountId == null) {
         loginAttemptsRemaining--;
         previousAccountId = accountId;
      }
      else if (accountId.equals(previousAccountId)) {
         loginAttemptsRemaining--;

         if (loginAttemptsRemaining == 0) {
            account.setRevoked(true);
            loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
         }
      }
      else {
         loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
         previousAccountId = accountId;
      }
   }
}
