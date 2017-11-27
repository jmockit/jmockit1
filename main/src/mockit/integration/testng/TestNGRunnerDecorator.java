/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import java.lang.reflect.*;
import javax.annotation.*;

import org.testng.*;
import org.testng.annotations.*;

import org.testng.internal.Invoker;
import org.testng.internal.Parameters;

import mockit.*;
import mockit.coverage.*;
import mockit.coverage.testRedundancy.*;
import mockit.integration.internal.*;
import mockit.internal.faking.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.state.TestRun;
import static mockit.internal.util.StackTrace.*;
import static mockit.internal.util.Utilities.*;

/**
 * Provides callbacks to be called by the TestNG 6.2+ test runner for each test execution.
 * JMockit will then assert any expectations recorded in {@link Expectations} subclasses during the test.
 * <p/>
 * This class is not supposed to be accessed from user code; it will be automatically loaded at startup.
 */
public final class TestNGRunnerDecorator extends TestRunnerDecorator
   implements IInvokedMethodListener, IExecutionListener
{
   public static final class FakeParameters extends MockUp<Parameters>
   {
      @Mock
      public static void checkParameterTypes(
         String methodName, Class<?>[] parameterTypes, String methodAnnotation, String[] parameterNames) {}

      @Mock
      @Nullable
      public static Object getInjectedParameter(
         @Nonnull Invocation invocation, @Nonnull Class<?> c, @Nullable Method method,
         ITestContext context, ITestResult testResult)
      {
         ((FakeInvocation) invocation).prepareToProceedFromNonRecursiveMock();
         //noinspection deprecation
         Object value = Parameters.getInjectedParameter(c, method, context, testResult);

         if (value != null) {
            return value;
         }

         if (method == null) {
            // Test execution didn't reach a test method yet.
            return null;
         }

         if (method.getParameterTypes().length == 0) {
            // A test method was reached, but it has no parameters.
            return null;
         }

         if (isMethodWithParametersProvidedByTestNG(method)) {
            // The test method has parameters, but they are to be provided by TestNG, not JMockit.
            return null;
         }

         // It's a mock parameter in a test method, to be provided by JMockit.
         return Deencapsulation.newUninitializedInstance(c);
      }

      @Mock
      public static Object[] injectParameters(Object[] parameterValues, Method method, ITestContext context)
      {
         return TestNGRunnerDecorator.injectParameters(parameterValues, method);
      }
   }

   private static boolean isMethodWithParametersProvidedByTestNG(@Nonnull Method method)
   {
      if (method.isAnnotationPresent(org.testng.annotations.Parameters.class)) {
         return true;
      }

      Test testMetadata = method.getAnnotation(Test.class);

      return testMetadata != null && !testMetadata.dataProvider().isEmpty();
   }

   public static final class FakeInvoker extends MockUp<Invoker>
   {
      @Mock
      public static Object[] injectParameters(
         Object[] parameterValues, Method method, ITestContext context, ITestResult testResult)
      {
         return TestNGRunnerDecorator.injectParameters(parameterValues, method);
      }
   }

   static Object[] injectParameters(Object[] parameterValues, Method method)
   {
      if (method == null) {
         return parameterValues;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      int numParameters = parameterTypes.length;

      if (numParameters == 0) {
         return parameterValues;
      }

      if (isMethodWithParametersProvidedByTestNG(method)) {
         return parameterValues;
      }

      Object[] mockValues = new Object[numParameters];

      for (int i = 0; i < numParameters; i++) {
         Class<?> parameterType = parameterTypes[i];
         mockValues[i] = Deencapsulation.newUninitializedInstance(parameterType);
      }

      return mockValues;
   }

   @Nonnull private final ThreadLocal<SavePoint> savePoint;

   public TestNGRunnerDecorator()
   {
      savePoint = new ThreadLocal<SavePoint>();
   }

   @Override
   public void beforeInvocation(@Nonnull IInvokedMethod invokedMethod, @Nonnull ITestResult testResult)
   {
      ITestNGMethod testNGMethod = testResult.getMethod();
      Class<?> testClass = testResult.getTestClass().getRealClass();

      TestRun.clearNoMockingZone();

      if (!invokedMethod.isTestMethod()) {
         beforeConfigurationMethod(testNGMethod, testClass);
         return;
      }

      Method method = testNGMethod.getConstructorOrMethod().getMethod();

      if (Metrics.DataCoverage.active) {
         TestCoverage.INSTANCE.setCurrentTestMethod(method);
      }

      Object testInstance = testResult.getInstance();

      if (testInstance == null || testInstance.getClass() != testClass) {
         // Happens when TestNG is running a JUnit test class, for which "TestResult#getInstance()" erroneously returns
         // a org.junit.runner.Description object.
         return;
      }

      TestRun.enterNoMockingZone();

      try {
         updateTestClassState(testInstance, testClass);
         TestRun.setRunningIndividualTest(testInstance);

         SavePoint testMethodSavePoint = new SavePoint();
         savePoint.set(testMethodSavePoint);

         if (shouldPrepareForNextTest) {
            TestRun.prepareForNextTest();
            shouldPrepareForNextTest = false;
            clearTestedObjectsCreatedDuringSetup();
         }

         createInstancesForTestedFieldsFromBaseClasses(testInstance);

         if (!isMethodWithParametersProvidedByTestNG(method)) {
            Object[] parameters = testResult.getParameters();
            Object[] mockParameters = createInstancesForAnnotatedParameters(testInstance, method, parameters);

            if (mockParameters != null) {
               System.arraycopy(mockParameters, 0, parameters, 0, parameters.length);
            }
         }

         createInstancesForTestedFields(testInstance, false);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private void beforeConfigurationMethod(@Nonnull ITestNGMethod method, @Nonnull Class<?> testClass)
   {
      TestRun.enterNoMockingZone();

      try {
         updateTestClassState(null, testClass);

         if (method.isBeforeMethodConfiguration()) {
            if (shouldPrepareForNextTest) {
               discardTestLevelMockedTypes();
               clearTestedObjectsCreatedDuringSetup();
            }

            Object testInstance = method.getInstance();
            updateTestClassState(testInstance, testClass);

            if (shouldPrepareForNextTest) {
               prepareForNextTest();
               shouldPrepareForNextTest = false;
               createInstancesForTestedFields(testInstance, true);
            }

            TestRun.setRunningIndividualTest(testInstance);
         }
         else if (method.isAfterClassConfiguration()) {
            TestRun.getExecutingTest().setRecordAndReplay(null);
            cleanUpMocksFromPreviousTest();
            TestRun.clearCurrentTestInstance();
         }
         else if (!method.isAfterMethodConfiguration() && !method.isBeforeClassConfiguration()) {
            TestRun.getExecutingTest().setRecordAndReplay(null);
            cleanUpMocksFromPreviousTestClass();
            TestRun.clearCurrentTestInstance();
            TestRun.setCurrentTestClass(null);
         }
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   public void afterInvocation(@Nonnull IInvokedMethod invokedMethod, @Nonnull ITestResult testResult)
   {
      if (!invokedMethod.isTestMethod()) {
         afterConfigurationMethod(testResult);
         return;
      }

      if (Metrics.DataCoverage.active) {
         TestCoverage.INSTANCE.setCurrentTestMethod(null);
      }

      SavePoint testMethodSavePoint = savePoint.get();

      if (testMethodSavePoint == null) {
         return;
      }

      TestRun.enterNoMockingZone();
      shouldPrepareForNextTest = true;
      savePoint.set(null);

      Throwable thrownByTest = testResult.getThrowable();

      try {
         if (thrownByTest == null) {
            concludeTestExecutionWithNothingThrown(testMethodSavePoint, testResult);
         }
         else if (thrownByTest instanceof TestException) {
            concludeTestExecutionWithExpectedExceptionNotThrown(invokedMethod, testMethodSavePoint, testResult);
         }
         else if (testResult.isSuccess()) {
            concludeTestExecutionWithExpectedExceptionThrown(testMethodSavePoint, testResult, thrownByTest);
         }
         else {
            concludeTestExecutionWithUnexpectedExceptionThrown(testMethodSavePoint, testResult, thrownByTest);
         }
      }
      finally {
         TestRun.finishCurrentTestExecution();
         TestRun.clearCurrentTestInstance();
      }
   }

   private static void afterConfigurationMethod(@Nonnull ITestResult testResult)
   {
      TestRun.enterNoMockingZone();

      try {
         ITestNGMethod method = testResult.getMethod();

         if (method.isAfterMethodConfiguration()) {
            Throwable thrownAfterTest = testResult.getThrowable();

            if (thrownAfterTest != null) {
               filterStackTrace(thrownAfterTest);
            }
         }
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private static void concludeTestExecutionWithNothingThrown(
      @Nonnull SavePoint testMethodSavePoint, @Nonnull ITestResult testResult)
   {
      clearTestMethodArguments(testResult);

      try {
         concludeTestMethodExecution(testMethodSavePoint, null, false);
      }
      catch (Throwable t) {
         filterStackTrace(t);
         testResult.setThrowable(t);
         testResult.setStatus(ITestResult.FAILURE);
      }
   }

   private static void clearTestMethodArguments(@Nonnull ITestResult testResult)
   {
      Method method = testResult.getMethod().getConstructorOrMethod().getMethod();

      if (!isMethodWithParametersProvidedByTestNG(method)) {
         testResult.setParameters(NO_ARGS);
      }
   }

   private static void concludeTestExecutionWithExpectedExceptionNotThrown(
      @Nonnull IInvokedMethod invokedMethod, @Nonnull SavePoint testMethodSavePoint, @Nonnull ITestResult testResult)
   {
      clearTestMethodArguments(testResult);

      try {
         concludeTestMethodExecution(testMethodSavePoint, null, false);
      }
      catch (Throwable t) {
         filterStackTrace(t);

         if (isExpectedException(invokedMethod, t)) {
            testResult.setThrowable(null);
            testResult.setStatus(ITestResult.SUCCESS);
         }
         else {
            filterStackTrace(testResult.getThrowable());
         }
      }
   }

   private static void concludeTestExecutionWithExpectedExceptionThrown(
      @Nonnull SavePoint testMethodSavePoint, @Nonnull ITestResult testResult, @Nonnull Throwable thrownByTest)
   {
      clearTestMethodArguments(testResult);
      filterStackTrace(thrownByTest);

      try {
         concludeTestMethodExecution(testMethodSavePoint, thrownByTest, true);
      }
      catch (Throwable t) {
         if (t != thrownByTest) {
            filterStackTrace(t);
            testResult.setThrowable(t);
            testResult.setStatus(ITestResult.FAILURE);
         }
      }
   }

   private static void concludeTestExecutionWithUnexpectedExceptionThrown(
      @Nonnull SavePoint testMethodSavePoint, @Nonnull ITestResult testResult, @Nonnull Throwable thrownByTest)
   {
      clearTestMethodArguments(testResult);
      filterStackTrace(thrownByTest);

      try {
         concludeTestMethodExecution(testMethodSavePoint, thrownByTest, false);
      }
      catch (Throwable ignored) {}
   }

   private static boolean isExpectedException(@Nonnull IInvokedMethod invokedMethod, @Nonnull Throwable thrownByTest)
   {
      Method testMethod = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
      Class<?>[] expectedExceptions = testMethod.getAnnotation(Test.class).expectedExceptions();
      Class<? extends Throwable> thrownExceptionType = thrownByTest.getClass();

      for (Class<?> expectedException : expectedExceptions) {
         if (expectedException.isAssignableFrom(thrownExceptionType)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void onExecutionStart()
   {
      if (Startup.initializeIfPossible()) {
         new FakeParameters();
         new FakeInvoker();
      }
   }

   @Override
   public void onExecutionFinish()
   {
      TestRun.enterNoMockingZone();

      try {
         TestRunnerDecorator.cleanUpAllMocks();
      }
      finally {
         // Maven Surefire, somehow, runs these methods twice per test run.
         TestRun.clearNoMockingZone();
      }
   }
}
