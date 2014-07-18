/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import static mockit.external.asm4.Opcodes.*;
import static mockit.internal.util.TypeConversion.*;

final class InvocationBlockModifier extends MethodVisitor
{
   private static final String CLASS_DESC = Type.getInternalName(ActiveInvocations.class);
   private static final Type[] NO_PARAMETERS = new Type[0];

   @NotNull private final MethodWriter mw;

   // Input data:
   @NotNull private final String blockOwner;
   private final boolean callEndInvocations;

   // Takes care of "withCapture()" matchers, if any:
   @NotNull private final ArgumentCapturing argumentCapturing;

   // Helper fields that allow argument matchers to be moved to the correct positions of their
   // corresponding parameters:
   @NotNull private final int[] matcherStacks;
   private int matcherCount;
   @NotNull private Type[] parameterTypes;

   Capture createCapture(int opcode, int varIndex, @Nullable String typeToCapture)
   {
      return new Capture(opcode, varIndex, typeToCapture);
   }

   final class Capture
   {
      final int opcode;
      private final int varIndex;
      @Nullable private final String typeToCapture;
      private int parameterIndex;
      private boolean parameterIndexFixed;

      Capture(int opcode, int varIndex, @Nullable String typeToCapture)
      {
         this.opcode = opcode;
         this.varIndex = varIndex;
         this.typeToCapture = typeToCapture;
         parameterIndex = matcherCount - 1;
      }

      /**
       * Generates bytecode that will be responsible for performing the following steps:
       * 1. Get the argument value (an Object) for the last matched invocation.
       * 2. Cast to a reference type or unbox to a primitive type, as needed.
       * 3. Store the converted value in its local variable.
       */
      void generateCodeToStoreCapturedValue()
      {
         mw.visitIntInsn(SIPUSH, parameterIndex);
         generateCallToActiveInvocationsMethod("matchedArgument", "(I)Ljava/lang/Object;");

         Type argType = getArgumentType();
         generateCastOrUnboxing(mw, argType, opcode);

         mw.visitVarInsn(opcode, varIndex);
      }

      @NotNull private Type getArgumentType()
      {
         if (typeToCapture == null) {
            return parameterTypes[parameterIndex];
         }
         else if (typeToCapture.charAt(0) == '[') {
            return Type.getType(typeToCapture);
         }
         else {
            return Type.getType('L' + typeToCapture + ';');
         }
      }

      boolean fixParameterIndex(int originalIndex, int newIndex)
      {
         if (!parameterIndexFixed && parameterIndex == originalIndex) {
            parameterIndex = newIndex;
            parameterIndexFixed = true;
            return true;
         }

         return false;
      }

      void generateCallToSetArgumentTypeIfNeeded()
      {
         if (typeToCapture != null && !isTypeToCaptureSameAsParameterType(typeToCapture)) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            mw.visitLdcInsn(typeToCapture);
            generateCallToActiveInvocationsMethod("setExpectedArgumentType", "(ILjava/lang/String;)V");
         }
      }

      private boolean isTypeToCaptureSameAsParameterType(@NotNull String typeDesc)
      {
         Type parameterType = parameterTypes[parameterIndex];
         int sort = parameterType.getSort();

         if (sort == Type.OBJECT || sort == Type.ARRAY) {
            return typeDesc.equals(parameterType.getInternalName());
         }

         return isPrimitiveWrapper(typeDesc);
      }
   }

   InvocationBlockModifier(@NotNull MethodWriter mw, @NotNull String blockOwner, boolean callEndInvocations)
   {
      super(mw);
      this.mw = mw;
      this.blockOwner = blockOwner;
      this.callEndInvocations = callEndInvocations;
      matcherStacks = new int[40];
      argumentCapturing = new ArgumentCapturing();
      parameterTypes = NO_PARAMETERS;
   }

   private void generateCallToActiveInvocationsMethod(@NotNull String name, @NotNull String desc)
   {
      mw.visitMethodInsn(INVOKESTATIC, CLASS_DESC, name, desc);
   }

   @Override
   public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc)
   {
      boolean gettingMockedEnumElement =
         opcode == GETSTATIC && TestRun.mockFixture().isMockedClass(ClassLoad.loadByInternalName(owner));

      if (gettingMockedEnumElement) {
         mw.visitVarInsn(ALOAD, 0);
      }
      else if (
         (opcode == GETFIELD || opcode == PUTFIELD) &&
         name.indexOf('$') < 1 &&
         isFieldDefinedByInvocationBlock(owner)
      ) {
         if (opcode == PUTFIELD) {
            if (generateCodeThatReplacesAssignmentToSpecialField(name)) {
               mw.visitInsn(POP);
               return;
            }
         }
         else if (name.startsWith("any")) {
            generateCodeToAddArgumentMatcherForAnyField(owner, name, desc);
            return;
         }
      }

      mw.visitFieldInsn(opcode, owner, name, desc);

      if (gettingMockedEnumElement) {
         mw.visitMethodInsn(INVOKEVIRTUAL, blockOwner, "onInstance", "(Ljava/lang/Object;)Ljava/lang/Object;");
         mw.visitTypeInsn(CHECKCAST, owner);
      }
   }

   private boolean isFieldDefinedByInvocationBlock(@NotNull String fieldOwner)
   {
      return
         blockOwner.equals(fieldOwner) ||
         ("mockit/Expectations mockit/NonStrictExpectations " +
          "mockit/Verifications mockit/VerificationsInOrder " +
          "mockit/FullVerifications mockit/FullVerificationsInOrder").contains(fieldOwner);
   }

   private boolean generateCodeThatReplacesAssignmentToSpecialField(@NotNull String fieldName)
   {
      if ("result".equals(fieldName)) {
         generateCallToActiveInvocationsMethod("addResult", "(Ljava/lang/Object;)V");
         return true;
      }

      if ("times".equals(fieldName) || "minTimes".equals(fieldName) || "maxTimes".equals(fieldName)) {
         generateCallToActiveInvocationsMethod(fieldName, "(I)V");
         return true;
      }

      if ("$".equals(fieldName)) {
         generateCallToActiveInvocationsMethod("setErrorMessage", "(Ljava/lang/CharSequence;)V");
         return true;
      }

      return false;
   }

   private void generateCodeToAddArgumentMatcherForAnyField(
      @NotNull String fieldOwner, @NotNull String name, @NotNull String desc)
   {
      mw.visitFieldInsn(GETFIELD, fieldOwner, name, desc);
      generateCallToActiveInvocationsMethod("addArgMatcher", "()V");
      matcherStacks[matcherCount++] = mw.stackSize2;
   }

   @Override
   public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc)
   {
      if (opcode == INVOKESTATIC && (isBoxing(owner, name, desc) || isAccessMethod(owner, name))) {
         // It's an invocation to a primitive boxing method or to a synthetic method for private access, just ignore it.
         mw.visitMethodInsn(INVOKESTATIC, owner, name, desc);
      }
      else if (opcode == INVOKEVIRTUAL && owner.equals(blockOwner) && name.startsWith("with")) {
         mw.visitMethodInsn(INVOKEVIRTUAL, owner, name, desc);

         if (argumentCapturing.registerMatcher(name, desc)) {
            matcherStacks[matcherCount++] = mw.stackSize2;
         }
      }
      else if (isUnboxing(opcode, owner, desc)) {
         if (argumentCapturing.justAfterWithCaptureInvocation) {
            generateCodeToReplaceNullWithZeroOnTopOfStack(desc.charAt(2));
            argumentCapturing.justAfterWithCaptureInvocation = false;
         }
         else {
            mw.visitMethodInsn(opcode, owner, name, desc);
         }
      }
      else if (matcherCount == 0) {
         mw.visitMethodInsn(opcode, owner, name, desc);
      }
      else {
         parameterTypes = Type.getArgumentTypes(desc);
         int stackSize = mw.stackSize2;
         int stackAfter = stackSize - sumOfParameterSizes();
         boolean mockedInvocationUsingTheMatchers = stackAfter < matcherStacks[0];

         if (mockedInvocationUsingTheMatchers) {
            generateCallsToMoveArgMatchers(stackAfter);
            argumentCapturing.generateCallsToSetArgumentTypesToCaptureIfAny();
            matcherCount = 0;
         }

         mw.visitMethodInsn(opcode, owner, name, desc);

         if (mockedInvocationUsingTheMatchers) {
            argumentCapturing.generateCallsToCaptureMatchedArgumentsIfPending();
         }

         argumentCapturing.justAfterWithCaptureInvocation = false;
      }
   }

   private boolean isAccessMethod(@NotNull String methodOwner, @NotNull String name)
   {
      return !methodOwner.equals(blockOwner) && name.startsWith("access$");
   }

   private void generateCodeToReplaceNullWithZeroOnTopOfStack(char primitiveTypeCode)
   {
      mw.visitInsn(POP);

      int zeroOpcode;
      switch (primitiveTypeCode) {
         case 'J': zeroOpcode = LCONST_0; break;
         case 'F': zeroOpcode = FCONST_0; break;
         case 'D': zeroOpcode = DCONST_0; break;
         default: zeroOpcode = ICONST_0;
      }

      mw.visitInsn(zeroOpcode);
   }

   private int sumOfParameterSizes()
   {
      int sum = 0;

      for (Type argType : parameterTypes) {
         sum += argType.getSize();
      }

      return sum;
   }

   private void generateCallsToMoveArgMatchers(int initialStack)
   {
      int stack = initialStack;
      int nextMatcher = 0;
      int matcherStack = matcherStacks[0];

      for (int i = 0; i < parameterTypes.length && nextMatcher < matcherCount; i++) {
         stack += parameterTypes[i].getSize();

         if (stack == matcherStack || stack == matcherStack + 1) {
            if (nextMatcher < i) {
               generateCallToMoveArgMatcher(nextMatcher, i);
               argumentCapturing.updateCaptureIfAny(nextMatcher, i);
            }

            matcherStack = matcherStacks[++nextMatcher];
         }
      }
   }

   private void generateCallToMoveArgMatcher(int originalMatcherIndex, int toIndex)
   {
      mw.visitIntInsn(SIPUSH, originalMatcherIndex);
      mw.visitIntInsn(SIPUSH, toIndex);
      generateCallToActiveInvocationsMethod("moveArgMatcher", "(II)V");
   }

   @Override
   public void visitLocalVariable(
      @NotNull String name, @NotNull String desc, @Nullable String signature, @NotNull Label start, @NotNull Label end,
      int index)
   {
      // In classes instrumented with EMMA some local variable information can be lost, so we discard it entirely to
      // avoid a ClassFormatError.
      if (end.position > 0) {
         mw.visitLocalVariable(name, desc, signature, start, end, index);
      }
   }

   @Override
   public void visitTypeInsn(int opcode, @NotNull String type)
   {
      argumentCapturing.registerTypeToCaptureIfApplicable(opcode, type);
      mw.visitTypeInsn(opcode, type);
   }

   @Override
   public void visitVarInsn(int opcode, @SuppressWarnings("QuestionableName") int var)
   {
      argumentCapturing.registerAssignmentToCaptureVariableIfApplicable(this, opcode, var);
      mw.visitVarInsn(opcode, var);
   }

   @Override
   public void visitInsn(int opcode)
   {
      if (opcode == RETURN && callEndInvocations) {
         generateCallToActiveInvocationsMethod("endInvocations", "()V");
      }

      mw.visitInsn(opcode);
   }
}
