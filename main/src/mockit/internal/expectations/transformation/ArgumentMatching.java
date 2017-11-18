/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import javax.annotation.*;

import mockit.external.asm.*;
import static mockit.external.asm.Opcodes.*;

final class ArgumentMatching
{
   private static final JavaType[] NO_PARAMETERS = new JavaType[0];
   private static final String ANY_FIELDS =
      "any anyString anyInt anyBoolean anyLong anyDouble anyFloat anyChar anyShort anyByte";
   private static final String WITH_METHODS =
      "withArgThat(Lorg/hamcrest/Matcher;)Ljava/lang/Object; " +
      "with(Lmockit/Delegate;)Ljava/lang/Object; " +
      "withAny(Ljava/lang/Object;)Ljava/lang/Object; " +
      "withCapture()Ljava/lang/Object; withCapture(Ljava/util/List;)Ljava/lang/Object; " +
      "withCapture(Ljava/lang/Object;)Ljava/util/List; " +
      "withEqual(Ljava/lang/Object;)Ljava/lang/Object; withEqual(DD)D withEqual(FD)F " +
      "withInstanceLike(Ljava/lang/Object;)Ljava/lang/Object; " +
      "withInstanceOf(Ljava/lang/Class;)Ljava/lang/Object; " +
      "withNotEqual(Ljava/lang/Object;)Ljava/lang/Object; " +
      "withNull()Ljava/lang/Object; withNotNull()Ljava/lang/Object; " +
      "withSameInstance(Ljava/lang/Object;)Ljava/lang/Object; " +
      "withSubstring(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
      "withPrefix(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
      "withSuffix(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
      "withMatch(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;";

   @Nonnull private final InvocationBlockModifier modifier;

   // Helper fields that allow argument matchers to be moved to the correct positions of their corresponding parameters:
   @Nonnull private final int[] matcherStacks;
   @Nonnegative private int matcherCount;
   @Nonnull private JavaType[] parameterTypes;

   static boolean isAnyField(@Nonnull String name)
   {
      return name.startsWith("any") && ANY_FIELDS.contains(name);
   }

   static boolean isCallToArgumentMatcher(@Nonnull String name, @Nonnull String desc)
   {
      return name.startsWith("with") && WITH_METHODS.contains(name + desc);
   }

   ArgumentMatching(@Nonnull InvocationBlockModifier modifier)
   {
      this.modifier = modifier;
      matcherStacks = new int[40];
      parameterTypes = NO_PARAMETERS;
   }

   void addMatcher(@Nonnegative int stackSize) { matcherStacks[matcherCount++] = stackSize; }

   @Nonnegative int getMatcherCount() { return matcherCount; }
   @Nonnull JavaType getParameterType(@Nonnegative int parameterIndex) { return parameterTypes[parameterIndex]; }

   void generateCodeToAddArgumentMatcherForAnyField(
      @Nonnull String fieldOwner, @Nonnull String name, @Nonnull String desc)
   {
      MethodWriter mw = modifier.getMethodWriter();
      mw.visitFieldInsn(GETFIELD, fieldOwner, name, desc);
      modifier.generateCallToActiveInvocationsMethod(name);
   }

   boolean handleInvocationParameters(@Nonnegative int stackSize, @Nonnull String desc)
   {
      parameterTypes = JavaType.getArgumentTypes(desc);
      int stackAfter = stackSize - getSumOfParameterSizes();
      boolean mockedInvocationUsingTheMatchers = stackAfter < matcherStacks[0];

      if (mockedInvocationUsingTheMatchers) {
         generateCallsToMoveArgMatchers(stackAfter);
         modifier.argumentCapturing.generateCallsToSetArgumentTypesToCaptureIfAny();
         matcherCount = 0;
      }

      return mockedInvocationUsingTheMatchers;
   }

   @Nonnegative
   private int getSumOfParameterSizes()
   {
      @Nonnegative int sum = 0;

      for (JavaType argType : parameterTypes) {
         sum += argType.getSize();
      }

      return sum;
   }

   private void generateCallsToMoveArgMatchers(@Nonnegative int initialStack)
   {
      @Nonnegative int stack = initialStack;
      @Nonnegative int nextMatcher = 0;
      @Nonnegative int matcherStack = matcherStacks[0];

      for (int i = 0; i < parameterTypes.length && nextMatcher < matcherCount; i++) {
         stack += parameterTypes[i].getSize();

         if (stack == matcherStack || stack == matcherStack + 1) {
            if (nextMatcher < i) {
               generateCallToMoveArgMatcher(nextMatcher, i);
               modifier.argumentCapturing.updateCaptureIfAny(nextMatcher, i);
            }

            matcherStack = matcherStacks[++nextMatcher];
         }
      }
   }

   private void generateCallToMoveArgMatcher(@Nonnegative int originalMatcherIndex, @Nonnegative int toIndex)
   {
      MethodWriter mw = modifier.getMethodWriter();
      mw.visitIntInsn(SIPUSH, originalMatcherIndex);
      mw.visitIntInsn(SIPUSH, toIndex);
      modifier.generateCallToActiveInvocationsMethod("moveArgMatcher", "(II)V");
   }
}
