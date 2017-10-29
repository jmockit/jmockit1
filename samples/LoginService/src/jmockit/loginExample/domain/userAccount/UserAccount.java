/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.loginExample.domain.userAccount;

import javax.annotation.*;

public final class UserAccount
{
   private final String id;
   private String password;
   private boolean loggedIn;
   private boolean revoked;

   public UserAccount(String id, String password)
   {
      this.id = id;
      this.password = password;
   }

   public String getId() { return id; }

   public void setPassword(String password) { this.password = password; }

   public boolean isLoggedIn() { return loggedIn; }
   public void setLoggedIn(boolean value) { loggedIn = value; }

   public boolean isRevoked() { return revoked; }
   public void setRevoked(boolean value) { revoked = value; }

   public boolean passwordMatches(String candidatePassword)
   {
      return password.equals(candidatePassword);
   }

   public static UserAccount find(@Nonnull String accountId)
   {
      throw new UnsupportedOperationException("Not implemented");
   }
}
