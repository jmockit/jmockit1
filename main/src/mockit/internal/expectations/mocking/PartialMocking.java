/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.*;
import mockit.internal.state.*;
import static mockit.internal.util.AutoBoxing.*;
import static mockit.internal.util.GeneratedClasses.*;

public final class PartialMocking extends BaseTypeRedefinition
{
   @Nonnull public final List<Object> targetInstances;
   @Nonnull private final Map<Class<?>, byte[]> modifiedClassfiles;

   public PartialMocking() {
      targetInstances = new ArrayList<>(2);
      modifiedClassfiles = new HashMap<>();
   }

   public void redefineTypes(@Nonnull Object[] instancesToBePartiallyMocked) {
      for (Object instance : instancesToBePartiallyMocked) {
         redefineClassHierarchy(instance);
      }

      if (!modifiedClassfiles.isEmpty()) {
         TestRun.mockFixture().redefineMethods(modifiedClassfiles);
         modifiedClassfiles.clear();
      }
   }

   private void redefineClassHierarchy(@Nonnull Object mockInstance) {
      if (mockInstance instanceof Class) {
         throw new IllegalArgumentException("Invalid Class argument for partial mocking (use a MockUp instead): " + mockInstance);
      }

      targetClass = getMockedClass(mockInstance);
      applyPartialMockingToGivenInstance(mockInstance);

      InstanceFactory instanceFactory = createInstanceFactory(targetClass);
      instanceFactory.lastInstance = mockInstance;

      TestRun.mockFixture().registerInstanceFactoryForMockedType(targetClass, instanceFactory);
      TestRun.getExecutingTest().getCascadingTypes().add(false, targetClass);
   }

   private void applyPartialMockingToGivenInstance(@Nonnull Object instance) {
      validateTargetClassType();
      redefineMethodsAndConstructorsInTargetType();
      targetInstances.add(instance);
   }

   private void validateTargetClassType() {
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
   void configureClassModifier(@Nonnull MockedClassModifier modifier) { modifier.useDynamicMocking(); }

   @Override
   void applyClassRedefinition(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass) {
      modifiedClassfiles.put(realClass, modifiedClass);
   }
}