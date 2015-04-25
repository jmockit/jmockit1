/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

final class ParameterNameExtractor extends ClassVisitor
{
   private final boolean forMethods;
   @Nonnull private String classDesc;
   private int methodAccess;
   @Nonnull private String methodName;
   @Nonnull private String methodDesc;

   ParameterNameExtractor(boolean forMethods)
   {
      this.forMethods = forMethods;
      classDesc = methodName = methodDesc = "";
   }

   @Nonnull
   String extractNames(@Nonnull Class<?> classOfInterest)
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

   @Nullable @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
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
         @Nonnull String name, @Nonnull String desc, @Nullable String signature,
         @Nonnull Label start, @Nonnull Label end, int index)
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
