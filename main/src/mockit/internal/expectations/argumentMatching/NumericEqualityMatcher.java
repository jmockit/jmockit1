/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

/**
 * Matches a decimal argument against another within a given margin of error.
 */
public final class NumericEqualityMatcher implements ArgumentMatcher
{
   private final double value;
   private final double delta;

   public NumericEqualityMatcher(double value, double delta)
   {
      this.value = value;
      this.delta = delta;
   }

   @Override
   public boolean matches(@Nullable Object decimalValue)
   {
      return decimalValue instanceof Number && actualDelta((Number) decimalValue) <= delta;
   }

   private double actualDelta(@NotNull Number decimalValue)
   {
      return Math.abs(decimalValue.doubleValue() - value);
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("a numeric value within ").append(delta).append(" of ").append(value);
   }
}
