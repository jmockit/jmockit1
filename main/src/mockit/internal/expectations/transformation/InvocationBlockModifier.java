/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import mockit.external.asm.*;
import mockit.internal.state.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.util.TypeConversion.*;

import org.jetbrains.annotations.*;

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
   private int stackSize;
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
      visitMethodInstruction(INVOKESTATIC, CLASS_DESC, name, desc, false);
   }

   @Override
   public void visitFieldInsn(int opcode, String owner, String name, String desc)
   {
      boolean gettingMockedEnumElement =
         opcode == GETSTATIC && TestRun.mockFixture().isMockedClass(owner.replace('/', '.'));

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
               visitInsn(POP);
               return;
            }
         }
         else if (name.startsWith("any")) {
            generateCodeToAddArgumentMatcherForAnyField(owner, name, desc);
            return;
         }
      }

      stackSize += stackSizeVariationForFieldAccess(opcode, desc);
      mw.visitFieldInsn(opcode, owner, name, desc);

      if (gettingMockedEnumElement) {
         visitMethodInstruction(
            INVOKEVIRTUAL, blockOwner, "onInstance", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
         mw.visitTypeInsn(CHECKCAST, owner);
      }
   }

   private boolean isFieldDefinedByInvocationBlock(@NotNull String fieldOwner)
   {
      return
         blockOwner.equals(fieldOwner) ||
         ("mockit/Expectations mockit/StrictExpectations mockit/NonStrictExpectations " +
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
      matcherStacks[matcherCount++] = stackSize;
   }

   private static int stackSizeVariationForFieldAccess(int opcode, @NotNull String fieldType)
   {
      char c = fieldType.charAt(0);
      boolean twoByteType = c == 'D' || c == 'J';

      switch (opcode) {
         case GETSTATIC: return twoByteType ? 2 : 1;
         case PUTSTATIC: return twoByteType ? -2 : -1;
         case GETFIELD: return twoByteType ? 1 : 0;
         default: return twoByteType ? -3 : -2;
      }
   }

   @Override
   public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
   {
      if (opcode == INVOKESTATIC && (isBoxing(owner, name, desc) || isAccessMethod(owner, name))) {
         // It's an invocation to a primitive boxing method or to a synthetic method for private access, just ignore it.
         visitMethodInstruction(INVOKESTATIC, owner, name, desc, itf);
      }
      else if (opcode == INVOKEVIRTUAL && owner.equals(blockOwner) && name.startsWith("with")) {
         visitMethodInstruction(INVOKEVIRTUAL, owner, name, desc, itf);

         if (argumentCapturing.registerMatcher(name, desc)) {
            matcherStacks[matcherCount++] = stackSize;
         }
      }
      else if (isUnboxing(opcode, owner, desc)) {
         if (argumentCapturing.justAfterWithCaptureInvocation) {
            generateCodeToReplaceNullWithZeroOnTopOfStack(desc);
            argumentCapturing.justAfterWithCaptureInvocation = false;
         }
         else {
            visitMethodInstruction(opcode, owner, name, desc, itf);
         }
      }
      else if (matcherCount == 0) {
         visitMethodInstruction(opcode, owner, name, desc, itf);
      }
      else {
         parameterTypes = Type.getArgumentTypes(desc);
         int stackAfter = stackSize - sumOfParameterSizes();
         boolean mockedInvocationUsingTheMatchers = stackAfter < matcherStacks[0];

         if (mockedInvocationUsingTheMatchers) {
            generateCallsToMoveArgMatchers(stackAfter);
            argumentCapturing.generateCallsToSetArgumentTypesToCaptureIfAny();
            matcherCount = 0;
         }

         visitMethodInstruction(opcode, owner, name, desc, itf);

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

   private void visitMethodInstruction(int opcode, String owner, String name, String desc, boolean itf)
   {
      int argSize = Type.getArgumentsAndReturnSizes(desc);
      int sizeVariation = (argSize & 0x03) - (argSize >> 2);

      if (opcode == INVOKESTATIC) {
         sizeVariation++;
      }

      stackSize += sizeVariation;
      mw.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   private void generateCodeToReplaceNullWithZeroOnTopOfStack(@NotNull String unboxingMethodDesc)
   {
      char primitiveTypeCode = unboxingMethodDesc.charAt(2);
      visitInsn(POP);

      int zeroOpcode;
      switch (primitiveTypeCode) {
         case 'J': zeroOpcode = LCONST_0; break;
         case 'F': zeroOpcode = FCONST_0; break;
         case 'D': zeroOpcode = DCONST_0; break;
         default: zeroOpcode = ICONST_0;
      }

      visitInsn(zeroOpcode);
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
   public void visitLabel(Label label)
   {
      mw.visitLabel(label);

      if (!label.isDebug()) {
         stackSize = 0;
      }
   }

   @Override
   public void visitTypeInsn(int opcode, @NotNull String type)
   {
      argumentCapturing.registerTypeToCaptureIfApplicable(opcode, type);

      if (opcode == NEW) {
         stackSize++;
      }

      mw.visitTypeInsn(opcode, type);
   }

   @Override
   public void visitIntInsn(int opcode, int operand)
   {
      if (opcode != NEWARRAY) {
         stackSize++;
      }

      mw.visitIntInsn(opcode, operand);
   }

   @Override
   public void visitVarInsn(int opcode, @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") int varIndex)
   {
      argumentCapturing.registerAssignmentToCaptureVariableIfApplicable(this, opcode, varIndex);

      if (opcode != RET) {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitVarInsn(opcode, varIndex);
   }

   @Override
   public void visitLdcInsn(Object cst)
   {
      stackSize++;

      if (cst instanceof Long || cst instanceof Double) {
         stackSize++;
      }

      mw.visitLdcInsn(cst);
   }

   @Override
   public void visitJumpInsn(int opcode, Label label)
   {
      if (opcode != JSR) {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitJumpInsn(opcode, label);
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels)
   {
      stackSize--;
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels)
   {
      stackSize--;
      mw.visitLookupSwitchInsn(dflt, keys, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(String desc, int dims)
   {
      stackSize += 1 - dims;
      mw.visitMultiANewArrayInsn(desc, dims);
   }

   @Override
   public void visitInsn(int opcode)
   {
      if (opcode == RETURN && callEndInvocations) {
         generateCallToActiveInvocationsMethod("endInvocations", "()V");
      }
      else {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitInsn(opcode);
   }
}
