/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.reflect.*;

import mockit.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

/**
 * Base class for encapsulating state and logic needed by both the Expectations and Mockups APIs, but which should not
 * go into {@link Invocation} in order to keep the published API clean.
 */
public abstract class BaseInvocation extends Invocation
{
   @Nullable protected Member realMember;
   @Nullable protected BaseInvocation previousInvocation;

   protected BaseInvocation(
      @Nullable Object invokedInstance, @NotNull Object[] invokedArguments,
      int invocationCount, int minInvocations, int maxInvocations)
   {
      super(invokedInstance, invokedArguments, invocationCount, minInvocations, maxInvocations);
   }

   @NotNull public final Member getRealMember()
   {
      if (realMember == null) {
         realMember = findRealMember();
      }

      return realMember;
   }

   @NotNull protected abstract Member findRealMember();

   @Nullable
   public final <T> T doProceed(@Nullable Object[] replacementArguments)
   {
      boolean withArgs = replacementArguments != null && replacementArguments.length > 0;
      Member memberToInvoke = getRealMember();

      if (memberToInvoke instanceof Constructor) {
         if (withArgs) {
            throw new UnsupportedOperationException("Cannot replace arguments when proceeding into constructor");
         }

         prepareToProceed();

         return null;
      }

      if (!prepareToProceed()) {
         return null;
      }

      Method realMethod = (Method) memberToInvoke;
      Object[] actualArgs = getInvokedArguments();

      if (withArgs) {
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

   @NotNull
   private Object[] createArgumentsArrayWithVarargs(int numInvokedArgs, @NotNull Object[] replacementArguments)
   {
      int m = numInvokedArgs - 1;
      Object[] actualArgs = new Object[numInvokedArgs];
      System.arraycopy(replacementArguments, 0, actualArgs, 0, m);

      Object[] replacementVarargs = new Object[replacementArguments.length - m];
      System.arraycopy(replacementArguments, m, replacementVarargs, 0, replacementVarargs.length);
      actualArgs[m] = replacementVarargs;

      return actualArgs;
   }

   public abstract boolean prepareToProceed();
   protected abstract void cleanUpAfterProceed();

   @Nullable public final BaseInvocation getPreviousInvocation() { return previousInvocation; }

   public final void setPreviousInvocation(@Nullable BaseInvocation previousInvocation)
   {
      this.previousInvocation = previousInvocation;
   }

   public final boolean isMethodInSuperclass(@Nullable Object mock, @NotNull String classDesc)
   {
      if (mock != null && mock == getInvokedInstance() && getInvokedMember() instanceof Method) {
         Method methodToInvoke = getInvokedMember();
         String invokedClassDesc = methodToInvoke.getDeclaringClass().getName().replace('.', '/');
         return !invokedClassDesc.equals(classDesc);
      }

      return previousInvocation != null && previousInvocation.isMethodInSuperclass(mock, classDesc);
   }
}
