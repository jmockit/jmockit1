/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
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
   private final boolean nonStrict;
   private boolean methodsOnly;

   public DynamicPartialMocking(boolean nonStrict)
   {
      targetInstances = new ArrayList<Object>(2);
      modifiedClassfiles = new HashMap<Class<?>, byte[]>();
      this.nonStrict = nonStrict;
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
      if (classOrInstance instanceof Class) {
         targetClass = (Class<?>) classOrInstance;
         CaptureOfNewInstances capture = TestRun.mockFixture().findCaptureOfImplementations(targetClass);

         if (capture != null) {
            capture.useDynamicMocking(targetClass);
            return;
         }

         applyPartialMockingToGivenClass();
      }
      else {
         targetClass = getMockedClass(classOrInstance);
         applyPartialMockingToGivenInstance(classOrInstance);
      }

      InstanceFactory instanceFactory = createInstanceFactory(targetClass);
      TestRun.mockFixture().registerInstanceFactoryForMockedType(targetClass, instanceFactory);

      String mockedTypeDesc = targetClass.getName().replace('.', '/');
      TestRun.getExecutingTest().addCascadingType(mockedTypeDesc, false, targetClass);
   }

   private void applyPartialMockingToGivenClass()
   {
      validateTargetClassType();
      registerAsMocked();
      ensureThatClassIsInitialized(targetClass);
      methodsOnly = false;
      redefineMethodsAndConstructorsInTargetType();
   }

   private void applyPartialMockingToGivenInstance(@NotNull Object instance)
   {
      validateTargetClassType();
      registerAsMocked(instance);
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

   private void registerAsMocked()
   {
      if (nonStrict) {
         ExecutingTest executingTest = TestRun.getExecutingTest();
         Class<?> classToRegister = targetClass;

         do {
            executingTest.registerAsNonStrictlyMocked(classToRegister);
            classToRegister = classToRegister.getSuperclass();
         }
         while (classToRegister != null && classToRegister != Object.class && classToRegister != Proxy.class);
      }
   }

   private void registerAsMocked(@NotNull Object mock)
   {
      if (nonStrict) {
         TestRun.getExecutingTest().registerAsNonStrictlyMocked(mock);
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
