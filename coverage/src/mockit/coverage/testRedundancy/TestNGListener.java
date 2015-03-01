/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.testRedundancy;

import org.testng.*;

public final class TestNGListener implements IInvokedMethodListener
{
   @Override
   public void beforeInvocation(IInvokedMethod method, ITestResult testResult)
   {
      if (method.isTestMethod()) {

      }
   }

   @Override
   public void afterInvocation(IInvokedMethod method, ITestResult testResult)
   {
      if (method.isTestMethod()) {

      }
   }
}
