/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import javax.annotation.*;

import static java.lang.Character.isDigit;

import mockit.external.asm.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.util.TypeConversion.*;

@SuppressWarnings("OverlyComplexClass")
final class InvocationBlockModifier extends MethodVisitor
{
   private static final String CLASS_DESC = Type.getInternalName(ActiveInvocations.class);
   private static final Type[] NO_PARAMETERS = new Type[0];
   private static final MockFixture MOCK_FIXTURE = TestRun.mockFixture();
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

   @Nonnull private final MethodWriter mw;

   // Input data:
   @Nonnull private final String blockOwner;
   private final boolean callEndInvocations;

   // Takes care of withCapture() matchers, if any:
   private final boolean verifications;
   private boolean justAfterWithCaptureInvocation;
   @Nonnull private final ArgumentCapturing argumentCapturing;

   // Stores the index of the local variable holding a list passed in a withCapture(List) call, if any:
   private int lastLoadedVarIndex;

   private int aload0Count;

   // Helper fields that allow argument matchers to be moved to the correct positions of their
   // corresponding parameters:
   @Nonnull private final int[] matcherStacks;
   private int matcherCount;
   private int stackSize;
   @Nonnull private Type[] parameterTypes;

   final class Capture
   {
      final int opcode;
      final int varIndex;
      @Nullable String typeToCapture;
      private int parameterIndex;
      private boolean parameterIndexFixed;

      Capture(int opcode, int varIndex, @Nullable String typeToCapture)
      {
         this.opcode = opcode;
         this.varIndex = varIndex;
         this.typeToCapture = typeToCapture;
         parameterIndex = matcherCount - 1;
      }

      Capture(int varIndex)
      {
         opcode = ALOAD;
         this.varIndex = varIndex;
         parameterIndex = matcherCount;
      }

      /**
       * Generates bytecode that will be responsible for performing the following steps:
       * 1. Get the argument value (an Object) for the last matched invocation.
       * 2. Cast to a reference type or unbox to a primitive type, as needed.
       * 3. Store the converted value in its local variable.
       */
      void generateCodeToStoreCapturedValue()
      {
         if (opcode != ALOAD) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            generateCallToActiveInvocationsMethod("matchedArgument", "(I)Ljava/lang/Object;");

            Type argType = getArgumentType();
            generateCastOrUnboxing(mw, argType, opcode);

            mw.visitVarInsn(opcode, varIndex);
         }
      }

      @Nonnull
      private Type getArgumentType()
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
         if (opcode == ALOAD) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            mw.visitLdcInsn(varIndex);
            generateCallToActiveInvocationsMethod("setExpectedArgumentType", "(II)V");
         }
         else if (typeToCapture != null && !isTypeToCaptureSameAsParameterType(typeToCapture)) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            mw.visitLdcInsn(typeToCapture);
            generateCallToActiveInvocationsMethod("setExpectedArgumentType", "(ILjava/lang/String;)V");
         }
      }

      private boolean isTypeToCaptureSameAsParameterType(@Nonnull String typeDesc)
      {
         Type parameterType = parameterTypes[parameterIndex];
         int sort = parameterType.getSort();

         if (sort == Type.OBJECT || sort == Type.ARRAY) {
            return typeDesc.equals(parameterType.getInternalName());
         }

         return isPrimitiveWrapper(typeDesc);
      }
   }

   InvocationBlockModifier(
      @Nonnull MethodWriter mw, @Nonnull String blockOwner, boolean callEndInvocations, boolean verifications)
   {
      super(mw);
      this.mw = mw;
      this.blockOwner = blockOwner;
      this.callEndInvocations = callEndInvocations;
      matcherStacks = new int[40];
      this.verifications = verifications;
      argumentCapturing = new ArgumentCapturing(this);
      parameterTypes = NO_PARAMETERS;
   }

   private void generateCallToActiveInvocationsMethod(@Nonnull String name, @Nonnull String desc)
   {
      visitMethodInstruction(INVOKESTATIC, CLASS_DESC, name, desc, false);
   }

   @Override
   public void visitFieldInsn(int opcode, String owner, String name, String desc)
   {
      boolean getField = opcode == GETFIELD;

      if ((getField || opcode == PUTFIELD) && blockOwner.equals(owner)) {
         aload0Count--;

         if (name.indexOf('$') > 0) {
            // Nothing to do.
         }
         else if (getField && name.startsWith("any") && ANY_FIELDS.contains(name)) {
            generateCodeToAddArgumentMatcherForAnyField(owner, name, desc);
            return;
         }
         else if (!getField && generateCodeThatReplacesAssignmentToSpecialField(name)) {
            visitInsn(POP);
            return;
         }
      }

      stackSize += stackSizeVariationForFieldAccess(opcode, desc);
      mw.visitFieldInsn(opcode, owner, name, desc);
   }

   private boolean generateCodeThatReplacesAssignmentToSpecialField(@Nonnull String fieldName)
   {
      if ("result".equals(fieldName)) {
         generateCallToActiveInvocationsMethod("addResult", "(Ljava/lang/Object;)V");
         return true;
      }

      if ("times".equals(fieldName) || "maxTimes".equals(fieldName)) {
         generateCallToActiveInvocationsMethod(fieldName, "(I)V");
         return true;
      }

      if ("minTimes".equals(fieldName)) {
         String methodToCall = fieldName;
         int p = blockOwner.lastIndexOf('$');

         if (p < 0 || !isDigit(blockOwner.charAt(p + 1))) {
            methodToCall = "minTimes0";
         }

         generateCallToActiveInvocationsMethod(methodToCall, "(I)V");
         return true;
      }

      return false;
   }

   private void generateCodeToAddArgumentMatcherForAnyField(
      @Nonnull String fieldOwner, @Nonnull String name, @Nonnull String desc)
   {
      mw.visitFieldInsn(GETFIELD, fieldOwner, name, desc);
      generateCallToActiveInvocationsMethod(name, "()V");
      matcherStacks[matcherCount++] = stackSize;
   }

   private static int stackSizeVariationForFieldAccess(int opcode, @Nonnull String fieldType)
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
      else if (isCallToArgumentMatcher(opcode, owner, name, desc)) {
         visitMethodInstruction(INVOKEVIRTUAL, owner, name, desc, itf);

         boolean withCaptureMethod = "withCapture".equals(name);

         if (argumentCapturing.registerMatcher(withCaptureMethod, desc, lastLoadedVarIndex)) {
            justAfterWithCaptureInvocation = withCaptureMethod;
            matcherStacks[matcherCount++] = stackSize;
         }

         aload0Count--;
      }
      else if (isUnboxing(opcode, owner, desc)) {
         if (justAfterWithCaptureInvocation) {
            generateCodeToReplaceNullWithZeroOnTopOfStack(desc);
            justAfterWithCaptureInvocation = false;
         }
         else {
            visitMethodInstruction(opcode, owner, name, desc, itf);
         }
      }
      else {
         checkForInvocationThatIsNotMockable(owner, name);
         handleMockedOrNonMockedInvocation(opcode, owner, name, desc, itf);
         aload0Count = 0;
      }
   }

   private boolean isAccessMethod(@Nonnull String methodOwner, @Nonnull String name)
   {
      return !methodOwner.equals(blockOwner) && name.startsWith("access$");
   }

   private void visitMethodInstruction(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
   {
      int argSize = Type.getArgumentsAndReturnSizes(desc);
      int sizeVariation = (argSize & 0x03) - (argSize >> 2);

      if (opcode == INVOKESTATIC) {
         sizeVariation++;
      }

      stackSize += sizeVariation;
      mw.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   private boolean isCallToArgumentMatcher(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc)
   {
      return
         opcode == INVOKEVIRTUAL && owner.equals(blockOwner) &&
         name.startsWith("with") && WITH_METHODS.contains(name + desc);
   }

   private void generateCodeToReplaceNullWithZeroOnTopOfStack(@Nonnull String unboxingMethodDesc)
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

   private void checkForInvocationThatIsNotMockable(@Nonnull String owner, @Nonnull String name)
   {
      if (isMockedClass(owner)) {
         if (MockingFilters.isUnmockable(owner, name)) {
            generateCodeToThrowException(
               "Attempted to " + (verifications ? "verify" : "record") +
               " expectation on unmockable " + (name.charAt(0) == '<' ? "constructor" : "method"));
         }
         else if (aload0Count > 0 && name.charAt(0) != '<') {
            generateCodeToThrowException("Invalid invocation to another mocked method during unfinished recording");
         }
      }
   }

   private static boolean isMockedClass(String owner) { return MOCK_FIXTURE.isMockedClass(owner.replace('/', '.')); }

   private void handleMockedOrNonMockedInvocation(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
   {
      if (matcherCount == 0) {
         visitMethodInstruction(opcode, owner, name, desc, itf);
      }
      else {
         boolean mockedInvocationUsingTheMatchers = handleInvocationParameters(desc);
         visitMethodInstruction(opcode, owner, name, desc, itf);
         handleArgumentCapturingIfNeeded(mockedInvocationUsingTheMatchers);
      }
   }

   private boolean handleInvocationParameters(@Nonnull String desc)
   {
      parameterTypes = Type.getArgumentTypes(desc);
      int stackAfter = stackSize - sumOfParameterSizes();
      boolean mockedInvocationUsingTheMatchers = stackAfter < matcherStacks[0];

      if (mockedInvocationUsingTheMatchers) {
         generateCallsToMoveArgMatchers(stackAfter);
         argumentCapturing.generateCallsToSetArgumentTypesToCaptureIfAny();
         matcherCount = 0;
      }

      return mockedInvocationUsingTheMatchers;
   }

   @Nonnegative
   private int sumOfParameterSizes()
   {
      int sum = 0;

      for (Type argType : parameterTypes) {
         sum += argType.getSize();
      }

      return sum;
   }

   private void generateCallsToMoveArgMatchers(@Nonnegative int initialStack)
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

   private void generateCallToMoveArgMatcher(@Nonnegative int originalMatcherIndex, @Nonnegative int toIndex)
   {
      mw.visitIntInsn(SIPUSH, originalMatcherIndex);
      mw.visitIntInsn(SIPUSH, toIndex);
      generateCallToActiveInvocationsMethod("moveArgMatcher", "(II)V");
   }

   private void handleArgumentCapturingIfNeeded(boolean mockedInvocationUsingTheMatchers)
   {
      if (mockedInvocationUsingTheMatchers) {
         argumentCapturing.generateCallsToCaptureMatchedArgumentsIfPending();
      }

      justAfterWithCaptureInvocation = false;
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
   public void visitTypeInsn(int opcode, @Nonnull String type)
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
      if (opcode == ALOAD) {
         lastLoadedVarIndex = varIndex;

         if (varIndex == 0) {
            aload0Count++;
         }
      }

      argumentCapturing.registerAssignmentToCaptureVariableIfApplicable(opcode, varIndex);

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

      boolean labelIsFromCompiledSourceCode = label.line > 0 || label.position > 0;

      if (labelIsFromCompiledSourceCode) {
         boolean verifyingWithCaptures = verifications && argumentCapturing.hasCaptures();

         if (!verifyingWithCaptures) {
            generateCodeToThrowExceptionReportingInvalidSyntax("conditional");
         }
      }

      mw.visitJumpInsn(opcode, label);
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels)
   {
      stackSize--;
      generateCodeToThrowExceptionReportingInvalidSyntax("switch");
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels)
   {
      stackSize--;
      generateCodeToThrowExceptionReportingInvalidSyntax("switch");
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

   @Override
   public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
   {
      String description = type == null ? "try/finally" : "try/catch";
      generateCodeToThrowExceptionReportingInvalidSyntax(description);
      mw.visitTryCatchBlock(start, end, handler, type);
   }

   private void generateCodeToThrowExceptionReportingInvalidSyntax(@Nonnull String description)
   {
      if (callEndInvocations) {
         generateCodeToThrowException("Invalid " + description + " statement inside expectation block");
      }
   }

   private void generateCodeToThrowException(@Nonnull String message)
   {
      mw.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
      mw.visitInsn(DUP);
      mw.visitLdcInsn(message);
      mw.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
      mw.visitInsn(ATHROW);
   }

   @Override
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      int index)
   {
      if (signature != null) {
         argumentCapturing.registerTypeToCaptureIntoListIfApplicable(index, signature);
      }

      // In classes instrumented with EMMA some local variable information can be lost, so we discard it entirely to
      // avoid a ClassFormatError.
      if (end.position > 0) {
         mw.visitLocalVariable(name, desc, signature, start, end, index);
      }
   }
}
