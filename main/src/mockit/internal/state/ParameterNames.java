/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;

import org.jetbrains.annotations.*;

public final class ParameterNames
{
   private static final Map<String, Map<String, String[]>> classesToMethodsToParameters =
      new HashMap<String, Map<String, String[]>>();
   private static final String[] NO_PARAMETERS = new String[0];

   private ParameterNames() {}

   public static boolean hasNamesForClass(@NotNull String classDesc)
   {
      return classesToMethodsToParameters.containsKey(classDesc);
   }

   public static void registerName(
      @NotNull String classDesc, int methodAccess, @NotNull String methodName, @NotNull String methodDesc,
      @NotNull String name, int index)
   {
      if ("this".equals(name)) {
         return;
      }

      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         methodsToParameters = new HashMap<String, String[]>();
         classesToMethodsToParameters.put(classDesc, methodsToParameters);
      }

      String methodKey = methodName + methodDesc;
      String[] parameterNames = methodsToParameters.get(methodKey);

      if (parameterNames == null) {
         int numParameters = Type.getArgumentTypes(methodDesc).length;
         parameterNames = numParameters == 0 ? NO_PARAMETERS : new String[numParameters];
         methodsToParameters.put(methodKey, parameterNames);
      }

      if (!isStatic(methodAccess)) {
         //noinspection AssignmentToMethodParameter
         index--;
      }

      if (index < parameterNames.length) {
         parameterNames[index] = name;
      }
   }

   @Nullable
   public static String getName(@NotNull String classDesc, @NotNull String methodDesc, int index)
   {
      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         return null;
      }

      String[] parameterNames = methodsToParameters.get(methodDesc);
      return parameterNames == null ? null : parameterNames[index];
   }
}
