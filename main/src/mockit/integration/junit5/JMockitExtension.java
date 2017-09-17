/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit5;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import mockit.*;
import mockit.integration.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import static mockit.internal.util.StackTrace.*;

@SuppressWarnings("Since15")
final class JMockitExtension extends TestRunnerDecorator implements
   BeforeAllCallback, AfterAllCallback,
   TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback,
   BeforeTestExecutionCallback, AfterTestExecutionCallback,
   ParameterResolver, TestExecutionExceptionHandler
{
   @Nonnull private final Field indexField;
   @Nullable private SavePoint savePointForTestClass;
   @Nullable private SavePoint savePointForTest;
   @Nullable private SavePoint savePointForTestMethod;
   @Nullable private Throwable thrownByTest;
   @Nullable private Object[] mockParameters;

   JMockitExtension()
   {
      // Somehow, "Parameter#index" is not exposed in the Java API.
      try { indexField = Parameter.class.getDeclaredField("index"); }
      catch (NoSuchFieldException e) { throw new RuntimeException(e); }
   }

   @Override
   public void beforeAll(@Nonnull ExtensionContext context)
   {
      @Nullable Class<?> testClass = context.getTestClass().orElse(null);

      if (testClass == null || isRegularTestClass(testClass)) {
         savePointForTestClass = new SavePoint();
         TestRun.setCurrentTestClass(testClass);
      }
   }

   @Override
   public void postProcessTestInstance(@Nonnull Object testInstance, @Nonnull ExtensionContext context)
   {
      if (isRegularTestClass(testInstance)) {
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

   private static boolean isRegularTestClass(@Nonnull Class<?> testClass)
   {
      return !testClass.isAnnotationPresent(Nested.class);
   }

   private static boolean isRegularTestClass(@Nonnull Object testInstance)
   {
      Class<?> testClass = testInstance.getClass();
      return isRegularTestClass(testClass);
   }

   private static boolean isRegularTestClass(@Nonnull ExtensionContext context)
   {
      Optional<Class<?>> testClass = context.getTestClass();
      return testClass.isPresent() && isRegularTestClass(testClass.get());
   }

   @Override
   public void beforeEach(@Nonnull ExtensionContext context)
   {
      Optional<Object> testInstance = context.getTestInstance();

      if (!testInstance.isPresent()) {
         return;
      }

      @Nonnull Object instance = testInstance.get();

      if (isRegularTestClass(instance)) {
         TestRun.prepareForNextTest();
         TestRun.enterNoMockingZone();

         try {
            savePointForTest = new SavePoint();
            createInstancesForTestedFields(instance, true);
         }
         finally {
            TestRun.exitNoMockingZone();
         }
      }
   }

   @Override
   public void beforeTestExecution(@Nonnull ExtensionContext context)
   {
      Optional<Method> testMethod = context.getTestMethod();

      if (!testMethod.isPresent()) {
         return;
      }

      Optional<Object> testInstance = context.getTestInstance();

      if (!testInstance.isPresent()) {
         return;
      }

      @Nonnull Object instance = testInstance.get();

      if (isRegularTestClass(instance)) {
         @Nonnull Method method = testMethod.get();
         TestRun.enterNoMockingZone();

         try {
            savePointForTestMethod = new SavePoint();
            mockParameters = createInstancesForAnnotatedParameters(instance, method, null);
            createInstancesForTestedFields(instance, false);
         }
         finally {
            TestRun.exitNoMockingZone();
         }

         TestRun.setRunningIndividualTest(instance);
      }
   }

   @Override
   public boolean supportsParameter(
      @Nonnull ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext)
   {
      if (!isRegularTestClass(extensionContext)) {
         return false;
      }

      @Nonnull Parameter parameter = parameterContext.getParameter();

      return
         parameter.isAnnotationPresent(Tested.class) ||
         parameter.isAnnotationPresent(Mocked.class) ||
         parameter.isAnnotationPresent(Injectable.class) ||
         parameter.isAnnotationPresent(Capturing.class);
   }

   @Override
   public Object resolveParameter(
      @Nonnull ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext)
   {
      @Nonnull Parameter parameter = parameterContext.getParameter();
      Integer parameterIndex = FieldReflection.getFieldValue(indexField, parameter);
      //noinspection ConstantConditions
      return mockParameters[parameterIndex];
   }

   @Override
   public void handleTestExecutionException(
      @Nonnull ExtensionContext context, @Nonnull Throwable throwable) throws Throwable
   {
      if (isRegularTestClass(context)) {
         thrownByTest = throwable;
      }

      throw throwable;
   }

   @Override
   public void afterTestExecution(@Nonnull ExtensionContext context)
   {
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
   public void afterEach(@Nonnull ExtensionContext context)
   {
      if (savePointForTest != null) {
         savePointForTest.rollback();
         savePointForTest = null;
      }
   }

   @Override
   public void afterAll(@Nonnull ExtensionContext context)
   {
      if (savePointForTestClass != null && isRegularTestClass(context)) {
         savePointForTestClass.rollback();
         savePointForTestClass = null;

         clearFieldTypeRedefinitions();
         TestRun.setCurrentTestClass(null);
      }
   }
}
