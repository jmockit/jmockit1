/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import mockit.internal.util.*;

public final class ReflectiveMatcher implements ArgumentMatcher
{
   @NotNull private final Object handlerObject;
   @Nullable private Method handlerMethod;
   @Nullable private Object matchedValue;

   public ReflectiveMatcher(@NotNull Object handlerObject) { this.handlerObject = handlerObject; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      if (handlerMethod == null) {
         handlerMethod = MethodReflection.findNonPrivateHandlerMethod(handlerObject);
      }

      matchedValue = argValue;
      Boolean result = MethodReflection.invoke(handlerObject, handlerMethod, argValue);

      return result == null || result;
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
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
