/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import javax.annotation.*;

import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.internal.util.AutoBoxing.*;
import static mockit.internal.util.GeneratedClasses.*;

public final class DynamicPartialMocking extends BaseTypeRedefinition
{
   @Nonnull public final List<Object> targetInstances;
   @Nonnull private final Map<Class<?>, byte[]> modifiedClassfiles;
   private boolean methodsOnly;

   public DynamicPartialMocking()
   {
      targetInstances = new ArrayList<Object>(2);
      modifiedClassfiles = new HashMap<Class<?>, byte[]>();
   }

   public void redefineTypes(@Nonnull Object[] classesOrInstancesToBePartiallyMocked)
   {
      for (Object classOrInstance : classesOrInstancesToBePartiallyMocked) {
         redefineClassHierarchy(classOrInstance);
      }

      if (!modifiedClassfiles.isEmpty()) {
         TestRun.mockFixture().redefineMethods(modifiedClassfiles);
         modifiedClassfiles.clear();
      }
   }

   private void redefineClassHierarchy(@Nonnull Object classOrInstance)
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
      TestRun.getExecutingTest().getCascadingTypes().add(false, targetClass);
   }

   private void applyPartialMockingToGivenClass()
   {
      validateTargetClassType();
      TestRun.ensureThatClassIsInitialized(targetClass);
      methodsOnly = false;
      redefineMethodsAndConstructorsInTargetType();
   }

   private void applyPartialMockingToGivenInstance(@Nonnull Object instance)
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
         targetClass.isArray() || targetClass.isPrimitive() || targetClass.isSynthetic() ||
         MockingFilters.isSubclassOfUnmockable(targetClass) ||
         isWrapperOfPrimitiveType(targetClass) ||
         isGeneratedImplementationClass(targetClass)
      ) {
         throw new IllegalArgumentException("Invalid type for partial mocking: " + targetClass);
      }
   }

   @Override
   void configureClassModifier(@Nonnull MockedClassModifier modifier) { modifier.useDynamicMocking(methodsOnly); }

   @Override
   void applyClassRedefinition(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass)
   {
      modifiedClassfiles.put(realClass, modifiedClass);
   }
}
