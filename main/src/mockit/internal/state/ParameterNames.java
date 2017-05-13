/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;
import javax.annotation.*;

import static java.lang.reflect.Modifier.isStatic;

import mockit.internal.util.*;

public final class ParameterNames
{
   private static final Map<String, Map<String, String[]>> classesToMethodsToParameters =
      new HashMap<String, Map<String, String[]>>();
   private static final String[] NO_PARAMETERS = new String[0];

   private ParameterNames() {}

   public static boolean hasNamesForClass(@Nonnull String classDesc)
   {
      return classesToMethodsToParameters.containsKey(classDesc);
   }

   public static void registerName(
      @Nonnull String classDesc, @Nonnegative int memberAccess, @Nonnull String memberName, @Nonnull String memberDesc,
      @Nonnull String desc, @Nonnull String name, @Nonnegative int index)
   {
      if ("this".equals(name)) {
         return;
      }

      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         methodsToParameters = new HashMap<String, String[]>();
         classesToMethodsToParameters.put(classDesc, methodsToParameters);
      }

      String methodKey = memberName + memberDesc;
      String[] parameterNames = methodsToParameters.get(methodKey);

      if (parameterNames == null) {
         int sumOfArgumentSizes = getSumOfArgumentSizes(memberDesc);
         parameterNames = sumOfArgumentSizes == 0 ? NO_PARAMETERS : new String[sumOfArgumentSizes];
         methodsToParameters.put(methodKey, parameterNames);
      }

      int i = index - (isStatic(memberAccess) ? 0 : 1);

      if (i < parameterNames.length) {
         parameterNames[i] = name;

         if (isDoubleSizeType(desc.charAt(0))) {
            parameterNames[i + 1] = "";
         }
      }
   }

   @SuppressWarnings("MethodWithMultipleLoops")
   private static int getSumOfArgumentSizes(@Nonnull String memberDesc)
   {
      int sum = 0;
      int i = 1;

      while (true) {
         char c = memberDesc.charAt(i);
         i++;

         if (c == ')') {
            return sum;
         }

         if (c == 'L') {
            while (memberDesc.charAt(i) != ';') i++;
            i++;
            sum++;
         }
         else if (c == '[') {
            while ((c = memberDesc.charAt(i)) == '[') i++;

            if (isDoubleSizeType(c)) { // if the array element type is double size...
               i++;
               sum++; // ...then count it here, otherwise let the outer loop count it
            }
         }
         else if (isDoubleSizeType(c)) {
            sum += 2;
         }
         else {
            sum++;
         }
      }
   }

   private static boolean isDoubleSizeType(char typeCode) { return typeCode == 'D' || typeCode == 'J'; }

   @Nonnull
   public static String getName(@Nonnull TestMethod method, @Nonnegative int index)
   {
      String name = getName(method.testClassDesc, method.testMethodDesc, index);
      return name == null ? "param" + index : name;
   }

   @Nullable
   public static String getName(@Nonnull String classDesc, @Nonnull String methodDesc, @Nonnegative int index)
   {
      Map<String, String[]> methodsToParameters = classesToMethodsToParameters.get(classDesc);

      if (methodsToParameters == null) {
         return null;
      }

      String[] parameterNames = methodsToParameters.get(methodDesc);
      int n = parameterNames.length;
      int i = 0;
      int j = 0;

      while (true) {
         String name = parameterNames[i];

         if (j == index || name == null) {
            return name;
         }

         i++;

         if (i == n) {
            break;
         }

         String nextName = parameterNames[i];

         if (nextName.isEmpty()) {
            i++;
         }

         j++;
      }

      return null;
   }
}
