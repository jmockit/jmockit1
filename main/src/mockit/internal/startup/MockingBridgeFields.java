/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

import org.omg.IOP.*;

final class MockingBridgeFields
{
   private static final MockingBridge[] MOCKING_BRIDGES = {MockedBridge.MB, MockupBridge.MB, MockMethodBridge.MB};

   private MockingBridgeFields() {}

   static void createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull Instrumentation inst)
   {
      ClassFileTransformer trans = new FieldAdditionTransformer();
      inst.addTransformer(trans);

      try {
         IORHelper.id(); // loads a JRE class expected to not be loaded initially by the JVM
      }
      finally {
         inst.removeTransformer(trans);
      }

      setMockingBridgeFields();
   }

   private static final class FieldAdditionTransformer implements ClassFileTransformer
   {
      @Nullable @Override
      public byte[] transform(
         @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
         @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
      {
         if (!"org/omg/IOP/IORHelper".equals(className)) {
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
         IORHelper.class.getDeclaredField(mockingBridge.id).set(null, mockingBridge);
      }
      catch (NoSuchFieldException ignore) {}
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
