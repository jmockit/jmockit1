/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.capturing.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;
import static mockit.internal.reflection.FieldReflection.*;

public class CaptureOfNewInstances extends CaptureOfImplementations<MockedType>
{
   static final class Capture
   {
      @Nonnull final MockedType typeMetadata;
      @Nullable private Object originalMockInstance;
      @Nonnull private final List<Object> instancesCaptured;

      private Capture(@Nonnull MockedType typeMetadata, @Nullable Object originalMockInstance)
      {
         this.typeMetadata = typeMetadata;
         this.originalMockInstance = originalMockInstance;
         instancesCaptured = new ArrayList<Object>(4);
      }

      private boolean isInstanceAlreadyCaptured(@Nonnull Object mock)
      {
         return Utilities.containsReference(instancesCaptured, mock);
      }

      private boolean captureInstance(@Nullable Object fieldOwner, @Nonnull Object instance)
      {
         if (instancesCaptured.size() < typeMetadata.getMaxInstancesToCapture()) {
            if (fieldOwner != null && typeMetadata.field != null && originalMockInstance == null) {
               originalMockInstance = getFieldValue(typeMetadata.field, fieldOwner);
            }

            instancesCaptured.add(instance);
            return true;
         }

         return false;
      }

      void reset()
      {
         originalMockInstance = null;
         instancesCaptured.clear();
      }
   }

   @Nonnull private final Map<Class<?>, List<Capture>> baseTypeToCaptures;
   @Nonnull private final List<Class<?>> partiallyMockedBaseTypes;

   CaptureOfNewInstances()
   {
      baseTypeToCaptures = new HashMap<Class<?>, List<Capture>>();
      partiallyMockedBaseTypes = new ArrayList<Class<?>>();
   }

   @Nonnull
   protected final Collection<List<Capture>> getCapturesForAllBaseTypes() { return baseTypeToCaptures.values(); }

   void useDynamicMocking(@Nonnull Class<?> baseType)
   {
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

   private static void redefineClassForDynamicPartialMocking(@Nonnull Class<?> baseType, @Nonnull Class<?> mockedClass)
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(mockedClass);

      ExpectationsModifier modifier = newModifier(mockedClass.getClassLoader(), classReader, baseType, null);
      modifier.useDynamicMocking(true);
      classReader.accept(modifier, SKIP_FRAMES);
      byte[] modifiedClassfile = modifier.toByteArray();

      Startup.redefineMethods(mockedClass, modifiedClassfile);
   }

   @Nonnull
   private static ExpectationsModifier newModifier(
      @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, @Nullable MockedType typeMetadata)
   {
      ExpectationsModifier modifier = new ExpectationsModifier(cl, cr, typeMetadata);
      modifier.setClassNameForCapturedInstanceMethods(Type.getInternalName(baseType));
      return modifier;
   }

   @Nonnull @Override
   protected final BaseClassModifier createModifier(
      @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, @Nullable MockedType typeMetadata)
   {
      ExpectationsModifier modifier = newModifier(cl, cr, baseType, typeMetadata);

      if (partiallyMockedBaseTypes.contains(baseType)) {
         modifier.useDynamicMocking(true);
      }

      return modifier;
   }

   @Override
   protected final void redefineClass(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass)
   {
      new RedefinitionEngine(realClass).redefineMethodsWhileRegisteringTheClass(modifiedClass);
   }

   final void registerCaptureOfNewInstances(@Nonnull MockedType typeMetadata, @Nullable Object mockInstance)
   {
      Class<?> baseType = typeMetadata.getClassType();

      if (!typeMetadata.isFinalFieldOrParameter()) {
         makeSureAllSubtypesAreModified(typeMetadata);
      }

      List<Capture> captures = baseTypeToCaptures.get(baseType);

      if (captures == null) {
         captures = new ArrayList<Capture>();
         baseTypeToCaptures.put(baseType, captures);
      }

      captures.add(new Capture(typeMetadata, mockInstance));
   }

   final void makeSureAllSubtypesAreModified(@Nonnull MockedType typeMetadata)
   {
      Class<?> baseType = typeMetadata.getClassType();
      makeSureAllSubtypesAreModified(baseType, typeMetadata.fieldFromTestClass, typeMetadata);
   }

   public final boolean captureNewInstance(@Nullable Object fieldOwner, @Nonnull Object mock)
   {
      Class<?> mockedClass = mock.getClass();
      List<Capture> captures = baseTypeToCaptures.get(mockedClass);
      boolean constructorModifiedForCaptureOnly = captures == null;

      if (constructorModifiedForCaptureOnly) {
         captures = findCaptures(mockedClass);

         if (captures == null) {
            return false;
         }
      }

      Capture captureFound = findCapture(fieldOwner, mock, captures);

      if (captureFound != null) {
         if (captureFound.typeMetadata.injectable) {
            TestRun.getExecutingTest().addCapturedInstanceForInjectableMock(captureFound.originalMockInstance, mock);
            constructorModifiedForCaptureOnly = true;
         }
         else {
            TestRun.getExecutingTest().addCapturedInstance(captureFound.originalMockInstance, mock);
         }
      }

      return constructorModifiedForCaptureOnly;
   }

   @Nullable
   private List<Capture> findCaptures(@Nonnull Class<?> mockedClass)
   {
      Class<?>[] interfaces = mockedClass.getInterfaces();

      for (Class<?> anInterface : interfaces) {
         List<Capture> found = baseTypeToCaptures.get(anInterface);

         if (found != null) {
            return found;
         }
      }

      Class<?> superclass = mockedClass.getSuperclass();

      if (superclass == Object.class) {
         return null;
      }

      List<Capture> found = baseTypeToCaptures.get(superclass);

      return found != null ? found : findCaptures(superclass);
   }

   @Nullable
   private static Capture findCapture(
      @Nullable Object fieldOwner, @Nonnull Object mock, @Nonnull List<Capture> captures)
   {
      for (Capture capture : captures) {
         if (capture.isInstanceAlreadyCaptured(mock)) {
            break;
         }
         else if (capture.captureInstance(fieldOwner, mock)) {
            return capture;
         }
      }

      return null;
   }

   public final void cleanUp()
   {
      baseTypeToCaptures.clear();
      partiallyMockedBaseTypes.clear();
   }
}