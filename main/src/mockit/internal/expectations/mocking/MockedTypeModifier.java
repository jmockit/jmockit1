/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.expectations.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.BaseClassModifier.*;

final class MockedTypeModifier
{
   private MockedTypeModifier() {}

   static void generateDirectCallToHandler(
      @Nonnull MethodWriter mw, @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String genericSignature)
   {
      generateDirectCallToHandler(mw, className, access, name, desc, genericSignature, ExecutionMode.Regular);
   }

   static void generateDirectCallToHandler(
      @Nonnull MethodWriter mw, @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String genericSignature, @Nonnull ExecutionMode executionMode)
   {
      // First argument: the mock instance, if any.
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod(mw, access);

      // Second argument: method access flags.
      mw.visitLdcInsn(access);

      // Third argument: class name.
      mw.visitLdcInsn(className);

      // Fourth argument: method signature.
      mw.visitLdcInsn(name + desc);

      // Fifth argument: generic signature, or null if none.
      generateInstructionToLoadNullableString(mw, genericSignature);

      // Sixth argument: indicate regular or special modes of execution.
      mw.visitLdcInsn(executionMode.ordinal());

      // Seventh argument: array with invocation arguments.
      JavaType[] argTypes = JavaType.getArgumentTypes(desc);
      int argCount = argTypes.length;

      if (argCount == 0) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         generateCodeToCreateArrayOfObject(mw, argCount);
         generateCodeToFillArrayWithParameterValues(mw, argTypes, 0, isStatic ? 0 : 1);
      }

      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/expectations/RecordAndReplayExecution", "recordOrReplay",
         "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;)" +
         "Ljava/lang/Object;", false);
   }

   private static void generateInstructionToLoadNullableString(@Nonnull MethodWriter mw, @Nullable String text)
   {
      if (text == null) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         mw.visitLdcInsn(text);
      }
   }
}
