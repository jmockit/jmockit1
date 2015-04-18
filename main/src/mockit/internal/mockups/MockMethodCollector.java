/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import javax.annotation.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
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

   @Nonnull private final MockMethods mockMethods;

   private boolean collectingFromSuperClass;
   @Nullable private String enclosingClassDescriptor;

   MockMethodCollector(@Nonnull MockMethods mockMethods) { this.mockMethods = mockMethods; }

   void collectMockMethods(@Nonnull Class<?> mockClass)
   {
      ClassLoad.registerLoadedClass(mockClass);

      Class<?> classToCollectMocksFrom = mockClass;

      do {
         ClassReader mcReader = ClassFile.createReaderOrGetFromCache(classToCollectMocksFrom);
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
         mockMethods.setMockClassInternalName(name);

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
      final int access, @Nonnull final String methodName, @Nonnull final String methodDesc,
      @Nullable String methodSignature, @Nullable String[] exceptions)
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

      return new MethodVisitor()
      {
         @Override
         @Nullable public AnnotationVisitor visitAnnotation(String desc, boolean visible)
         {
            if ("Lmockit/Mock;".equals(desc)) {
               MockMethods.MockMethod mockMethod =
                  mockMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

               if (mockMethod != null) {
                  return new MockAnnotationVisitor(mockMethod);
               }
            }

            return null;
         }

         @Override
         public void visitLocalVariable(
            @Nonnull String name, @Nonnull String desc, @Nullable String signature,
            @Nonnull Label start, @Nonnull Label end, int index)
         {
            String classDesc = mockMethods.getMockClassInternalName();
            ParameterNames.registerName(classDesc, access, methodName, methodDesc, name, index);
         }
      };
   }

   private final class MockAnnotationVisitor extends AnnotationVisitor
   {
      @Nonnull private final MockMethods.MockMethod mockMethod;
      @Nullable private MockState mockState;

      private MockAnnotationVisitor(@Nonnull MockMethods.MockMethod mockMethod)
      {
         this.mockMethod = mockMethod;

         if (mockMethod.requiresMockState()) {
            getMockState();
         }
      }

      @Override
      public void visit(String name, Object value)
      {
         Integer numInvocations = (Integer) value;

         if ("invocations".equals(name)) {
            getMockState().expectedInvocations = numInvocations;
         }
         else if ("minInvocations".equals(name)) {
            getMockState().minExpectedInvocations = numInvocations;
         }
         else { // "maxInvocations"
            getMockState().maxExpectedInvocations = numInvocations;
         }
      }

      @Nonnull
      private MockState getMockState()
      {
         if (mockState == null) {
            mockState = new MockState(mockMethod);
         }

         return mockState;
      }

      @Override
      public void visitEnd()
      {
         if (mockState != null) {
            if (mockMethod.canBeReentered()) {
               mockState.makeReentrant();
            }

            mockMethods.addMockState(mockState);
         }
      }
   }
}
