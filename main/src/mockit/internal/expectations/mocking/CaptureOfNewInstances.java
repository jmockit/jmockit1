/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.instrument.*;
import java.util.*;
import javax.annotation.*;

import mockit.asm.*;
import mockit.internal.*;
import mockit.internal.capturing.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;

public final class CaptureOfNewInstances extends CaptureOfImplementations<MockedType>
{
   @Nonnull private final List<Class<?>> baseTypes;
   @Nonnull private final List<Class<?>> partiallyMockedBaseTypes;

   CaptureOfNewInstances() {
      baseTypes = new ArrayList<Class<?>>();
      partiallyMockedBaseTypes = new ArrayList<Class<?>>();
   }

   void useDynamicMocking(@Nonnull Class<?> baseType) {
      partiallyMockedBaseTypes.add(baseType);

      List<Class<?>> mockedClasses = TestRun.mockFixture().getMockedClasses();

      for (Class<?> mockedClass : mockedClasses) {
         if (baseType.isAssignableFrom(mockedClass)) {
            if (mockedClass != baseType || !baseType.isInterface()) {
               redefineClassForDynamicPartialMocking(baseType, mockedClass);
            }
         }
      }
   }

   private static void redefineClassForDynamicPartialMocking(@Nonnull Class<?> baseType, @Nonnull Class<?> mockedClass) {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(mockedClass);

      MockedClassModifier modifier = newModifier(mockedClass.getClassLoader(), classReader, baseType, null);
      modifier.useDynamicMocking(true);
      classReader.accept(modifier);
      byte[] modifiedClassfile = modifier.toByteArray();

      Startup.redefineMethods(mockedClass, modifiedClassfile);
   }

   @Nonnull
   private static MockedClassModifier newModifier(
      @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, @Nullable MockedType typeMetadata
   ) {
      MockedClassModifier modifier = new MockedClassModifier(cl, cr, typeMetadata);
      String baseTypeDesc = JavaType.getInternalName(baseType);
      modifier.setClassNameForCapturedInstanceMethods(baseTypeDesc);
      return modifier;
   }

   @Nonnull @Override
   protected BaseClassModifier createModifier(
      @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, @Nullable MockedType typeMetadata
   ) {
      MockedClassModifier modifier = newModifier(cl, cr, baseType, typeMetadata);

      if (partiallyMockedBaseTypes.contains(baseType)) {
         modifier.useDynamicMocking(true);
      }

      return modifier;
   }

   @Override
   protected void redefineClass(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass) {
      ClassDefinition newClassDefinition = new ClassDefinition(realClass, modifiedClass);
      Startup.redefineMethods(newClassDefinition);

      MockFixture mockFixture = TestRun.mockFixture();
      mockFixture.addRedefinedClass(newClassDefinition);
      mockFixture.registerMockedClass(realClass);
   }

   void registerCaptureOfNewInstances(@Nonnull MockedType typeMetadata) {
      Class<?> baseType = typeMetadata.getClassType();

      if (!typeMetadata.isFinalFieldOrParameter()) {
         makeSureAllSubtypesAreModified(typeMetadata);
      }

      if (!baseTypes.contains(baseType)) {
         baseTypes.add(baseType);
      }
   }

   void makeSureAllSubtypesAreModified(@Nonnull MockedType typeMetadata) {
      Class<?> baseType = typeMetadata.getClassType();
      makeSureAllSubtypesAreModified(baseType, typeMetadata.fieldFromTestClass, typeMetadata);
   }

   public boolean captureNewInstance(@Nonnull Object mock) {
      Class<?> mockedClass = mock.getClass();
      return !baseTypes.contains(mockedClass) && isWithCapturing(mockedClass);
   }

   private boolean isWithCapturing(@Nonnull Class<?> mockedClass) {
      Class<?>[] interfaces = mockedClass.getInterfaces();

      for (Class<?> anInterface : interfaces) {
         if (baseTypes.contains(anInterface)) {
            return true;
         }
      }

      Class<?> superclass = mockedClass.getSuperclass();
      return superclass != Object.class && (baseTypes.contains(superclass) || isWithCapturing(superclass));
   }

   public void cleanUp() {
      baseTypes.clear();
      partiallyMockedBaseTypes.clear();
   }
}