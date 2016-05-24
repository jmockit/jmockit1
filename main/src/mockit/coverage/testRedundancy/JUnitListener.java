/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.testRedundancy;

import java.lang.reflect.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

@SuppressWarnings("unused")
public final class JUnitListener extends RunListener
{
   @Override
   public void testStarted(Description description)
   {
      if (description.isTest()) {
         Class<?> testClass = description.getTestClass();
         String testMethodName = description.getMethodName();

         for (Method testMethod : testClass.getDeclaredMethods()) {
            if (testMethod.getName().equals(testMethodName)) {
               TestCoverage.INSTANCE.setCurrentTestMethod(testMethod);
               return;
            }
         }
      }
   }

   @Override
   public void testFinished(Description description)
   {
      if (description.isTest()) {
         TestCoverage.INSTANCE.setCurrentTestMethod(null);
      }
   }
}
