/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

@SuppressWarnings("unused")
public final class ActiveInvocations
{
   private ActiveInvocations() {}

   public static void addArgMatcher()
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         currentPhase.addArgMatcher(AlwaysTrueMatcher.INSTANCE);
      }
   }

   public static void moveArgMatcher(int originalMatcherIndex, int toIndex)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         currentPhase.moveArgMatcher(originalMatcherIndex, toIndex);
      }
   }

   public static void setExpectedArgumentType(int parameterIndex, @NotNull String typeDesc)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         Class<?> argumentType = ClassLoad.loadByInternalName(typeDesc);
         currentPhase.setExpectedArgumentType(parameterIndex, argumentType);
      }
   }

   @Nullable
   public static Object matchedArgument(int parameterIndex)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         BaseVerificationPhase verificationPhase = (BaseVerificationPhase) instance.getCurrentTestOnlyPhase();
         return verificationPhase.getArgumentValueForCurrentVerification(parameterIndex);
      }

      return null;
   }

   public static void addResult(@Nullable Object result)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         instance.getRecordPhase().addResult(result);
      }
   }

   public static void times(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         currentPhase.handleInvocationCountConstraint(n, n);
      }
   }

   public static void minTimes(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         currentPhase.handleInvocationCountConstraint(n, -1);
      }
   }

   public static void maxTimes(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();
         currentPhase.setMaxInvocationCount(n);
      }
   }

   public static void setErrorMessage(@Nullable CharSequence customMessage)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         instance.getCurrentTestOnlyPhase().setCustomErrorMessage(customMessage);
      }
   }

   public static void endInvocations()
   {
      TestRun.enterNoMockingZone();

      try {
         RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();
         assert instance != null;
         instance.endInvocations();
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}
