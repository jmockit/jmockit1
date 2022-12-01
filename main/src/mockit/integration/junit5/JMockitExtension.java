/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit5;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.*;

import mockit.internal.util.Utilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import mockit.*;
import mockit.integration.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import static mockit.internal.util.StackTrace.*;

@SuppressWarnings("Since15")
public final class JMockitExtension extends TestRunnerDecorator implements
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
   private ParamValueInitContext initContext = new ParamValueInitContext(null, null, null,
           "No callbacks have been processed, preventing parameter population");

   @Override
   public void beforeAll(@Nonnull ExtensionContext context) {
      if (isRegularTestClass(context)) {
         @Nullable Class<?> testClass = context.getTestClass().orElse(null);
         savePointForTestClass = new SavePoint();
         TestRun.setCurrentTestClass(testClass);

         if (testClass == null) {
            initContext = new ParamValueInitContext(null, null, null,
                    "@BeforeAll setup failed to acquire 'Class' of test");
            return;
         }

         // @BeforeAll can be used on instance methods depending on @TestInstance(PER_CLASS) usage
         Object testInstance = context.getTestInstance().orElse(null);
         Method beforeAllMethod = Utilities.getAnnotatedDeclaredMethod(testClass, BeforeAll.class);
         if (testInstance == null) {
            initContext = new ParamValueInitContext(null, testClass, beforeAllMethod,
                    "@BeforeAll setup failed to acquire instance of test class");
            return;
         }

         if (beforeAllMethod != null) {
            initContext = new ParamValueInitContext(testInstance, testClass, beforeAllMethod, null);
            parameterValues = createInstancesForAnnotatedParameters(testInstance, beforeAllMethod, null);
         }
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
      Class<?> testClass = context.getTestClass().orElse(null);
      if (testInstance == null) {
         initContext = new ParamValueInitContext(null, null, null,
                 "@BeforeEach setup failed to acquire instance of test class");
         return;
      }

      TestRun.prepareForNextTest();
      TestRun.enterNoMockingZone();

      try {
         savePointForTest = new SavePoint();
         createInstancesForTestedFieldsBeforeSetup(testInstance);

         if (testClass == null) {
            initContext = new ParamValueInitContext(null, null, null,
                    "@BeforeEach setup failed to acquire Class<?> of test");
            return;
         }

         Method beforeEachMethod = Utilities.getAnnotatedDeclaredMethod(testClass, BeforeEach.class);
         if (beforeEachMethod != null) {
            initContext = new ParamValueInitContext(testInstance, testClass, beforeEachMethod, null);
            parameterValues = createInstancesForAnnotatedParameters(testInstance, beforeEachMethod, null);
         }
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   public void beforeTestExecution(@Nonnull ExtensionContext context) {
      Class<?> testClass = context.getTestClass().orElse(null);
      Method testMethod = context.getTestMethod().orElse(null);
      Object testInstance = context.getTestInstance().orElse(null);

      if (testMethod == null || testInstance == null) {
         initContext = new ParamValueInitContext(testInstance, testClass, testMethod,
                 "@Test failed to acquire instance of test class, or target method");
         return;
      }

      TestRun.enterNoMockingZone();

      try {
         savePointForTestMethod = new SavePoint();
         createInstancesForTestedFieldsFromBaseClasses(testInstance);
         initContext = new ParamValueInitContext(testInstance, testClass, testMethod, null);
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
      if (parameterValues == null) {
         String warning = initContext.warning;
         String exceptionMessage = "JMockit failed to provide parameters to JUnit 5 ParameterResolver.";
         if (warning != null) {
            exceptionMessage += "\nAdditional info: " + warning;
         }
         exceptionMessage += "\n - Class: " + initContext.displayClass();
         exceptionMessage += "\n - Method: " +  initContext.displayMethod();
         throw new IllegalStateException(exceptionMessage);
      }
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

   private static class ParamValueInitContext {
      private final Object instance;
      private final Class<?> clazz;
      private final Method method;
      private final String warning;

      ParamValueInitContext(Object instance, Class<?> clazz, Method method, String warning) {
         this.instance = instance;
         this.clazz = clazz;
         this.method = method;
         this.warning = warning;
      }

      boolean isBeforeAllMethod() {
         return method.getDeclaredAnnotation(BeforeAll.class) != null;
      }

      boolean isBeforeEachMethod() {
         return method.getDeclaredAnnotation(BeforeEach.class) != null;
      }

      String displayClass() {
         if (clazz == null) return "<no class reference>";
         return clazz.getName();
      }

      String displayMethod() {
         if (method == null) return "<no method reference>";
         String methodPrefix = isBeforeAllMethod() ? "@BeforeAll " :
                 isBeforeEachMethod() ? "@BeforeEach " : "";
         String args = Arrays.stream(method.getParameterTypes())
                 .map(Class::getName)
                 .collect(Collectors.joining(", "));
         return methodPrefix + method.getName() + "(" + args + ")";
      }

      @Override
      public String toString() {
         return "ParamContext{" +
                 "hasInstance=" + (instance == null ?  "false" : "true")  +
                 ", class=" + clazz +
                 ", method=" + method +
                 '}';
      }
   }
}