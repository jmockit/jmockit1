/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

import mockit.external.asm.*;

public final class TestMethod
{
   @Nonnull public final String testClassDesc;
   @Nonnull public final String testMethodDesc;
   @Nonnull private final Type[] parameterTypes;
   @Nonnull private final Class<?>[] parameterClasses;
   @Nonnull private final Annotation[][] parameterAnnotations;
   @Nonnull private final Object[] parameterValues;

   public TestMethod(@Nonnull Method testMethod, @Nonnull Object[] parameterValues)
   {
      testClassDesc = JavaType.getInternalName(testMethod.getDeclaringClass());
      testMethodDesc = testMethod.getName() + JavaType.getMethodDescriptor(testMethod);
      parameterTypes = testMethod.getGenericParameterTypes();
      parameterClasses = testMethod.getParameterTypes();
      parameterAnnotations = testMethod.getParameterAnnotations();
      this.parameterValues = parameterValues;
   }

   @Nonnegative public int getParameterCount() { return parameterTypes.length; }
   @Nonnull public Type getParameterType(@Nonnegative int index) { return parameterTypes[index]; }
   @Nonnull public Class<?> getParameterClass(@Nonnegative int index) { return parameterClasses[index]; }
   @Nonnull public Annotation[] getParameterAnnotations(@Nonnegative int index) { return parameterAnnotations[index]; }
   @Nullable public Object getParameterValue(@Nonnegative int index) { return parameterValues[index]; }

   public void setParameterValue(@Nonnegative int index, @Nullable Object value)
   {
      if (value != null) {
         parameterValues[index] = value;
      }
   }
}
