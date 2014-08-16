/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

import org.jetbrains.annotations.*;

final class ParameterNameExtractor extends ClassVisitor
{
   private final boolean forMethods;
   @NotNull private String classDesc;
   private int methodAccess;
   @NotNull private String methodName;
   @NotNull private String methodDesc;

   ParameterNameExtractor(boolean forMethods)
   {
      this.forMethods = forMethods;
      classDesc = methodName = methodDesc = "";
   }

   @NotNull String extractNames(@NotNull Class<?> classOfInterest)
   {
      String className = classOfInterest.getName();
      classDesc = className.replace('.', '/');

      if (!ParameterNames.hasNamesForClass(classDesc)) {
         // Reads class from file, since JRE 1.6 (but not 1.7) discards parameter names on retransformation.
         ClassReader cr = ClassFile.readFromFile(classDesc);
         cr.accept(this, SKIP_FRAMES);
      }

      return classDesc;
   }

   @Override @Nullable
   public MethodVisitor visitMethod(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if ((access & ACC_SYNTHETIC) == 0) {
         boolean visitingAMethod = name.charAt(0) != '<';

         if (visitingAMethod == forMethods) {
            methodAccess = access;
            methodName = name;
            methodDesc = desc;
            return new MethodOrConstructorVisitor();
         }
      }

      return null;
   }

   private final class MethodOrConstructorVisitor extends MethodVisitor
   {
      @Nullable private String previousDesc;
      private int previousIndex;

      @Override
      public void visitLocalVariable(
         @NotNull String name, @NotNull String desc, @Nullable String signature,
         @NotNull Label start, @NotNull Label end, int index)
      {
         int parameterIndex = index;

         if ("J".equals(previousDesc) || "D".equals(previousDesc)) {
            parameterIndex = previousIndex + 1;
         }

         ParameterNames.registerName(classDesc, methodAccess, methodName, methodDesc, name, parameterIndex);
         previousIndex = parameterIndex;
         previousDesc = desc;
      }
   }
}
