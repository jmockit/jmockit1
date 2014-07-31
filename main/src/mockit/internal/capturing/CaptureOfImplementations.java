/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.capturing;

import java.lang.reflect.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import static mockit.external.asm4.ClassReader.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public abstract class CaptureOfImplementations<M>
{
   protected CaptureOfImplementations() {}

   public final void makeSureAllSubtypesAreModified(
      @NotNull Class<?> baseType, boolean registerCapturedClasses, @Nullable M typeMetadata)
   {
      if (baseType == TypeVariable.class) {
         throw new IllegalArgumentException("Capturing implementations of multiple base types is not supported");
      }

      CapturedType captureMetadata = new CapturedType(baseType);
      redefineClassesAlreadyLoaded(captureMetadata, baseType, typeMetadata);
      createCaptureTransformer(captureMetadata, registerCapturedClasses, typeMetadata);
   }

   private void redefineClassesAlreadyLoaded(
      @NotNull CapturedType captureMetadata, @NotNull Class<?> baseType, @Nullable M typeMetadata)
   {
      Class<?>[] classesLoaded = Startup.instrumentation().getAllLoadedClasses();

      for (Class<?> aClass : classesLoaded) {
         if (captureMetadata.isToBeCaptured(aClass)) {
            redefineClass(aClass, baseType, typeMetadata);
         }
      }
   }

   public void redefineClass(@NotNull Class<?> realClass, @NotNull Class<?> baseType, @Nullable M typeMetadata)
   {
      if (!TestRun.mockFixture().containsRedefinedClass(realClass)) {
         ClassReader classReader;

         try {
            classReader = ClassFile.createReaderOrGetFromCache(realClass);
         }
         catch (ClassFile.NotFoundException ignore) {
            return;
         }

         ensureThatClassIsInitialized(realClass);

         BaseClassModifier modifier = createModifier(realClass.getClassLoader(), classReader, baseType, typeMetadata);
         classReader.accept(modifier, SKIP_FRAMES);

         if (modifier.wasModified()) {
            byte[] modifiedClass = modifier.toByteArray();
            redefineClass(realClass, modifiedClass);
         }
      }
   }

   @NotNull
   protected abstract BaseClassModifier createModifier(
      @Nullable ClassLoader cl, @NotNull ClassReader cr, @NotNull Class<?> baseType, M typeMetadata);

   protected abstract void redefineClass(@NotNull Class<?> realClass, @NotNull byte[] modifiedClass);

   private void createCaptureTransformer(
      @NotNull CapturedType captureMetadata, boolean registerCapturedClasses, @Nullable M typeMetadata)
   {
      CaptureTransformer<M> transformer =
         new CaptureTransformer<M>(captureMetadata, this, registerCapturedClasses, typeMetadata);

      Startup.instrumentation().addTransformer(transformer, true);
      TestRun.mockFixture().addCaptureTransformer(transformer);
   }
}
