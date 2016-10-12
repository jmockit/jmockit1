/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.spock;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.integration.internal.*;
import mockit.integration.junit4.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.mockups.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import org.spockframework.runtime.extension.*;
import org.spockframework.runtime.model.*;
import spock.lang.*;
import static mockit.internal.util.StackTrace.*;

public final class JMockitExtension extends TestRunnerDecorator implements IGlobalExtension, IMethodInterceptor {

   @Override
   public void start() {
      Startup.initializeIfPossible();
   }

   @Override
   public void visitSpec(SpecInfo spec) {
      spec.addInterceptor(this);
      spec.addInitializerInterceptor(this);
      for (FeatureInfo featureInfo : spec.getAllFeatures()) {
         featureInfo.addIterationInterceptor(this);
         featureInfo.getFeatureMethod().addInterceptor(this);
      }
   }

   @Override
   public void stop() {
   }

   public final void intercept(IMethodInvocation invocation) throws Throwable {
      switch (invocation.getMethod().getKind()) {
         case SPEC_EXECUTION:
            interceptSpecExecution(invocation);
            break;

         case INITIALIZER:
            interceptInitializerMethod(invocation);
            break;

         case ITERATION_EXECUTION:
            interceptIterationExecution(invocation);
            break;

         case FEATURE:
            interceptFeatureMethod(invocation);
            break;

         default:
            throw new UnsupportedOperationException("intercepting for kind '" + invocation.getMethod().getKind() + "' is not implemented");
      }
   }

   private void interceptSpecExecution(IMethodInvocation invocation) throws Throwable {
      // Clean up, because if JUnit 4 and Spock tests are mixed, there might be some
      // ghosts around from JUnit that would be cleaned up before the next test class,
      // so do it here also, as mixing JUnit and Spock tests is a valid use-case
      if (MockFrameworkMethod.hasDependenciesInClasspath()) {
         clearFieldTypeRedefinitions();
      }

      SavePoint savePointForTestClass = new SavePoint();

      Class<?> testClass = invocation.getInstance().getClass();
      TestRun.setCurrentTestClass(testClass);

      try {
         invocation.proceed();
      } finally {
         savePointForTestClass.rollback();

         clearFieldTypeRedefinitions();

         // Do not do this if JUnit 4 is the parent runner, as accompanying
         // JUnit tests would assume they are the first test to run and
         // miss to do some cleanup
         if (!MockFrameworkMethod.hasDependenciesInClasspath()) {
            TestRun.setCurrentTestClass(null);
         }
      }
   }

   private void interceptInitializerMethod(IMethodInvocation invocation) throws Throwable {
      invocation.proceed();

      TestRun.enterNoMockingZone();

      try {
         handleMockFieldsForWholeTestClass(invocation.getInstance());
      } finally {
         TestRun.exitNoMockingZone();
      }

      TestRun.setRunningIndividualTest(invocation.getInstance(), true);
   }

   private void interceptIterationExecution(IMethodInvocation invocation) throws Throwable {
      TestRun.prepareForNextTest();
      TestRun.enterNoMockingZone();

      SavePoint savePointForTest = new SavePoint();
      try {
         createInstancesForTestedFields(invocation.getInstance(), true);
      } finally {
         TestRun.exitNoMockingZone();
      }

      try {
         invocation.proceed();
      } finally {
         savePointForTest.rollback();
      }
   }

   private void interceptFeatureMethod(IMethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod().getReflection();
      Object testInstance = invocation.getInstance();

      TestRun.enterNoMockingZone();

      SavePoint savePointForTestMethod = new SavePoint();
      try {
         invocation.setArguments(createInstancesForMockParameters(method, invocation.getArguments()));
         createInstancesForTestedFields(testInstance, false);
      } finally {
         TestRun.exitNoMockingZone();
      }

      TestRun.setRunningIndividualTest(testInstance, false);

      Throwable thrownByTest = null;
      try {
         invocation.proceed();
      } catch (Throwable throwable) {
         thrownByTest = throwable;
         throw throwable;
      } finally {
         TestRun.enterNoMockingZone();

         thrownByTest = thrownByTest == null ? ((Specification) invocation.getInstance()).getSpecificationContext().getThrownException() : thrownByTest;

         try {
            savePointForTestMethod.rollback();

            if (thrownByTest != null) {
               filterStackTrace(thrownByTest);
            }

            Error expectationsFailure = RecordAndReplayExecution.endCurrentReplayIfAny();
            clearTestedFieldsIfAny();

            MockStates mockStates = TestRun.getMockStates();
            try {
               if (expectationsFailure == null && thrownByTest == null) {
                  mockStates.verifyMissingInvocations();
               }
            } finally {
               mockStates.resetExpectations();
            }

            if (thrownByTest == null && expectationsFailure != null) {
               filterStackTrace(expectationsFailure);
               throw expectationsFailure;
            }
         } finally {
            TestRun.finishCurrentTestExecution();
            TestRun.exitNoMockingZone();
         }
      }
   }
}
