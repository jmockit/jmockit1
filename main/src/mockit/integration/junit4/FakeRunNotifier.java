/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import javax.annotation.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

import mockit.*;
import mockit.coverage.testRedundancy.*;
import mockit.integration.*;
import mockit.internal.faking.*;
import mockit.internal.state.TestRun;

/**
 * Startup fake which works in conjunction with {@link JUnit4TestRunnerDecorator} to provide JUnit 4.5+ integration.
 * <p/>
 * This class is not supposed to be accessed from user code. JMockit will automatically load it at startup.
 */
public final class FakeRunNotifier extends MockUp<RunNotifier>
{
   @Mock
   public static void fireTestRunStarted(Invocation invocation, Description description) {
      RunNotifier it = invocation.getInvokedInstance();
      assert it != null;

      TestCoverage testCoverage = TestCoverage.INSTANCE;

      if (testCoverage != null) {
         it.addListener(new JUnitListener(testCoverage));
      }

      prepareToProceed(invocation);
      it.fireTestRunStarted(description);
   }

   private static void prepareToProceed(@Nonnull Invocation invocation) {
      ((FakeInvocation) invocation).prepareToProceedFromNonRecursiveMock();
   }

   @Mock
   public static void fireTestStarted(Invocation invocation, Description description) {
      Class<?> currentTestClass = TestRun.getCurrentTestClass();

      if (currentTestClass != null) {
         Class<?> newTestClass = description.getTestClass();

         if (newTestClass == null || !currentTestClass.isAssignableFrom(newTestClass)) {
            TestRunnerDecorator.cleanUpMocksFromPreviousTestClass();
         }
      }

      prepareToProceed(invocation);

      RunNotifier it = invocation.getInvokedInstance();
      assert it != null;
      it.fireTestStarted(description);
   }

   @Mock
   public static void fireTestRunFinished(Invocation invocation, Result result) {
      TestRun.enterNoMockingZone();

      try {
         TestRunnerDecorator.cleanUpAllMocks();

         prepareToProceed(invocation);

         RunNotifier it = invocation.getInvokedInstance();
         assert it != null;
         it.fireTestRunFinished(result);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}