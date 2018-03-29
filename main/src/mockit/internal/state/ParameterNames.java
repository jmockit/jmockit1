/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;
import javax.annotation.*;

import mockit.internal.util.*;

public final class ParameterNames
{
   private static final Map<String, Map<String, String[]>> classesToMethodsToParameters = new HashMap<>();

   private ParameterNames() {}

   public static boolean hasNamesForClass(@Nonnull String classDesc) {
      return classesToMethodsToParameters.containsKey(classDesc);
   }

   public static void register(@Nonnull String classDesc, @Nonnull String memberName, @Nonnull String memberDesc, @Nonnull String[] names) {
      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         methodsToParameters = new HashMap<>();
         classesToMethodsToParameters.put(classDesc, methodsToParameters);
      }

      String methodKey = memberName + memberDesc;
      methodsToParameters.put(methodKey, names);
   }

   @Nonnull
   public static String getName(@Nonnull TestMethod method, @Nonnegative int index) {
      String name = getName(method.testClassDesc, method.testMethodDesc, index);
      return name == null ? "param" + index : name;
   }

   @Nullable
   public static String getName(@Nonnull String classDesc, @Nonnull String methodDesc, @Nonnegative int index) {
      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         return null;
      }

      String[] parameterNames = methodsToParameters.get(methodDesc);
      return parameterNames[index];
   }
}
