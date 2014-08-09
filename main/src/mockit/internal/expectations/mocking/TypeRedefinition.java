/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;

import mockit.internal.state.*;
import static mockit.internal.util.GeneratedClasses.*;

import org.jetbrains.annotations.*;

final class TypeRedefinition extends BaseTypeRedefinition
{
   TypeRedefinition(@NotNull MockedType typeMetadata) { super(typeMetadata); }

   boolean redefineTypeForFinalField()
   {
      if (targetClass == TypeVariable.class || !typeMetadata.injectable && targetClass.isInterface()) {
         throw new IllegalArgumentException("Final mock field \"" + typeMetadata.mockId + "\" must be of a class type");
      }

      Integer mockedClassId = redefineClassesFromCache();
      boolean redefined = mockedClassId == null;

      if (mockedClassId != null) {
         typeMetadata.buildMockingConfiguration();
         redefined = redefineMethodsAndConstructorsInTargetType();

         if (redefined) {
            storeRedefinedClassesInCache(mockedClassId);
         }
      }

      if (redefined) {
         TestRun.mockFixture().registerMockedClass(targetClass);
      }

      return redefined;
   }

   @Nullable
   InstanceFactory redefineType()
   {
      typeMetadata.buildMockingConfiguration();

      return redefineType(typeMetadata.declaredType);
   }

   @NotNull @Override
   String getNameForConcreteSubclassToCreate()
   {
      String mockId = typeMetadata.mockId;
      return mockId == null ? getNameForGeneratedClass(targetClass) : getNameForGeneratedClass(targetClass, mockId);
   }
}
