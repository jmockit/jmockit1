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

   private TypeDescriptor() {}

   @Nonnull
   public static Class<?>[] getParameterTypes(@Nonnull String methodDesc)
   {
      JavaType[] paramTypes = JavaType.getArgumentTypes(methodDesc);

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
      JavaType returnType = JavaType.getReturnType(methodDesc);
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
   public static Class<?> getClassForType(@Nonnull JavaType type)
   {
      if (type instanceof PrimitiveType) {
         return ((PrimitiveType) type).getType();
      }

      String className;

      if (type instanceof ArrayType) {
         className = type.getDescriptor().replace('/', '.');
      }
      else {
         className = type.getClassName();
         assert className != null;
      }

      return ClassLoad.loadClass(className);
   }
}
