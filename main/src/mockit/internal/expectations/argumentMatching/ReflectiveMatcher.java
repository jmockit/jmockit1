/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.reflection.*;

public final class ReflectiveMatcher implements ArgumentMatcher<ReflectiveMatcher>
{
   @Nonnull private final Delegate<?> delegate;
   @Nullable private Method handlerMethod;
   @Nullable private Object matchedValue;

   public ReflectiveMatcher(@Nonnull Delegate<?> delegate) { this.delegate = delegate; }

   @Override
   public boolean same(@Nonnull ReflectiveMatcher other) { return delegate == other.delegate; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      if (handlerMethod == null) {
         handlerMethod = MethodReflection.findNonPrivateHandlerMethod(delegate);
      }

      matchedValue = argValue;
      Boolean result = MethodReflection.invoke(delegate, handlerMethod, argValue);

      return result == null || result;
   }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch)
   {
      if (handlerMethod != null) {
         argumentMismatch.append(handlerMethod.getName()).append('(');
         argumentMismatch.appendFormatted(matchedValue);
         argumentMismatch.append(") (should return true, was false)");
      }
      else {
         argumentMismatch.append('?');
      }

      argumentMismatch.markAsFinished();
   }
}
