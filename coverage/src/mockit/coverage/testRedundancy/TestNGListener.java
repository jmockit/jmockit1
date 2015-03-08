/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.testRedundancy;

import java.lang.reflect.*;

import org.testng.*;

public final class TestNGListener implements IInvokedMethodListener
{
   @Override
   public void beforeInvocation(IInvokedMethod method, ITestResult testResult)
   {
      if (method.isTestMethod()) {
         Method testMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
         TestCoverage.INSTANCE.setCurrentTestMethod(testMethod);
      }
   }

   @Override
   public void afterInvocation(IInvokedMethod method, ITestResult testResult)
   {
      if (method.isTestMethod()) {
         TestCoverage.INSTANCE.setCurrentTestMethod(null);
      }
   }
}
