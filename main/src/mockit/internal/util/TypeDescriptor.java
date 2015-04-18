/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;

import mockit.external.asm.*;

public final class TypeDescriptor
{
   private static final Class<?>[] PRIMITIVE_TYPES = {
      void.class, boolean.class, char.class, byte.class, short.class, int.class, float.class, long.class, double.class
   };

   private TypeDescriptor() {}

   @Nonnull
   public static Class<?>[] getParameterTypes(@Nonnull String methodDesc)
   {
      Type[] paramTypes = Type.getArgumentTypes(methodDesc);

      if (paramTypes.length == 0) {
         return ParameterReflection.NO_PARAMETERS;
      }

      Class<?>[] paramClasses = new Class<?>[paramTypes.length];

      for (int i = 0; i < paramTypes.length; i++) {
         paramClasses[i] = getClassForType(paramTypes[i]);
      }

      return paramClasses;
   }

   @Nonnull
   public static Class<?> getReturnType(@Nonnull String methodDesc)
   {
      Type returnType = Type.getReturnType(methodDesc);
      return getClassForType(returnType);
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

         int p = className.indexOf('<');

         if (p > 0) {
            className = className.substring(0, p);
         }
      }

      return ClassLoad.loadClass(className);
   }
}
