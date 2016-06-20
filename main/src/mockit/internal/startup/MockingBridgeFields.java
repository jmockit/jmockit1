/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.security.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.mockups.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

public final class MockingBridgeFields
{
   private static final MockingBridge[] MOCKING_BRIDGES = {MockedBridge.MB, MockupBridge.MB, MockMethodBridge.MB};

   // JRE classes expected to not be loaded before JMockit gets initialized
   private static final String[] mockingBridgeFieldsHolderCandidates = new String[] {
         "java.lang.NegativeArraySizeException",
         "java.lang.annotation.IncompleteAnnotationException",
         "java.lang.OutOfMemoryError",
         "java.lang.StackOverflowError"
   };

   private static Class<?> mockingBridgeFieldsHolderClass;

   private MockingBridgeFields() {}

   public static String getMockingBridgeFieldsHolderClassName() {
      return mockingBridgeFieldsHolderClass.getName().replace('.', '/');
   }

   static void createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull Instrumentation inst)
   {
      for (String mockingBridgeFieldsHolderCandidate : mockingBridgeFieldsHolderCandidates) {
         if (isClassLoaded(inst, mockingBridgeFieldsHolderCandidate)) {
            // class is already loaded, try the next candidate
            continue;
         }

         try {
            Class<?> mockingBridgeFieldsHolderCandidateClass = createSyntheticFieldsInJREClassToHoldMockingBridges(mockingBridgeFieldsHolderCandidate, inst);
            for (MockingBridge mockingBridge : MOCKING_BRIDGES) {
               mockingBridgeFieldsHolderCandidateClass.getDeclaredField(mockingBridge.id);
            }
            mockingBridgeFieldsHolderClass = mockingBridgeFieldsHolderCandidateClass;
            break;
         } catch (NoSuchFieldException e) {
            // field addition did not work properly, try the next candidate
         } catch (ClassNotFoundException e) {
            // class is not available, try the next candidate
         }
      }
      if (mockingBridgeFieldsHolderClass == null) {
         throw new RuntimeException("No suitable mocking bridge field holder class found");
      }
      setMockingBridgeFields();
   }

   private static boolean isClassLoaded(@Nonnull Instrumentation inst, @Nonnull String clazz) {
      for (Class loadedClass : inst.getAllLoadedClasses()) {
         if (loadedClass.getName().equals(clazz)) {
            return true;
         }
      }
      return false;
   }

   private static Class<?> createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull String mockingBridgeFieldsHolderCandidate, @Nonnull Instrumentation inst) throws ClassNotFoundException {
      ClassFileTransformer trans = new FieldAdditionTransformer(mockingBridgeFieldsHolderCandidate);
      inst.addTransformer(trans);

      try {
         return Class.forName(mockingBridgeFieldsHolderCandidate); // load a JRE class expected to not be loaded initially by the JVM
      }
      finally {
         inst.removeTransformer(trans);
      }
   }

   private static final class FieldAdditionTransformer implements ClassFileTransformer
   {
      private String mockingBridgeFieldsHolderCandidateClassName;

      public FieldAdditionTransformer(@Nonnull String mockingBridgeFieldsHolderCandidate) {

         mockingBridgeFieldsHolderCandidateClassName = mockingBridgeFieldsHolderCandidate.replace('.', '/');
      }

      @Nullable @Override
      public byte[] transform(
         @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
         @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
      {
         if (!mockingBridgeFieldsHolderCandidateClassName.equals(className)) {
            return null;
         }

         ClassReader cr = new ClassReader(classfileBuffer);
         final ClassWriter cw = new ClassWriter(cr);

         ClassVisitor cv = new ClassVisitor(cw) {
            @Override
            public void visitEnd()
            {
               int fieldAccess = ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC;

               for (MockingBridge mockingBridge : MOCKING_BRIDGES) {
                  cw.visitField(fieldAccess, mockingBridge.id, "Ljava/lang/reflect/InvocationHandler;", null, null);
               }
            }
         };

         cr.accept(cv, SKIP_FRAMES);
         return cw.toByteArray();
      }
   }

   static void setMockingBridgeFields()
   {
      for (MockingBridge mockingBridge : MOCKING_BRIDGES) {
         setMockingBridgeField(mockingBridge);
      }
   }

   private static void setMockingBridgeField(@Nonnull MockingBridge mockingBridge)
   {
      try {
         if (mockingBridgeFieldsHolderClass == null) {
            ClassLoader systemCL = ClassLoader.getSystemClassLoader();
            Class<?> initialMockingBridgeFieldsClass = ClassLoad.loadClass(systemCL, MockingBridgeFields.class.getName());

            if (initialMockingBridgeFieldsClass != null) {
               mockingBridgeFieldsHolderClass = FieldReflection.getField(initialMockingBridgeFieldsClass, "mockingBridgeFieldsHolderClass", null);
            }
         }

         if (mockingBridgeFieldsHolderClass != null) {
            mockingBridgeFieldsHolderClass.getDeclaredField(mockingBridge.id).set(null, mockingBridge);
         }
      }
      catch (NoSuchFieldException ignore) {}
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
