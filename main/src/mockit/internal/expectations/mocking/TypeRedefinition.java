/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import static mockit.internal.util.GeneratedClasses.*;

import org.jetbrains.annotations.*;

class TypeRedefinition extends BaseTypeRedefinition
{
   TypeRedefinition(@NotNull MockedType typeMetadata) { super(typeMetadata); }

   @Nullable
   final InstanceFactory redefineType()
   {
      typeMetadata.buildMockingConfiguration();

      return redefineType(typeMetadata.declaredType);
   }

   @NotNull @Override
   final String getNameForConcreteSubclassToCreate()
   {
      String mockId = typeMetadata.mockId;
      return mockId == null ? getNameForGeneratedClass(targetClass) : getNameForGeneratedClass(targetClass, mockId);
   }
}
