/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.capturing;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import static mockit.internal.util.GeneratedClasses.*;

public final class CaptureTransformer implements ClassFileTransformer
{
   @NotNull private final CapturedType capturedType;
   @NotNull private final String capturedTypeDesc;
   @NotNull private final CaptureOfImplementations captureOfImplementations;
   @NotNull private final Map<ClassIdentification, byte[]> transformedClasses;
   private boolean inactive;

   CaptureTransformer(
      @NotNull CapturedType capturedType, @NotNull CaptureOfImplementations captureOfImplementations,
      boolean registerTransformedClasses)
   {
      this.capturedType = capturedType;
      capturedTypeDesc = Type.getInternalName(capturedType.baseType);
      this.captureOfImplementations = captureOfImplementations;
      transformedClasses = registerTransformedClasses ?
         new HashMap<ClassIdentification, byte[]>(2) : Collections.<ClassIdentification, byte[]>emptyMap();
   }

   public void deactivate()
   {
      inactive = true;

      if (!transformedClasses.isEmpty()) {
         RedefinitionEngine redefinitionEngine = new RedefinitionEngine();

         for (Map.Entry<ClassIdentification, byte[]> classNameAndOriginalBytecode : transformedClasses.entrySet()) {
            ClassIdentification classId = classNameAndOriginalBytecode.getKey();
            byte[] originalBytecode = classNameAndOriginalBytecode.getValue();
            redefinitionEngine.restoreToDefinition(classId.getLoadedClass(), originalBytecode);
         }

         transformedClasses.clear();
      }
   }

   @Override @Nullable
   public byte[] transform(
      @Nullable ClassLoader loader, @NotNull String classDesc, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @NotNull byte[] classfileBuffer)
   {
      if (
         classBeingRedefined != null || inactive || CapturedType.isNotToBeCaptured(loader, protectionDomain, classDesc)
      ) {
         return null;
      }

      ClassReader cr = new ClassReader(classfileBuffer);
      SuperTypeCollector superTypeCollector = new SuperTypeCollector(loader);

      try {
         cr.accept(superTypeCollector, ClassReader.SKIP_DEBUG);
      }
      catch (VisitInterruptedException ignore) {
         if (superTypeCollector.classExtendsCapturedType && !isGeneratedClass(classDesc)) {
            String className = classDesc.replace('/', '.');
            return modifyAndRegisterClass(loader, className, cr);
         }
      }

      return null;
   }

   @NotNull
   private byte[] modifyAndRegisterClass(
      @Nullable ClassLoader loader, @NotNull String className, @NotNull ClassReader cr)
   {
      ClassVisitor modifier = captureOfImplementations.createModifier(loader, cr, capturedTypeDesc);
      cr.accept(modifier, 0);

      ClassIdentification classId = new ClassIdentification(loader, className);
      byte[] originalBytecode = cr.b;

      if (transformedClasses == Collections.<ClassIdentification, byte[]>emptyMap()) {
         TestRun.mockFixture().addTransformedClass(classId, originalBytecode);
      }
      else {
         transformedClasses.put(classId, originalBytecode);
      }

      TestRun.mockFixture().registerMockedClass(capturedType.baseType);
      return modifier.toByteArray();
   }

   private final class SuperTypeCollector extends ClassVisitor
   {
      @Nullable private final ClassLoader loader;
      boolean classExtendsCapturedType;

      private SuperTypeCollector(@Nullable ClassLoader loader) { this.loader = loader; }

      @Override
      public void visit(
         int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         classExtendsCapturedType = false;

         if (capturedTypeDesc.equals(superName)) {
            classExtendsCapturedType = true;
         }
         else if (interfaces != null && interfaces.length > 0) {
            for (String implementedInterface : interfaces) {
               if (capturedTypeDesc.equals(implementedInterface)) {
                  classExtendsCapturedType = true;
                  break;
               }
            }
         }

         if (superName != null && !classExtendsCapturedType && !"java/lang/Object mockit/MockUp".contains(superName)) {
            ClassReader cr = ClassFile.createClassFileReader(loader, superName);
            cr.accept(this, ClassReader.SKIP_DEBUG);
         }

         throw VisitInterruptedException.INSTANCE;
      }
   }
}
