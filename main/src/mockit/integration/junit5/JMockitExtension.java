/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit5;

import java.lang.reflect.*;
import javax.annotation.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import mockit.*;
import mockit.integration.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import static mockit.internal.util.StackTrace.*;

@SuppressWarnings("Since15")
final class JMockitExtension extends TestRunnerDecorator implements
   BeforeAllCallback, AfterAllCallback,
   TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback,
   BeforeTestExecutionCallback, AfterTestExecutionCallback,
   ParameterResolver, TestExecutionExceptionHandler
{
   @Nullable private SavePoint savePointForTestClass;
   @Nullable private SavePoint savePointForTest;
   @Nullable private SavePoint savePointForTestMethod;
   @Nullable private Throwable thrownByTest;
   private Object[] parameterValues;

   @Override
   public void beforeAll(@Nonnull ExtensionContext context) {
      if (isRegularTestClass(context)) {
         @Nullable Class<?> testClass = context.getTestClass().orElse(null);
         savePointForTestClass = new SavePoint();
         TestRun.setCurrentTestClass(testClass);
      }
   }

   private static boolean isRegularTestClass(@Nonnull ExtensionContext context) {
      Class<?> testClass = context.getTestClass().orElse(null);
      return testClass != null && !testClass.isAnnotationPresent(Nested.class);
   }

   @Override
   public void postProcessTestInstance(@Nonnull Object testInstance, @Nonnull ExtensionContext context) {
      if (isRegularTestClass(context)) {
         TestRun.enterNoMockingZone();

         try {
            handleMockFieldsForWholeTestClass(testInstance);
         }
         finally {
            TestRun.exitNoMockingZone();
         }

         TestRun.setRunningIndividualTest(testInstance);
      }
   }

   @Override
   public void beforeEach(@Nonnull ExtensionContext context) {
      Object testInstance = context.getTestInstance().orElse(null);

      if (testInstance == null) {
         return;
      }

      TestRun.prepareForNextTest();
      TestRun.enterNoMockingZone();

      try {
         savePointForTest = new SavePoint();
         createInstancesForTestedFieldsBeforeSetup(testInstance);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   public void beforeTestExecution(@Nonnull ExtensionContext context) {
      Method testMethod = context.getTestMethod().orElse(null);
      Object testInstance = context.getTestInstance().orElse(null);

      if (testMethod == null || testInstance == null) {
         return;
      }

      TestRun.enterNoMockingZone();

      try {
         savePointForTestMethod = new SavePoint();
         createInstancesForTestedFieldsFromBaseClasses(testInstance);
         parameterValues = createInstancesForAnnotatedParameters(testInstance, testMethod, null);
         createInstancesForTestedFields(testInstance);
      }
      finally {
         TestRun.exitNoMockingZone();
      }

      TestRun.setRunningIndividualTest(testInstance);
   }

   @Override
   public boolean supportsParameter(@Nonnull ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext) {
      return
         parameterContext.isAnnotated(Tested.class) ||
         parameterContext.isAnnotated(Mocked.class) ||
         parameterContext.isAnnotated(Injectable.class) ||
         parameterContext.isAnnotated(Capturing.class);
   }

   @Override
   public Object resolveParameter(@Nonnull ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext) {
      int parameterIndex = parameterContext.getIndex();
      return parameterValues[parameterIndex];
   }

   @Override
   public void handleTestExecutionException(@Nonnull ExtensionContext context, @Nonnull Throwable throwable) throws Throwable {
      thrownByTest = throwable;
      throw throwable;
   }

   @Override
   public void afterTestExecution(@Nonnull ExtensionContext context) {
      if (savePointForTestMethod != null) {
         TestRun.enterNoMockingZone();

         try {
            savePointForTestMethod.rollback();
            savePointForTestMethod = null;

            if (thrownByTest != null) {
               filterStackTrace(thrownByTest);
            }

            Error expectationsFailure = RecordAndReplayExecution.endCurrentReplayIfAny();
            clearTestedObjectsIfAny();

            if (expectationsFailure != null) {
               filterStackTrace(expectationsFailure);
               throw expectationsFailure;
            }
         }
         finally {
            TestRun.finishCurrentTestExecution();
            TestRun.exitNoMockingZone();
         }
      }
   }

   @Override
   public void afterEach(@Nonnull ExtensionContext context) {
      if (savePointForTest != null) {
         savePointForTest.rollback();
         savePointForTest = null;
      }
   }

   @Override
   public void afterAll(@Nonnull ExtensionContext context) {
      if (savePointForTestClass != null && isRegularTestClass(context)) {
         savePointForTestClass.rollback();
         savePointForTestClass = null;

         clearFieldTypeRedefinitions();
         TestRun.setCurrentTestClass(null);
      }
   }
}
