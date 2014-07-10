/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import org.jetbrains.annotations.*;

public final class ObjectMethods
{
   @NotNull public static String objectIdentity(@NotNull Object obj)
   {
      return obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
   }

   @Nullable
   public static Object evaluateOverride(
      @Nullable Object obj, @NotNull String methodNameAndDesc, @NotNull Object[] args)
   {
      if (obj == null) {
         return null;
      }
      else if ("equals(Ljava/lang/Object;)Z".equals(methodNameAndDesc)) {
         return obj == args[0];
      }
      else if ("hashCode()I".equals(methodNameAndDesc)) {
         return System.identityHashCode(obj);
      }
      else if ("toString()Ljava/lang/String;".equals(methodNameAndDesc)) {
         return objectIdentity(obj);
      }
      else if (
         args.length == 1 && methodNameAndDesc.startsWith("compareTo(L") && methodNameAndDesc.endsWith(";)I") &&
         obj instanceof Comparable<?>
      ) {
         Object arg = args[0];

         if (obj == arg) {
            return 0;
         }

         return System.identityHashCode(obj) > System.identityHashCode(arg) ? 1 : -1;
      }

      return null;
   }

   public static boolean isMethodFromObject(@NotNull String name, @NotNull String desc)
   {
      return
         "equals".equals(name)   && "(Ljava/lang/Object;)Z".equals(desc) ||
         "hashCode".equals(name) && "()I".equals(desc) ||
         "toString".equals(name) && "()Ljava/lang/String;".equals(desc) ||
         "finalize".equals(name) && "()V".equals(desc);
   }
}
