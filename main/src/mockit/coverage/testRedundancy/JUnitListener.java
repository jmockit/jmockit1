/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.testRedundancy;

import java.lang.reflect.*;

import javax.annotation.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

public final class JUnitListener extends RunListener
{
   @Nonnull private final TestCoverage testCoverage;

   public JUnitListener(@Nonnull TestCoverage testCoverage) { this.testCoverage = testCoverage; }

   @Override
   public void testStarted(@Nonnull Description description) {
      if (description.isTest()) {
         Class<?> testClass = description.getTestClass();
         String testMethodName = description.getMethodName();

         for (Method testMethod : testClass.getDeclaredMethods()) {
            if (testMethod.getName().equals(testMethodName)) {
               testCoverage.setCurrentTestMethod(testMethod);
               return;
            }
         }
      }
   }

   @Override
   public void testFinished(@Nonnull Description description) {
      if (description.isTest()) {
         testCoverage.setCurrentTestMethod(null);
      }
   }
}