/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import mockit.internal.state.*;
import mockit.internal.util.*;

final class RealImplementationResult extends DynamicInvocationResult
{
   RealImplementationResult(@NotNull Object instanceToInvoke, @NotNull String methodToInvoke)
      throws NoSuchMethodException
   {
      super(
         instanceToInvoke,
         new RealMethodOrConstructor(instanceToInvoke.getClass(), methodToInvoke).<Method>getMember());
   }

   @Nullable @Override
   Object produceResult(@NotNull Object[] args)
   {
      TestRun.getExecutingTest().markAsProceedingIntoRealImplementation();
      return executeMethodToInvoke(args);
   }
}
