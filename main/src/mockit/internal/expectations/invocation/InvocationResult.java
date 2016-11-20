/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;
import javax.annotation.*;

abstract class InvocationResult
{
   InvocationResult next;

   @Nullable
   Object produceResult(@Nonnull Object[] args) throws Throwable { return null; }

   @Nullable
   Object produceResult(
      @Nullable Object invokedObject, @Nonnull ExpectedInvocation invocation,
      @Nonnull InvocationConstraints constraints, @Nonnull Object[] args)
      throws Throwable
   {
      return produceResult(args);
   }

   static final class ReturnValueResult extends InvocationResult
   {
      @Nullable private final Object returnValue;

      ReturnValueResult(@Nullable Object returnValue) { this.returnValue = returnValue; }

      @Nullable @Override
      Object produceResult(@Nonnull Object[] args) { return returnValue; }
   }

   static final class ThrowableResult extends InvocationResult
   {
      @Nonnull private final Throwable throwable;

      ThrowableResult(@Nonnull Throwable throwable) { this.throwable = throwable; }

      @Nonnull @Override
      Object produceResult(@Nonnull Object[] args) throws Throwable
      {
         throwable.fillInStackTrace();
         throw throwable;
      }
   }

   static final class DeferredResults extends InvocationResult
   {
      @Nonnull private final Iterator<?> values;

      DeferredResults(@Nonnull Iterator<?> values) { this.values = values; }

      @Nullable @Override
      Object produceResult(@Nonnull Object[] args) throws Throwable
      {
         Object nextValue = values.hasNext() ? values.next() : null;

         if (nextValue instanceof Throwable) {
            Throwable t = (Throwable) nextValue;
            t.fillInStackTrace();
            throw t;
         }

         return nextValue;
      }
   }
}
