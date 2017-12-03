/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.lang.instrument.*;
import java.security.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.util.*;

public final class ExpectationsTransformer implements ClassFileTransformer
{
   private static final String BASE_CLASSES =
      "mockit/Expectations mockit/Verifications " +
      "mockit/VerificationsInOrder mockit/FullVerifications mockit/FullVerificationsInOrder";

   @Nullable @Override
   public byte[] transform(
      @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
   {
      if (classBeingRedefined == null && protectionDomain != null) {
         boolean anonymousClass = ClassNaming.isAnonymousClass(className);

         if (anonymousClass && !className.startsWith("mockit/internal/") && !className.startsWith("org/junit/")) {
            ClassReader cr = new ClassReader(classfileBuffer);
            String superClassName = cr.getSuperName();

            if (superClassName == null || !BASE_CLASSES.contains(superClassName)) {
               return null;
            }

            return modifyInvocationsSubclass(cr, className);
         }
      }

      return null;
   }

   @Nullable
   private static byte[] modifyInvocationsSubclass(@Nonnull ClassReader cr, @Nonnull final String classDesc)
   {
      ClassWriter cw = new ClassWriter(cr);

      ClassVisitor modifier = new WrappingClassVisitor(cw) {
         @Override
         public MethodVisitor visitMethod(
            int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature,
            @Nullable String[] exceptions)
         {
            MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);

            if (!"<init>".equals(name)) {
               return mw;
            }

            return new InvocationBlockModifier(mw, classDesc);
         }
      };

      try {
         cr.accept(modifier);
         return modifier.toByteArray();
      }
      catch (VisitInterruptedException ignore) {}
      catch (Throwable e) { e.printStackTrace(); }

      return null;
   }
}
