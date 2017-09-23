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
import mockit.internal.faking.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.external.asm.Opcodes.*;

final class MockingBridgeFields
{
   private MockingBridgeFields() {}

   @Nonnull
   static String createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull Instrumentation instrumentation)
   {
      FieldAdditionTransformer fieldAdditionTransformer = new FieldAdditionTransformer(instrumentation);
      instrumentation.addTransformer(fieldAdditionTransformer);

      // Loads some JRE classes expected to not be loaded yet.
      NegativeArraySizeException.class.getName();

      if (fieldAdditionTransformer.hostClassName == null) {
         Provider.class.getName();
      }

      return fieldAdditionTransformer.hostClassName;
   }

   private static final class FieldAdditionTransformer implements ClassFileTransformer
   {
      private static final int FIELD_ACCESS = ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC;
      @Nonnull private final Instrumentation instrumentation;
      String hostClassName;

      FieldAdditionTransformer(@Nonnull Instrumentation instrumentation) { this.instrumentation = instrumentation; }

      @Nullable @Override
      public byte[] transform(
         @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
         @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
      {
         if (loader == null && hostClassName == null) { // adds the fields to the first public JRE class to be loaded
            ClassReader cr = new ClassReader(classfileBuffer);

            if (isPublic(cr.getAccess())) {
               instrumentation.removeTransformer(this);
               hostClassName = className;
               return getModifiedJREClassWithAddedFields(cr);
            }
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
               addField(MockedBridge.MB);
               addField(FakeBridge.MB);
               addField(FakeMethodBridge.MB);
            }

            private void addField(@Nonnull MockingBridge mb)
            {
               cw.visitField(FIELD_ACCESS, mb.id, "Ljava/lang/reflect/InvocationHandler;", null, null);
            }
         };

         classReader.accept(cv, SKIP_FRAMES);
         return cw.toByteArray();
      }
   }
}
