/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.util.*;
import javax.annotation.*;

public final class AutoBoxing
{
   private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<Class<?>, Class<?>>();
   private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new HashMap<Class<?>, Class<?>>();

   static {
      WRAPPER_TO_PRIMITIVE.put(Boolean.class, boolean.class);
      WRAPPER_TO_PRIMITIVE.put(Character.class, char.class);
      WRAPPER_TO_PRIMITIVE.put(Byte.class, byte.class);
      WRAPPER_TO_PRIMITIVE.put(Short.class, short.class);
      WRAPPER_TO_PRIMITIVE.put(Integer.class, int.class);
      WRAPPER_TO_PRIMITIVE.put(Float.class, float.class);
      WRAPPER_TO_PRIMITIVE.put(Long.class, long.class);
      WRAPPER_TO_PRIMITIVE.put(Double.class, double.class);

      PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
      PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
      PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
      PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
      PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
      PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
      PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
      PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
   }

   private AutoBoxing() {}

   public static boolean isWrapperOfPrimitiveType(@Nonnull Class<?> type)
   {
      return WRAPPER_TO_PRIMITIVE.containsKey(type);
   }

   @Nullable
   public static Class<?> getPrimitiveType(@Nonnull Class<?> wrapperType)
   {
      return WRAPPER_TO_PRIMITIVE.get(wrapperType);
   }

   @Nullable
   public static Class<?> getWrapperType(@Nonnull Class<?> primitiveType)
   {
      return PRIMITIVE_TO_WRAPPER.get(primitiveType);
   }
}
