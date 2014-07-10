/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import org.jetbrains.annotations.*;

import mockit.*;
import mockit.internal.util.*;

final class DelegatedResult extends DynamicInvocationResult
{
   DelegatedResult(@NotNull Delegate<?> delegate)
   {
      super(delegate, MethodReflection.findNonPrivateHandlerMethod(delegate));
   }

   @Override
   @Nullable
   Object produceResult(
      @Nullable Object invokedObject, @NotNull ExpectedInvocation invocation,
      @NotNull InvocationConstraints constraints, @NotNull Object[] args)
   {
      return invokeMethodOnTargetObject(invokedObject, invocation, constraints, args);
   }
}
