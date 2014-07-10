/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;

import org.jetbrains.annotations.*;

abstract class InvocationResult
{
   InvocationResult next;

   @Nullable
   Object produceResult(@NotNull Object[] args) throws Throwable { return null; }

   @Nullable
   Object produceResult(
      @Nullable Object invokedObject, @NotNull ExpectedInvocation invocation,
      @NotNull InvocationConstraints constraints, @NotNull Object[] args)
      throws Throwable
   {
      return produceResult(args);
   }

   static final class ReturnValueResult extends InvocationResult
   {
      @Nullable private final Object returnValue;

      ReturnValueResult(@Nullable Object returnValue) { this.returnValue = returnValue; }

      @Override
      @Nullable
      Object produceResult(@NotNull Object[] args) { return returnValue; }
   }

   static final class ThrowableResult extends InvocationResult
   {
      @NotNull private final Throwable throwable;

      ThrowableResult(@NotNull Throwable throwable) { this.throwable = throwable; }

      @Override
      @NotNull
      Object produceResult(@NotNull Object[] args) throws Throwable
      {
         throwable.fillInStackTrace();
         throw throwable;
      }
   }

   static final class DeferredReturnValues extends InvocationResult
   {
      @NotNull private final Iterator<?> values;

      DeferredReturnValues(@NotNull Iterator<?> values) { this.values = values; }

      @Override
      @Nullable
      Object produceResult(@NotNull Object[] args)
      {
         return values.hasNext() ? values.next() : null;
      }
   }

   static final class DeferredResults extends InvocationResult
   {
      @NotNull private final Iterator<?> values;

      DeferredResults(@NotNull Iterator<?> values) { this.values = values; }

      @Override
      @Nullable
      Object produceResult(@NotNull Object[] args) throws Throwable
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
