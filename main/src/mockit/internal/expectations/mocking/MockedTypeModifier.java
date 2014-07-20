/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.*;

import static mockit.external.asm4.Opcodes.*;

class MockedTypeModifier extends BaseClassModifier
{
   protected MockedTypeModifier(@NotNull ClassReader classReader) { super(classReader); }

   protected final void generateDirectCallToHandler(
      @NotNull String className, int access, @NotNull String name, @NotNull String desc,
      @Nullable String genericSignature)
   {
      generateDirectCallToHandler(className, access, name, desc, genericSignature, ExecutionMode.Regular);
   }

   protected final void generateDirectCallToHandler(
      @NotNull String className, int access, @NotNull String name, @NotNull String desc,
      @Nullable String genericSignature, @NotNull ExecutionMode executionMode)
   {
      // First argument: the mock instance, if any.
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod(access);

      // Second argument: method access flags.
      mw.visitLdcInsn(access);

      // Third argument: class name.
      mw.visitLdcInsn(className);

      // Fourth argument: method signature.
      mw.visitLdcInsn(name + desc);

      // Fifth argument: generic signature, or null if none.
      generateInstructionToLoadNullableString(genericSignature);

      // Sixth argument: indicate regular or special modes of execution.
      mw.visitLdcInsn(executionMode.ordinal());
      
      // Seventh argument: array with invocation arguments.
      Type[] argTypes = Type.getArgumentTypes(desc);
      int argCount = argTypes.length;

      if (argCount == 0) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         generateCodeToCreateArrayOfObject(argCount);
         generateCodeToFillArrayWithParameterValues(argTypes, 0, isStatic ? 0 : 1);
      }

      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/expectations/RecordAndReplayExecution", "recordOrReplay",
         "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;)" +
         "Ljava/lang/Object;");
   }

   private void generateInstructionToLoadNullableString(@Nullable String text)
   {
      if (text == null) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         mw.visitLdcInsn(text);
      }
   }
}
