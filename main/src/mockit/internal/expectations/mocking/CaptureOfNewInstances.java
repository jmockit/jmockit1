/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.capturing.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import static mockit.internal.util.FieldReflection.*;

public class CaptureOfNewInstances extends CaptureOfImplementations
{
   static final class Capture
   {
      @NotNull final MockedType typeMetadata;
      @Nullable private Object originalMockInstance;
      @NotNull private final List<Object> instancesCaptured;

      private Capture(@NotNull MockedType typeMetadata, @Nullable Object originalMockInstance)
      {
         this.typeMetadata = typeMetadata;
         this.originalMockInstance = originalMockInstance;
         instancesCaptured = new ArrayList<Object>(4);
      }

      private boolean isInstanceAlreadyCaptured(@NotNull Object mock)
      {
         return Utilities.containsReference(instancesCaptured, mock);
      }

      private boolean captureInstance(@Nullable Object fieldOwner, @NotNull Object instance)
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

   @NotNull final Map<Class<?>, List<Capture>> baseTypeToCaptures;
   @NotNull private MockedType capturingTypeMetadata;

   CaptureOfNewInstances() { baseTypeToCaptures = new HashMap<Class<?>, List<Capture>>(); }

   @Override
   @NotNull
   protected final BaseClassModifier createModifier(
      @Nullable ClassLoader cl, @NotNull ClassReader cr, @NotNull String baseTypeDesc)
   {
      ExpectationsModifier modifier = new ExpectationsModifier(cl, cr, capturingTypeMetadata);
      modifier.setClassNameForCapturedInstanceMethods(baseTypeDesc);
      return modifier;
   }

   @Override
   protected final void redefineClass(@NotNull Class<?> realClass, @NotNull byte[] modifiedClass)
   {
      new RedefinitionEngine(realClass).redefineMethodsWhileRegisteringTheClass(modifiedClass);
   }

   final void registerCaptureOfNewInstances(@NotNull MockedType typeMetadata, @Nullable Object mockInstance)
   {
      capturingTypeMetadata = typeMetadata;

      Class<?> baseType = typeMetadata.getClassType();

      if (!typeMetadata.isFinalFieldOrParameter()) {
         makeSureAllSubtypesAreModified(baseType, typeMetadata.fieldFromTestClass);
      }

      List<Capture> captures = baseTypeToCaptures.get(baseType);

      if (captures == null) {
         captures = new ArrayList<Capture>();
         baseTypeToCaptures.put(baseType, captures);
      }

      captures.add(new Capture(typeMetadata, mockInstance));
   }

   public final boolean captureNewInstance(@Nullable Object fieldOwner, @NotNull Object mock)
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

      if (capturingTypeMetadata.injectable) {
         if (captureFound != null) {
            TestRun.getExecutingTest().addCapturedInstanceForInjectableMock(captureFound.originalMockInstance, mock);
         }

         constructorModifiedForCaptureOnly = true;
      }
      else if (captureFound != null) {
         TestRun.getExecutingTest().addCapturedInstance(captureFound.originalMockInstance, mock);
      }

      return constructorModifiedForCaptureOnly;
   }

   @Nullable private List<Capture> findCaptures(@NotNull Class<?> mockedClass)
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
      @Nullable Object fieldOwner, @NotNull Object mock, @NotNull List<Capture> captures)
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

   public final void cleanUp() { baseTypeToCaptures.clear(); }
}