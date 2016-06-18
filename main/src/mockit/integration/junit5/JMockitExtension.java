/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit5;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import org.junit.gen5.api.extension.*;

import mockit.*;
import mockit.integration.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.mockups.*;
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
   @Nullable private Object[] mockParameters;

   @Override
   public void beforeAll(ContainerExtensionContext context)
   {
      savePointForTestClass = new SavePoint();

      //noinspection OptionalGetWithoutIsPresent
      Class<?> testClass = context.getTestClass().get();
      TestRun.setCurrentTestClass(testClass);
   }

   @Override
   public void postProcessTestInstance(Object testInstance, ExtensionContext context)
   {
      TestRun.enterNoMockingZone();

      try {
         handleMockFieldsForWholeTestClass(testInstance);
      }
      finally {
         TestRun.exitNoMockingZone();
      }

      TestRun.setRunningIndividualTest(testInstance);
   }

   @Override
   public void beforeEach(TestExtensionContext context)
   {
      Object testInstance = context.getTestInstance();

      TestRun.prepareForNextTest();
      TestRun.enterNoMockingZone();

      try {
         savePointForTest = new SavePoint();
         createInstancesForTestedFields(testInstance, true);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   public void beforeTestExecution(TestExtensionContext context)
   {
      //noinspection OptionalGetWithoutIsPresent
      Method method = context.getTestMethod().get();
      Object testInstance = context.getTestInstance();

      TestRun.enterNoMockingZone();

      try {
         savePointForTestMethod = new SavePoint();
         mockParameters = createInstancesForMockParameters(method, null);
         createInstancesForTestedFields(testInstance, false);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      Parameter parameter = parameterContext.getParameter();
      return
         parameter.isAnnotationPresent(Mocked.class) ||
         parameter.isAnnotationPresent(Injectable.class) ||
         parameter.isAnnotationPresent(Capturing.class);
   }

   @Override
   public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      return mockParameters[parameterContext.getIndex()];
   }

   @Override
   public void handleTestExecutionException(TestExtensionContext context, Throwable throwable) throws Throwable
   {
      thrownByTest = throwable;
      throw throwable;
   }

   @Override
   public void afterTestExecution(TestExtensionContext context)
   {
      TestRun.enterNoMockingZone();

      try {
         assert savePointForTestMethod != null;
         savePointForTestMethod.rollback();
         savePointForTestMethod = null;

         if (thrownByTest != null) {
            filterStackTrace(thrownByTest);
         }

         Error expectationsFailure = RecordAndReplayExecution.endCurrentReplayIfAny();
         clearTestedFieldsIfAny();
         verifyAndClearMockupConstraintsIfAny(expectationsFailure);

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

   private void verifyAndClearMockupConstraintsIfAny(Error expectationsFailure)
   {
      MockStates mockStates = TestRun.getMockStates();

      try {
         if (expectationsFailure == null && thrownByTest == null) {
            mockStates.verifyMissingInvocations();
         }
      }
      finally {
         mockStates.resetExpectations();
      }
   }

   @Override
   public void afterEach(TestExtensionContext context)
   {
      assert savePointForTest != null;
      savePointForTest.rollback();
      savePointForTest = null;
   }

   @Override
   public void afterAll(ContainerExtensionContext context)
   {
      assert savePointForTestClass != null;
      savePointForTestClass.rollback();
      savePointForTestClass = null;

      clearFieldTypeRedefinitions();
      TestRun.setCurrentTestClass(null);
   }
}
