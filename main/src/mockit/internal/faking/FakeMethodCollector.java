/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import javax.annotation.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.faking.FakeMethods.FakeMethod;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given fake class which are explicitly annotated
 * as {@link Mock fakes}.
 */
final class FakeMethodCollector extends ClassVisitor
{
   private static final int INVALID_METHOD_ACCESSES =
      Access.BRIDGE + Access.SYNTHETIC + Access.ABSTRACT + Access.NATIVE;

   @Nonnull private final FakeMethods fakeMethods;

   private boolean collectingFromSuperClass;
   @Nullable private String enclosingClassDescriptor;

   FakeMethodCollector(@Nonnull FakeMethods fakeMethods) { this.fakeMethods = fakeMethods; }

   void collectFakeMethods(@Nonnull Class<?> fakeClass)
   {
      ClassLoad.registerLoadedClass(fakeClass);

      Class<?> classToCollectFakesFrom = fakeClass;

      do {
         ClassReader mcReader = ClassFile.readFromFile(classToCollectFakesFrom);
         mcReader.accept(this, Flags.SKIP_CODE);
         classToCollectFakesFrom = classToCollectFakesFrom.getSuperclass();
         collectingFromSuperClass = true;
      }
      while (classToCollectFakesFrom != Object.class && classToCollectFakesFrom != MockUp.class);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      if (!collectingFromSuperClass) {
         fakeMethods.setFakeClassInternalName(name);

         int p = name.lastIndexOf('$');

         if (p > 0) {
            enclosingClassDescriptor = "(L" + name.substring(0, p) + ";)V";
         }
      }
   }

   /**
    * Adds the method specified to the set of fake methods, as long as it's annotated with <tt>@Mock</tt>.
    *
    * @param methodSignature generic signature for a Java 5 generic method, ignored since redefinition only needs to
    *                        consider the "erased" signature
    * @param exceptions zero or more thrown exceptions in the method "throws" clause, also ignored
    */
   @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
   @Nullable @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String methodName, @Nonnull String methodDesc, String methodSignature, String[] exceptions)
   {
      if ((access & INVALID_METHOD_ACCESSES) != 0) {
         return null;
      }

      if ("<init>".equals(methodName)) {
         if (!collectingFromSuperClass && methodDesc.equals(enclosingClassDescriptor)) {
            enclosingClassDescriptor = null;
         }

         return null;
      }

      return new FakeMethodVisitor(access, methodName, methodDesc);
   }

   private final class FakeMethodVisitor extends MethodVisitor
   {
      private final int access;
      @Nonnull private final String methodName;
      @Nonnull private final String methodDesc;

      FakeMethodVisitor(int access, @Nonnull String methodName, @Nonnull String methodDesc)
      {
         this.access = access;
         this.methodName = methodName;
         this.methodDesc = methodDesc;
      }

      @Nullable @Override
      public AnnotationVisitor visitAnnotation(@Nullable String desc)
      {
         if ("Lmockit/Mock;".equals(desc)) {
            FakeMethod fakeMethod = fakeMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

            if (fakeMethod != null && fakeMethod.requiresFakeState()) {
               FakeState fakeState = new FakeState(fakeMethod);
               fakeMethods.addFakeState(fakeState);
            }
         }

         return null;
      }

      @Override
      public void visitLocalVariable(
         @Nonnull String name, @Nonnull String desc, String signature, @Nonnull Label start, @Nonnull Label end,
         @Nonnegative int index)
      {
         String classDesc = fakeMethods.getFakeClassInternalName();
         ParameterNames.registerName(classDesc, access, methodName, methodDesc, desc, name, index);
      }
   }
}
