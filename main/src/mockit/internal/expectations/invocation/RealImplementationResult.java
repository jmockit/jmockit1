/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.reflection.*;
import mockit.internal.state.*;

final class RealImplementationResult extends DynamicInvocationResult
{
   RealImplementationResult(@Nonnull ExpectedInvocation invocation, @Nonnull Object instanceToInvoke)
      throws NoSuchMethodException
   {
      super(
         invocation, instanceToInvoke,
         new RealMethodOrConstructor(instanceToInvoke.getClass(), invocation.getMethodNameAndDescription())
            .<Method>getMember());
   }

   @Nullable @Override
   Object produceResult(@Nonnull Object[] args)
   {
      TestRun.getExecutingTest().markAsProceedingIntoRealImplementation();
      return executeMethodToInvoke(args);
   }
}
