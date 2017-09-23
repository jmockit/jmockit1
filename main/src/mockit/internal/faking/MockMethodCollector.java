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
import static mockit.external.asm.Opcodes.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given mock class which are explicitly annotated
 * as {@link Mock mocks}.
 */
final class MockMethodCollector extends ClassVisitor
{
   private static final int INVALID_METHOD_ACCESSES = ACC_BRIDGE + ACC_SYNTHETIC + ACC_ABSTRACT + ACC_NATIVE;

   @Nonnull private final FakeMethods mockMethods;

   private boolean collectingFromSuperClass;
   @Nullable private String enclosingClassDescriptor;

   MockMethodCollector(@Nonnull FakeMethods mockMethods) { this.mockMethods = mockMethods; }

   void collectMockMethods(@Nonnull Class<?> mockClass)
   {
      ClassLoad.registerLoadedClass(mockClass);

      Class<?> classToCollectMocksFrom = mockClass;

      do {
         ClassReader mcReader = ClassFile.readFromFile(classToCollectMocksFrom);
         mcReader.accept(this, SKIP_CODE + SKIP_FRAMES);
         classToCollectMocksFrom = classToCollectMocksFrom.getSuperclass();
         collectingFromSuperClass = true;
      }
      while (classToCollectMocksFrom != Object.class && classToCollectMocksFrom != MockUp.class);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      if (!collectingFromSuperClass) {
         mockMethods.setFakeClassInternalName(name);

         int p = name.lastIndexOf('$');

         if (p > 0) {
            enclosingClassDescriptor = "(L" + name.substring(0, p) + ";)V";
         }
      }
   }

   /**
    * Adds the method specified to the set of mock methods, as long as it's annotated with {@code @Mock}.
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

      return new MockMethodVisitor(access, methodName, methodDesc);
   }

   private final class MockMethodVisitor extends MethodVisitor
   {
      private final int access;
      @Nonnull private final String methodName;
      @Nonnull private final String methodDesc;

      MockMethodVisitor(int access, @Nonnull String methodName, @Nonnull String methodDesc)
      {
         this.access = access;
         this.methodName = methodName;
         this.methodDesc = methodDesc;
      }

      @Nullable @Override
      public AnnotationVisitor visitAnnotation(@Nullable String desc, boolean visible)
      {
         if ("Lmockit/Mock;".equals(desc)) {
            FakeMethod mockMethod = mockMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

            if (mockMethod != null && mockMethod.requiresFakeState()) {
               MockState mockState = new MockState(mockMethod);
               mockMethods.addMockState(mockState);
            }
         }

         return null;
      }

      @Override
      public void visitLocalVariable(
         @Nonnull String name, @Nonnull String desc, String signature, Label start, Label end, @Nonnegative int index)
      {
         String classDesc = mockMethods.getFakeClassInternalName();
         ParameterNames.registerName(classDesc, access, methodName, methodDesc, desc, name, index);
      }
   }
}
