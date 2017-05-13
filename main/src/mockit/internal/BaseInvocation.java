/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.reflect.*;

import javax.annotation.*;

import mockit.*;
import mockit.internal.reflection.*;

/**
 * Base class for encapsulating state and logic needed by both the Expectations and Mockups APIs, but which should not
 * go into {@link Invocation} in order to keep the published API clean.
 */
public abstract class BaseInvocation extends Invocation
{
   @Nullable protected Member realMember;
   @Nullable protected BaseInvocation previousInvocation;

   protected BaseInvocation(@Nullable Object invokedInstance, @Nonnull Object[] invokedArguments, int invocationCount)
   {
      super(invokedInstance, invokedArguments, invocationCount);
   }

   @Nonnull
   public final Member getRealMember()
   {
      if (realMember == null) {
         realMember = findRealMember();
      }

      return realMember;
   }

   @Nonnull protected abstract Member findRealMember();

   @Nullable
   public final <T> T doProceed(@Nullable Object[] replacementArguments)
   {
      Member memberToInvoke = getRealMember();

      if (memberToInvoke instanceof Constructor) {
         prepareToProceed();
         return null;
      }

      prepareToProceed();

      Method realMethod = (Method) memberToInvoke;
      Object[] actualArgs = getInvokedArguments();

      if (replacementArguments != null && replacementArguments.length > 0) {
         actualArgs = realMethod.isVarArgs() ?
            createArgumentsArrayWithVarargs(actualArgs.length, replacementArguments) : replacementArguments;
      }

      try {
         return MethodReflection.invoke(getInvokedInstance(), realMethod, actualArgs);
      }
      finally {
         cleanUpAfterProceed();
      }
   }

   @Nonnull
   private static Object[] createArgumentsArrayWithVarargs(int numInvokedArgs, @Nonnull Object[] replacementArguments)
   {
      int m = numInvokedArgs - 1;
      Object[] actualArgs = new Object[numInvokedArgs];
      System.arraycopy(replacementArguments, 0, actualArgs, 0, m);

      Object[] replacementVarargs = new Object[replacementArguments.length - m];
      System.arraycopy(replacementArguments, m, replacementVarargs, 0, replacementVarargs.length);
      actualArgs[m] = replacementVarargs;

      return actualArgs;
   }

   public abstract void prepareToProceed();
   protected abstract void cleanUpAfterProceed();

   @Nullable public final BaseInvocation getPrevious() { return previousInvocation; }
   public final void setPrevious(@Nonnull BaseInvocation previous) { previousInvocation = previous; }

   public final boolean isMethodInSuperclass(@Nullable Object mock, @Nonnull String classDesc)
   {
      if (mock != null && mock == getInvokedInstance() && getInvokedMember() instanceof Method) {
         Method methodToInvoke = getInvokedMember();
         String invokedClassDesc = methodToInvoke.getDeclaringClass().getName().replace('.', '/');
         return !invokedClassDesc.equals(classDesc);
      }

      return previousInvocation != null && previousInvocation.isMethodInSuperclass(mock, classDesc);
   }
}
