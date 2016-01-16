/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import javax.annotation.*;

public final class CascadingTypeRedefinition extends BaseTypeRedefinition
{
   @Nonnull private final Type mockedType;

   public CascadingTypeRedefinition(@Nonnull String cascadingMethodName, @Nonnull Type mockedType)
   {
      super(new MockedType(cascadingMethodName, mockedType));
      this.mockedType = mockedType;
   }

   @Nullable
   public InstanceFactory redefineType()
   {
      return redefineType(mockedType);
   }
}
