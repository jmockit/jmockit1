/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;

import mockit.internal.*;
import mockit.internal.state.*;

import org.jetbrains.annotations.*;

final class DelegateInvocation extends BaseInvocation
{
   @NotNull private final InvocationArguments invocationArguments;

   DelegateInvocation(
      @Nullable Object invokedInstance, @NotNull Object[] invokedArguments,
      @NotNull ExpectedInvocation expectedInvocation, @NotNull InvocationConstraints constraints)
   {
      super(
         invokedInstance, invokedArguments,
         constraints.invocationCount, constraints.minInvocations, constraints.maxInvocations);
      invocationArguments = expectedInvocation.arguments;
   }

   @NotNull @Override
   protected Member findRealMember()
   {
      return invocationArguments.getRealMethodOrConstructor();
   }

   @Override
   public boolean prepareToProceed()
   {
      ExecutingTest executingTest = TestRun.getExecutingTest();

      if (getInvokedMember() instanceof Constructor) {
         executingTest.markAsProceedingIntoRealImplementation();
      }
      else {
         executingTest.markAsProceedingIntoRealImplementation(this);
      }

      return true;
   }

   @Override
   public void cleanUpAfterProceed()
   {
      TestRun.getExecutingTest().clearProceedingState();
   }
}
