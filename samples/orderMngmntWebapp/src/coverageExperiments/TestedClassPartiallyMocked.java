/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package coverageExperiments;

public final class TestedClassPartiallyMocked
{
   public int doSomething(String s)
   {
      int i = doSomethingElse();
      return i + s.length();
   }

   public int doSomethingElse()
   {
      System.out.println("Something else");
      return 123;
   }
}
