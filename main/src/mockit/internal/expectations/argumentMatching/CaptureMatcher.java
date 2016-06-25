/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.util.*;
import javax.annotation.*;

public final class CaptureMatcher<T> implements ArgumentMatcher<CaptureMatcher<T>>
{
   @Nonnull private final List<T> valueHolder;
   @Nullable private Class<?> expectedType;

   public CaptureMatcher(@Nonnull List<T> valueHolder) { this.valueHolder = valueHolder; }

   public void setExpectedType(@Nonnull Class<?> expectedType) { this.expectedType = expectedType; }

   @Override
   public boolean same(@Nonnull CaptureMatcher<T> other) { return false; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      if (expectedType == null || expectedType.isInstance(argValue)) {
         //noinspection unchecked
         valueHolder.add((T) argValue);
      }

      return true;
   }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) {}
}
