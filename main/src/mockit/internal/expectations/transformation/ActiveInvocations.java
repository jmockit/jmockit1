/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

@SuppressWarnings("unused")
public final class ActiveInvocations
{
   static final Map<Integer, String> varIndexToTypeDesc = new HashMap<Integer, String>();

   private ActiveInvocations() {}

   public static void anyString()  { addArgMatcher(AlwaysTrueMatcher.ANY_STRING); }
   public static void anyBoolean() { addArgMatcher(AlwaysTrueMatcher.ANY_BOOLEAN); }
   public static void anyChar()    { addArgMatcher(AlwaysTrueMatcher.ANY_CHAR); }
   public static void anyByte()    { addArgMatcher(AlwaysTrueMatcher.ANY_BYTE); }
   public static void anyShort()   { addArgMatcher(AlwaysTrueMatcher.ANY_SHORT); }
   public static void anyInt()     { addArgMatcher(AlwaysTrueMatcher.ANY_INT); }
   public static void anyFloat()   { addArgMatcher(AlwaysTrueMatcher.ANY_FLOAT); }
   public static void anyLong()    { addArgMatcher(AlwaysTrueMatcher.ANY_LONG); }
   public static void anyDouble()  { addArgMatcher(AlwaysTrueMatcher.ANY_DOUBLE); }
   public static void any()        { addArgMatcher(AlwaysTrueMatcher.ANY_VALUE); }

   private static void addArgMatcher(@Nonnull ArgumentMatcher<?> argumentMatcher)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            currentPhase.addArgMatcher(argumentMatcher);
         }
      }
   }

   public static void moveArgMatcher(@Nonnegative int originalMatcherIndex, @Nonnegative int toIndex)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            currentPhase.moveArgMatcher(originalMatcherIndex, toIndex);
         }
      }
   }

   public static void setExpectedArgumentType(@Nonnegative int parameterIndex, @Nonnull String typeDesc)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            Class<?> argumentType = ClassLoad.loadByInternalName(typeDesc);
            currentPhase.setExpectedSingleArgumentType(parameterIndex, argumentType);
         }
      }
   }

   public static void setExpectedArgumentType(@Nonnegative int parameterIndex, int varIndex)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         String typeDesc = varIndexToTypeDesc.remove(varIndex);

         if (typeDesc != null) {
            TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

            if (currentPhase != null) {
               Class<?> argumentType = ClassLoad.loadByInternalName(typeDesc);
               currentPhase.setExpectedMultiArgumentType(parameterIndex, argumentType);
            }
         }
      }
   }

   @Nullable
   public static Object matchedArgument(@Nonnegative int parameterIndex)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         BaseVerificationPhase verificationPhase = (BaseVerificationPhase) instance.getCurrentTestOnlyPhase();

         if (verificationPhase != null) {
            return verificationPhase.getArgumentValueForCurrentVerification(parameterIndex);
         }
      }

      return null;
   }

   public static void addResult(@Nullable Object result)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         RecordPhase recordPhase = instance.getRecordPhase();

         if (recordPhase != null) {
            recordPhase.addResult(result);
         }
      }
   }

   public static void times(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            currentPhase.handleInvocationCountConstraint(n, n);
         }
      }
   }

   public static void minTimes(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            currentPhase.handleInvocationCountConstraint(n, -1);
         }
      }
   }

   public static void maxTimes(int n)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();

      if (instance != null) {
         TestOnlyPhase currentPhase = instance.getCurrentTestOnlyPhase();

         if (currentPhase != null) {
            currentPhase.setMaxInvocationCount(n);
         }
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
