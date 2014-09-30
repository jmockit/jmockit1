/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4.internal;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import org.junit.*;
import org.junit.runners.model.*;

import mockit.integration.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.mockups.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class JUnit4TestRunnerDecorator extends TestRunnerDecorator
{
   @Nullable
   Object invokeExplosively(@NotNull MockInvocation invocation, @Nullable Object target, Object... params)
      throws Throwable
   {
      FrameworkMethod it = invocation.getInvokedInstance();

      // A @BeforeClass/@AfterClass method:
      if (target == null) {
         handleMockingOutsideTests(it);
         TestRun.setRunningIndividualTest(null);
         invocation.prepareToProceed();
         return it.invokeExplosively(null, params);
      }

      handleMockingOutsideTestMethods(target);

      // A @Before/@After method:
      if (it.getAnnotation(Test.class) == null) {
         if (shouldPrepareForNextTest && it.getAnnotation(Before.class) != null) {
            discardTestLevelMockedTypes();
            prepareForNextTest();
            shouldPrepareForNextTest = false;
         }

         TestRun.setRunningIndividualTest(target);

         try {
            invocation.prepareToProceed();
            return it.invokeExplosively(target, params);
         }
         catch (Throwable t) {
            RecordAndReplayExecution.endCurrentReplayIfAny();
            StackTrace.filterStackTrace(t);
            throw t;
         }
         finally {
            if (it.getAnnotation(After.class) != null) {
               shouldPrepareForNextTest = true;
            }
         }
      }

      if (shouldPrepareForNextTest) {
         prepareForNextTest();
      }

      shouldPrepareForNextTest = true;

      try {
         executeTestMethod(invocation, target, params);
         return null; // it's a test method, therefore has void return type
      }
      catch (Throwable t) {
         StackTrace.filterStackTrace(t);
         throw t;
      }
      finally {
         TestRun.finishCurrentTestExecution();
      }
   }

   private static void handleMockingOutsideTests(@NotNull FrameworkMethod it)
   {
      Class<?> testClass = it.getMethod().getDeclaringClass();

      TestRun.enterNoMockingZone();

      try {
         Class<?> currentTestClass = TestRun.getCurrentTestClass();

         if (
            currentTestClass != null && testClass.isAssignableFrom(currentTestClass) &&
            it.getAnnotation(AfterClass.class) != null
         ) {
            cleanUpMocksFromPreviousTest();
         }

         if (it.getAnnotation(BeforeClass.class) != null) {
            updateTestClassState(null, testClass);
         }
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private static void handleMockingOutsideTestMethods(@NotNull Object target)
   {
      Class<?> testClass = target.getClass();

      TestRun.enterNoMockingZone();

      try {
         updateTestClassState(target, testClass);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private static void executeTestMethod(
      @NotNull MockInvocation invocation, @NotNull Object target, @Nullable Object... parameters)
      throws Throwable
   {
      SavePoint savePoint = new SavePoint();

      TestRun.setRunningIndividualTest(target);

      FrameworkMethod it = invocation.getInvokedInstance();
      Method testMethod = it.getMethod();
      Throwable testFailure = null;
      boolean testFailureExpected = false;

      try {
         Object[] mockParameters = createInstancesForMockParameters(testMethod, parameters);
         createInstancesForTestedFields(target);

         invocation.prepareToProceed();

         Object[] params = mockParameters == null ? parameters : mockParameters;
         it.invokeExplosively(target, params);
      }
      catch (Throwable thrownByTest) {
         testFailure = thrownByTest;
         Class<?> expectedType = testMethod.getAnnotation(Test.class).expected();
         testFailureExpected = expectedType.isAssignableFrom(thrownByTest.getClass());
      }
      finally {
         concludeTestMethodExecution(savePoint, testFailure, testFailureExpected);
      }
   }
}
