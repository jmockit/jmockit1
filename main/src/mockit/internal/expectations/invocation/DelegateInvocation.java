/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.*;
import mockit.internal.expectations.state.*;
import mockit.internal.state.*;

final class DelegateInvocation extends BaseInvocation
{
   @Nonnull private final InvocationArguments invocationArguments;

   DelegateInvocation(
      @Nullable Object invokedInstance, @Nonnull Object[] invokedArguments,
      @Nonnull ExpectedInvocation expectedInvocation, @Nonnull InvocationConstraints constraints)
   {
      super(invokedInstance, invokedArguments, constraints.invocationCount);
      invocationArguments = expectedInvocation.arguments;
   }

   @Nonnull @Override
   protected Member findRealMember()
   {
      return invocationArguments.getRealMethodOrConstructor();
   }

   @Override
   public void prepareToProceed()
   {
      ExecutingTest executingTest = TestRun.getExecutingTest();

      if (getInvokedMember() instanceof Constructor) {
         executingTest.markAsProceedingIntoRealImplementation();
      }
      else {
         executingTest.markAsProceedingIntoRealImplementation(this);
      }
   }

   @Override
   public void cleanUpAfterProceed()
   {
      TestRun.getExecutingTest().clearProceedingState();
   }
}
