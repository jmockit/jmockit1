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
   RealImplementationResult(@Nonnull Object instanceToInvoke, @Nonnull String methodToInvoke)
      throws NoSuchMethodException
   {
      super(
         instanceToInvoke,
         new RealMethodOrConstructor(instanceToInvoke.getClass(), methodToInvoke).<Method>getMember());
   }

   @Nullable @Override
   Object produceResult(@Nonnull Object[] args)
   {
      TestRun.getExecutingTest().markAsProceedingIntoRealImplementation();
      return executeMethodToInvoke(args);
   }
}
