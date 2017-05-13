/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;

import mockit.external.asm.*;

public final class TypeDescriptor
{
   private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];
   private static final Class<?>[] PRIMITIVE_TYPES = {
      void.class, boolean.class, char.class, byte.class, short.class, int.class, float.class, long.class, double.class
   };

   private TypeDescriptor() {}

   @Nonnull
   public static Class<?>[] getParameterTypes(@Nonnull String methodDesc)
   {
      Type[] paramTypes = Type.getArgumentTypes(methodDesc);

      if (paramTypes.length == 0) {
         return NO_PARAMETERS;
      }

      Class<?>[] paramClasses = new Class<?>[paramTypes.length];

      for (int i = 0; i < paramTypes.length; i++) {
         paramClasses[i] = getClassForType(paramTypes[i]);
      }

      return paramClasses;
   }

   @Nonnull
   public static Class<?> getReturnType(@Nonnull String methodSignature)
   {
      String methodDesc = methodDescriptionWithoutTypeArguments(methodSignature);
      Type returnType = Type.getReturnType(methodDesc);
      return getClassForType(returnType);
   }

   @Nonnull
   private static String methodDescriptionWithoutTypeArguments(@Nonnull String methodSignature)
   {
      while (true) {
         int p = methodSignature.indexOf('<');

         if (p < 0) {
            return methodSignature;
         }

         String firstPart = methodSignature.substring(0, p);
         int q = methodSignature.indexOf('>', p) + 1;

         if (methodSignature.charAt(q) == '.') { // in case there is an inner class
            methodSignature = firstPart + '$' + methodSignature.substring(q + 1);
         }
         else {
            methodSignature = firstPart + methodSignature.substring(q);
         }
      }
   }

   @Nonnull
   public static Class<?> getClassForType(@Nonnull Type type)
   {
      int sort = type.getSort();

      if (sort < PRIMITIVE_TYPES.length) {
         return PRIMITIVE_TYPES[sort];
      }

      String className;

      if (sort == Type.ARRAY) {
         className = type.getDescriptor().replace('/', '.');
      }
      else {
         className = type.getClassName();
         assert className != null;
      }

      return ClassLoad.loadClass(className);
   }
}
