/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.multicast;

public final class Client
{
   private final String address;

   public Client(String address)
   {
      this.address = address;
   }

   public String getAddress()
   {
      return address;
   }
}
