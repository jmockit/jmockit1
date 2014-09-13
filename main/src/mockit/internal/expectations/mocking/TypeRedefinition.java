/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import org.jetbrains.annotations.*;

class TypeRedefinition extends BaseTypeRedefinition
{
   TypeRedefinition(@NotNull MockedType typeMetadata) { super(typeMetadata); }

   @Nullable
   final InstanceFactory redefineType()
   {
      //noinspection ConstantConditions
      typeMetadata.buildMockingConfiguration();

      return redefineType(typeMetadata.declaredType);
   }
}
