/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;

public final class ShutdownTest
{
   @Test
   public void addShutdownHookToExerciseSUTAfterTestRunHasFinished()
   {
      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() { exerciseSUT(); }
      });
   }

   void exerciseSUT()
   {
      new ClassNotExercised().doSomething(123, "not to be counted");
      ClassWithNestedClasses.doSomething();
   }
}
