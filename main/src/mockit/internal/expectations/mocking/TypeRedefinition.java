/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.*;

class TypeRedefinition extends BaseTypeRedefinition
{
   TypeRedefinition(@Nonnull MockedType typeMetadata) { super(typeMetadata); }

   @Nullable
   final InstanceFactory redefineType()
   {
      //noinspection ConstantConditions
      Class<?> classToMock = typeMetadata.getClassType();

      if (MockingFilters.isSubclassOfUnmockable(classToMock)) {
         String mockSource = typeMetadata.field == null ? "mock parameter" : "mock field";
         throw new IllegalArgumentException(
            classToMock + " is not mockable (" + mockSource + " \"" + typeMetadata.getName() + "\")");
      }

      Type declaredType = typeMetadata.getDeclaredType();
      return redefineType(declaredType);
   }
}
