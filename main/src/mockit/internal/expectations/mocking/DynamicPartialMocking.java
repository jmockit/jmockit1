/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;

import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.internal.util.AutoBoxing.*;
import static mockit.internal.util.GeneratedClasses.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public final class DynamicPartialMocking extends BaseTypeRedefinition
{
   @NotNull public final List<Object> targetInstances;
   @NotNull private final Map<Class<?>, byte[]> modifiedClassfiles;
   private boolean methodsOnly;

   public DynamicPartialMocking()
   {
      targetInstances = new ArrayList<Object>(2);
      modifiedClassfiles = new HashMap<Class<?>, byte[]>();
   }

   public void redefineTypes(@NotNull Object[] classesOrInstancesToBePartiallyMocked)
   {
      for (Object classOrInstance : classesOrInstancesToBePartiallyMocked) {
         redefineClassHierarchy(classOrInstance);
      }

      if (!modifiedClassfiles.isEmpty()) {
         new RedefinitionEngine().redefineMethods(modifiedClassfiles);
         modifiedClassfiles.clear();
      }
   }

   private void redefineClassHierarchy(@NotNull Object classOrInstance)
   {
      Object mockInstance;

      if (classOrInstance instanceof Class) {
         mockInstance = null;
         targetClass = (Class<?>) classOrInstance;
         CaptureOfNewInstances capture = TestRun.mockFixture().findCaptureOfImplementations(targetClass);

         if (capture != null) {
            capture.useDynamicMocking(targetClass);
            return;
         }

         applyPartialMockingToGivenClass();
      }
      else {
         mockInstance = classOrInstance;
         targetClass = getMockedClass(classOrInstance);
         applyPartialMockingToGivenInstance(classOrInstance);
      }

      InstanceFactory instanceFactory = createInstanceFactory(targetClass);
      instanceFactory.lastInstance = mockInstance;

      TestRun.mockFixture().registerInstanceFactoryForMockedType(targetClass, instanceFactory);
      TestRun.getExecutingTest().getCascadingTypes().add(false, targetClass, mockInstance);
   }

   private void applyPartialMockingToGivenClass()
   {
      validateTargetClassType();
      ensureThatClassIsInitialized(targetClass);
      methodsOnly = false;
      redefineMethodsAndConstructorsInTargetType();
   }

   private void applyPartialMockingToGivenInstance(@NotNull Object instance)
   {
      validateTargetClassType();
      methodsOnly = true;
      redefineMethodsAndConstructorsInTargetType();
      targetInstances.add(instance);
   }

   private void validateTargetClassType()
   {
      if (
         targetClass.isInterface() || targetClass.isAnnotation() ||
         targetClass.isArray() || targetClass.isPrimitive() ||
         isWrapperOfPrimitiveType(targetClass) ||
         isGeneratedImplementationClass(targetClass)
      ) {
         throw new IllegalArgumentException("Invalid type for partial mocking: " + targetClass);
      }

      if (
         !modifiedClassfiles.containsKey(targetClass) &&
         TestRun.mockFixture().isMockedClass(targetClass) &&
         !TestRun.getExecutingTest().isClassWithInjectableMocks(targetClass)
      ) {
         throw new IllegalArgumentException("Class is already mocked: " + targetClass);
      }
   }

   @Override
   void configureClassModifier(@NotNull ExpectationsModifier modifier)
   {
      modifier.useDynamicMocking(methodsOnly);
   }

   @Override
   void applyClassRedefinition(@NotNull Class<?> realClass, @NotNull byte[] modifiedClass)
   {
      modifiedClassfiles.put(realClass, modifiedClass);
   }
}
