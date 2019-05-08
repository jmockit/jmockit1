/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.injection.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

/**
 * Base class for "test runner decorators", which provide integration between JMockit and specific test runners from JUnit and TestNG.
 */
public class TestRunnerDecorator
{
   @Nullable private static SavePoint savePointForTestClass;
   @Nullable private static SavePoint savePointForTest;

   /**
    * A "volatile boolean" is as good as a java.util.concurrent.atomic.AtomicBoolean here, since we only need the basic get/set operations.
    */
   protected volatile boolean shouldPrepareForNextTest;

   protected TestRunnerDecorator() { shouldPrepareForNextTest = true; }

   protected static void updateTestClassState(@Nullable Object target, @Nonnull Class<?> testClass) {
      testClass = getActualTestClass(testClass);

      try {
         handleSwitchToNewTestClassIfApplicable(testClass);

         if (target != null) {
            handleMockFieldsForWholeTestClass(target);
         }
      }
      catch (Error e) {
         try {
            rollbackForTestClass();
         }
         catch (Error err) {
            StackTrace.filterStackTrace(err);
            throw err;
         }

         throw e;
      }
      catch (RuntimeException e) {
         rollbackForTestClass();
         StackTrace.filterStackTrace(e);
         throw e;
      }
   }

   @Nonnull
   private static Class<?> getActualTestClass(@Nonnull Class<?> testClass) {
      return testClass.isSynthetic() ? testClass.getSuperclass() : testClass;
   }

   private static void handleSwitchToNewTestClassIfApplicable(@Nonnull Class<?> testClass) {
      Class<?> currentTestClass = TestRun.getCurrentTestClass();

      if (testClass != currentTestClass) {
         if (currentTestClass == null) {
            savePointForTestClass = new SavePoint();
         }
         else if (!currentTestClass.isAssignableFrom(testClass)) {
            cleanUpMocksFromPreviousTestClass();
            savePointForTestClass = new SavePoint();
         }

         TestRun.setCurrentTestClass(testClass);
      }
   }

   public static void cleanUpMocksFromPreviousTestClass() { cleanUpMocks(true); }
   protected static void cleanUpMocksFromPreviousTest() { cleanUpMocks(false); }

   public static void cleanUpAllMocks() {
      cleanUpMocks(true);
      TestRun.getFakeClasses().discardStartupFakes();
   }

   private static void cleanUpMocks(boolean forTestClassAsWell) {
      discardTestLevelMockedTypes();

      if (forTestClassAsWell) {
         rollbackForTestClass();
      }

      clearFieldTypeRedefinitions();
   }

   private static void rollbackForTestClass() {
      SavePoint savePoint = savePointForTestClass;

      if (savePoint != null) {
         savePoint.rollback();
         savePointForTestClass = null;
      }
   }

   protected static void clearFieldTypeRedefinitions() {
      TypeRedefinitions fieldTypeRedefinitions = TestRun.getFieldTypeRedefinitions();

      if (fieldTypeRedefinitions != null) {
         fieldTypeRedefinitions.cleanUp();
         TestRun.setFieldTypeRedefinitions(null);
      }
   }

   protected static void prepareForNextTest() {
      if (savePointForTest == null) {
         savePointForTest = new SavePoint();
      }

      TestRun.prepareForNextTest();
   }

   protected static void discardTestLevelMockedTypes() {
      SavePoint savePoint = savePointForTest;

      if (savePoint != null) {
         savePoint.rollback();
         savePointForTest = null;
      }
   }

   protected static void handleMockFieldsForWholeTestClass(@Nonnull Object target) {
      Class<?> testClass = getActualTestClass(target.getClass());
      FieldTypeRedefinitions fieldTypeRedefinitions = TestRun.getFieldTypeRedefinitions();

      if (needsRedifinitions(testClass, fieldTypeRedefinitions)) {
         ParameterNameExtractor.extractNames(testClass);

         fieldTypeRedefinitions = new FieldTypeRedefinitions(testClass);
         TestRun.setFieldTypeRedefinitions(fieldTypeRedefinitions);

         TestedClassInstantiations testedClassInstantiations = new TestedClassInstantiations();

         if (!testedClassInstantiations.findTestedAndInjectableMembers(testClass)) {
            testedClassInstantiations = null;
         }

         TestRun.setTestedClassInstantiations(testedClassInstantiations);
      }

      //noinspection ObjectEquality
      if (target != TestRun.getCurrentTestInstance()) {
         fieldTypeRedefinitions.assignNewInstancesToMockFields(target);
      }
   }

   private static boolean needsRedifinitions(Class<?> testClass, @Nullable FieldTypeRedefinitions fieldTypeRedefinitions) {
      final Object current = TestRun.getCurrentTestInstance();
      if (fieldTypeRedefinitions == null || current == null) {
          return true;
      }
      if (!Objects.equals(current.getClass(), testClass)) {
          return true;
      }
      return false;
   }

   protected static void createInstancesForTestedFieldsFromBaseClasses(@Nonnull Object testClassInstance) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses != null) {
         TestRun.enterNoMockingZone();

         try {
            testedClasses.assignNewInstancesToTestedFieldsFromBaseClasses(testClassInstance);
         }
         finally {
            TestRun.exitNoMockingZone();
         }
      }
   }

   protected static void createInstancesForTestedFieldsBeforeSetup(@Nonnull Object testClassInstance) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses != null) {
         TestRun.enterNoMockingZone();

         try {
            testedClasses.assignNewInstancesToTestedFields(testClassInstance, true, Collections.<InjectionProvider>emptyList());
         }
         finally {
            TestRun.exitNoMockingZone();
         }
      }
   }

   protected static void createInstancesForTestedFields(@Nonnull Object testClassInstance) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses != null) {
         List<? extends InjectionProvider> injectableParameters = Collections.emptyList();
         ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterRedefinitions();

         if (paramTypeRedefs != null) {
            injectableParameters = paramTypeRedefs.getInjectableParameters();
         }

         TestRun.enterNoMockingZone();

         try {
            testedClasses.assignNewInstancesToTestedFields(testClassInstance, false, injectableParameters);
         }
         finally {
            TestRun.exitNoMockingZone();
         }
      }
   }

   @Nullable
   protected static Object[] createInstancesForAnnotatedParameters(
      @Nonnull Object testClassInstance, @Nonnull Method testMethod, @Nullable Object[] parameterValues
   ) {
      int numParameters = testMethod.getParameterTypes().length;

      if (numParameters == 0) {
         return null;
      }

      if (parameterValues == null || parameterValues.length != numParameters) {
         //noinspection AssignmentToMethodParameter
         parameterValues = new Object[numParameters];
      }

      TestMethod methodInfo = new TestMethod(testMethod, parameterValues);

      TestRun.enterNoMockingZone();

      try {
         ParameterTypeRedefinitions redefinitions = new ParameterTypeRedefinitions(methodInfo, parameterValues);
         TestRun.getExecutingTest().setParameterRedefinitions(redefinitions);

         TestedParameters testedParameters = new TestedParameters(methodInfo);
         List<? extends InjectionProvider> injectableParameters = redefinitions.getInjectableParameters();
         testedParameters.createTestedParameters(testClassInstance, injectableParameters);
      }
      finally {
         TestRun.exitNoMockingZone();
      }

      return parameterValues;
   }

   protected static void concludeTestMethodExecution(
      @Nonnull SavePoint savePoint, @Nullable Throwable thrownByTest, boolean thrownAsExpected
   ) throws Throwable {
      TestRun.enterNoMockingZone();

      Error expectationsFailure = RecordAndReplayExecution.endCurrentReplayIfAny();

      try {
         clearTestedObjectsIfAny();
      }
      finally {
         savePoint.rollback();
         TestRun.exitNoMockingZone();
      }

      if (thrownByTest != null) {
         if (expectationsFailure == null || !thrownAsExpected || isUnexpectedOrMissingInvocation(thrownByTest)) {
            throw thrownByTest;
         }

         Throwable expectationsFailureCause = expectationsFailure.getCause();

         if (expectationsFailureCause != null) {
            expectationsFailureCause.initCause(thrownByTest);
         }
      }

      if (expectationsFailure != null) {
         throw expectationsFailure;
      }
   }

   protected static void clearTestedObjectsIfAny() {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses != null) {
         testedClasses.clearTestedObjects();
      }
   }

   protected static void clearTestedObjectsCreatedDuringSetup() {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses != null) {
         testedClasses.clearTestedObjectsCreatedDuringSetup();
      }
   }

   private static boolean isUnexpectedOrMissingInvocation(@Nonnull Throwable error) {
      Class<?> errorType = error.getClass();
      return errorType == UnexpectedInvocation.class || errorType == MissingInvocation.class;
   }
}
