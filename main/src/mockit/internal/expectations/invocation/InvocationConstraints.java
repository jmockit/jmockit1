/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import javax.annotation.*;

import mockit.internal.*;

public final class InvocationConstraints
{
   public int minInvocations;
   int maxInvocations;
   public int invocationCount;

   public InvocationConstraints(boolean strictInvocation, boolean nonStrictInvocation)
   {
      setLimits(nonStrictInvocation ? 0 : 1, strictInvocation ? 1 : -1);
   }

   public void setLimits(int minInvocations, int maxInvocations)
   {
      this.minInvocations = minInvocations;
      this.maxInvocations = maxInvocations;
   }

   void adjustMaxInvocations(int expectedInvocationCount)
   {
      if (maxInvocations > 0 && maxInvocations < expectedInvocationCount) {
         maxInvocations = expectedInvocationCount;
      }
   }

   void setUnlimitedMaxInvocations() { maxInvocations = -1; }

   public void incrementInvocationCount() { invocationCount++; }

   public boolean isInvocationCountAtMaximumAllowed() { return invocationCount == maxInvocations; }

   public boolean isInvocationCountLessThanMinimumExpected() { return invocationCount < minInvocations; }

   public boolean isInvocationCountMoreThanMaximumExpected()
   {
      return maxInvocations >= 0 && invocationCount > maxInvocations;
   }

   public boolean isInvocationCountInExpectedRange()
   {
      return minInvocations <= invocationCount && (invocationCount <= maxInvocations || maxInvocations < 0);
   }

   @Nullable
   public Error verifyLowerLimit(@Nonnull ExpectedInvocation invocation, int lowerLimit)
   {
      if (invocationCount < lowerLimit) {
         int missingInvocations = lowerLimit - invocationCount;
         return invocation.errorForMissingInvocations(missingInvocations);
      }

      return null;
   }

   @Nullable
   public Error verifyUpperLimit(
      @Nonnull ExpectedInvocation invocation, @Nonnull Object[] replayArgs, int upperLimit,
      @Nullable CharSequence customErrorMessage)
   {
      if (upperLimit >= 0) {
         int unexpectedInvocations = invocationCount - upperLimit;

         if (unexpectedInvocations > 0) {
            invocation.customErrorMessage = customErrorMessage;
            UnexpectedInvocation error = invocation.errorForUnexpectedInvocations(replayArgs, unexpectedInvocations);
            invocation.customErrorMessage = null;
            return error;
         }
      }

      return null;
   }

   @Nonnull
   public Error errorForMissingExpectations(@Nonnull ExpectedInvocation invocation)
   {
      return invocation.errorForMissingInvocations(minInvocations - invocationCount) ;
   }
}
