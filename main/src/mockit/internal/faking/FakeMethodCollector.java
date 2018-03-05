/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import javax.annotation.*;

import mockit.*;
import mockit.asm.*;
import mockit.internal.*;
import mockit.internal.faking.FakeMethods.FakeMethod;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.asm.Access.*;
import static mockit.asm.ClassReader.Flags.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given fake class which are explicitly annotated
 * as {@link Mock fakes}.
 */
final class FakeMethodCollector extends ClassVisitor
{
   private static final int INVALID_METHOD_ACCESSES = BRIDGE + SYNTHETIC + ABSTRACT + NATIVE;

   @Nonnull private final FakeMethods fakeMethods;
   private boolean collectingFromSuperClass;

   FakeMethodCollector(@Nonnull FakeMethods fakeMethods) { this.fakeMethods = fakeMethods; }

   void collectFakeMethods(@Nonnull Class<?> fakeClass) {
      ClassLoad.registerLoadedClass(fakeClass);
      fakeMethods.setFakeClassInternalName(JavaType.getInternalName(fakeClass));

      Class<?> classToCollectFakesFrom = fakeClass;

      do {
         ClassReader cr = ClassFile.readFromFile(classToCollectFakesFrom);
         cr.accept(this, SKIP_CODE);

         classToCollectFakesFrom = classToCollectFakesFrom.getSuperclass();
         collectingFromSuperClass = true;
      }
      while (classToCollectFakesFrom != MockUp.class);
   }

   /**
    * Adds the method specified to the set of fake methods, as long as it's annotated with <tt>@Mock</tt>.
    *
    * @param signature generic signature for a generic method, ignored since redefinition only needs to consider the "erased" signature
    * @param exceptions zero or more thrown exceptions in the method "throws" clause, also ignored
    */
   @Nullable @Override
   public MethodVisitor visitMethod(int access, @Nonnull String name, @Nonnull String desc, String signature, String[] exceptions) {
      if ((access & INVALID_METHOD_ACCESSES) != 0 || "<init>".equals(name)) {
         return null;
      }

      return new FakeMethodVisitor(access, name, desc);
   }

   private final class FakeMethodVisitor extends MethodVisitor {
      private final int access;
      @Nonnull private final String methodName;
      @Nonnull private final String methodDesc;

      FakeMethodVisitor(int access, @Nonnull String methodName, @Nonnull String methodDesc) {
         this.access = access;
         this.methodName = methodName;
         this.methodDesc = methodDesc;
      }

      @Nullable @Override
      public AnnotationVisitor visitAnnotation(@Nullable String desc) {
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
         @Nonnull String name, @Nonnull String desc, String signature, @Nonnull Label start, @Nonnull Label end, @Nonnegative int index
      ) {
         String classDesc = fakeMethods.getFakeClassInternalName();
         ParameterNames.registerName(classDesc, access, methodName, methodDesc, desc, name, index);
      }
   }
}
