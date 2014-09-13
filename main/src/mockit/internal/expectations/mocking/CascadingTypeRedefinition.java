/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

public final class CascadingTypeRedefinition extends BaseTypeRedefinition
{
   @NotNull private final Type mockedType;

   public CascadingTypeRedefinition(@NotNull Type mockedType)
   {
      super(new MockedType(mockedType));
      this.mockedType = mockedType;
   }

   @NotNull
   public InstanceFactory redefineType()
   {
      //noinspection ConstantConditions
      return redefineType(mockedType);
   }
}