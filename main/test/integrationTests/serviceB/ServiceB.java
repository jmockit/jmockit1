/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.serviceB;

public final class ServiceB
{
   private final String config;

   static
   {
      // Do something inconvenient for testing, like connect to a database.
      System.out.println("Static initialization for ServiceB performed");
   }

   public ServiceB(String config)
   {
      this.config = config;
   }

   public int computeX(int a, int b)
   {
      // Instead of a simple sum, assume that a complex calculation is performed, perhaps accessing
      // a database or another external resource.
      return a + b;
   }

   public String getConfig()
   {
      return config;
   }

   public String findItem(String... values)
   {
      StringBuilder result = new StringBuilder();

      for (String value : values) {
         result.append(value);
      }

      return result.toString();
   }

   public static class Helper
   {
      Helper()
      {
         // do nothing
      }
   }
}
