/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.security.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.mockups.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

final class MockingBridgeFields
{
   private static final MockingBridge[] MOCKING_BRIDGES = {MockedBridge.MB, MockupBridge.MB, MockMethodBridge.MB};

   private MockingBridgeFields() {}

   static void createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull Instrumentation instrumentation)
   {
      instrumentation.addTransformer(new FieldAdditionTransformer(instrumentation));
   }

   private static final class FieldAdditionTransformer implements ClassFileTransformer
   {
      @Nonnull private final Instrumentation instrumentation;

      FieldAdditionTransformer(@Nonnull Instrumentation instrumentation) { this.instrumentation = instrumentation; }

      @Nullable @Override
      public byte[] transform(
         @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
         @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
      {
         if (MockingBridge.hostClassName == null) {
            if (loader == null) { // first, adds the fields to the first public JRE class to be loaded
               ClassReader cr = new ClassReader(classfileBuffer);

               if (isPublic(cr.getAccess())) {
                  MockingBridge.hostClassName = className;
                  return getModifiedJREClassWithAddedFields(cr);
               }
            }
         }
         else { // second, sets the fields; at this point, the transformer is removed
            instrumentation.removeTransformer(this);
            setMockingBridgeFields();
         }

         return null;
      }

      @Nonnull
      private byte[] getModifiedJREClassWithAddedFields(@Nonnull ClassReader classReader)
      {
         final ClassWriter cw = new ClassWriter(classReader);

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

         classReader.accept(cv, SKIP_FRAMES);
         return cw.toByteArray();
      }
   }

   static void setMockingBridgeFields()
   {
      Class<?> hostClass = ClassLoad.loadByInternalName(MockingBridge.hostClassName);

      for (MockingBridge mockingBridge : MOCKING_BRIDGES) {
         setMockingBridgeField(hostClass, mockingBridge);
      }
   }

   private static void setMockingBridgeField(@Nonnull Class<?> hostClass, @Nonnull MockingBridge mockingBridge)
   {
      try {
         hostClass.getDeclaredField(mockingBridge.id).set(null, mockingBridge);
      }
      catch (NoSuchFieldException ignore) {}
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
