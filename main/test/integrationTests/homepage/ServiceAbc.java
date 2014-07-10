/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.homepage;

public final class ServiceAbc
{
   private final DependencyXyz xyz = new DependencyXyz();

   public Object doOperationAbc(String s)
   {
      xyz.doSomething(s);
      return "";
   }

   public void doAnotherOperation(String s)
   {
      xyz.doSomething(s);
      new AnotherDependency().complexOperation(1, new Object());
   }
}
