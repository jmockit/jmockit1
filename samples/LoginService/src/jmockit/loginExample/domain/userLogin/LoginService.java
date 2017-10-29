/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.loginExample.domain.userLogin;

import javax.annotation.*;

import jmockit.loginExample.domain.userAccount.*;

public final class LoginService
{
   private static final int MAX_LOGIN_ATTEMPTS = 3;

   @Nonnegative private int loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
   @Nullable private String previousAccountId;
   @Nullable private UserAccount account;

   public void login(@Nonnull String accountId, @Nonnull String password)
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
      //noinspection ConstantConditions
      if (account.isLoggedIn()) {
         throw new AccountLoginLimitReachedException();
      }

      if (account.isRevoked()) {
         throw new UserAccountRevokedException();
      }

      account.setLoggedIn(true);
      loginAttemptsRemaining = MAX_LOGIN_ATTEMPTS;
   }

   private void handleFailedLoginAttempt(@Nonnull String accountId)
   {
      if (previousAccountId == null) {
         loginAttemptsRemaining--;
         previousAccountId = accountId;
      }
      else if (accountId.equals(previousAccountId)) {
         loginAttemptsRemaining--;

         if (loginAttemptsRemaining == 0) {
            //noinspection ConstantConditions
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
