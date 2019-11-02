/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.reflect.*;

import javax.annotation.*;

import mockit.*;
import mockit.internal.reflection.*;

/**
 * Base class for encapsulating state and logic needed by both the Mocking and Faking APIs, but which should not go into
 * {@link Invocation} in order to keep the published API clean.
 */
public abstract class BaseInvocation extends Invocation
{
   @Nullable private Member realMember;
   @Nullable private BaseInvocation previousInvocation;

   protected BaseInvocation(@Nullable Object invokedInstance, @Nonnull Object[] invokedArguments, @Nonnegative int invocationCount) {
      super(invokedInstance, invokedArguments, invocationCount);
   }

   @Nonnull
   public final Member getRealMember() {
      if (realMember == null) {
         realMember = findRealMember();
      }

      return realMember;
   }

   @Nonnull protected abstract Member findRealMember();

   @Nullable
   public final <T> T doProceed(@Nullable Object[] replacementArguments) {
      Member memberToInvoke = getRealMember();

      if (memberToInvoke instanceof Constructor) {
         prepareToProceed();
         return null;
      }

      prepareToProceed();

      Method realMethod = (Method) memberToInvoke;
      Object[] actualArgs = getInvokedArguments();

      if (replacementArguments != null && replacementArguments.length > 0) {
         actualArgs = replacementArguments;
      }

      try {
         return MethodReflection.invoke(getInvokedInstance(), realMethod, actualArgs);
      }
      finally {
         cleanUpAfterProceed();
      }
   }

   public abstract void prepareToProceed();
   protected abstract void cleanUpAfterProceed();

   @Nullable public final BaseInvocation getPrevious() { return previousInvocation; }
   public final void setPrevious(@Nonnull BaseInvocation previous) { previousInvocation = previous; }

   public final boolean isMethodInSuperclass(@Nullable Object mock, @Nonnull String classDesc) {
      if (mock != null && mock == getInvokedInstance() && getInvokedMember() instanceof Method) {
         Method methodToInvoke = getInvokedMember();
         String invokedClassDesc = methodToInvoke.getDeclaringClass().getName().replace('.', '/');
         return !invokedClassDesc.equals(classDesc);
      }

      return previousInvocation != null && previousInvocation.isMethodInSuperclass(mock, classDesc);
   }
}