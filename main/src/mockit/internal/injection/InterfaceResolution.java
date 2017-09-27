/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import static mockit.internal.reflection.MethodReflection.invoke;
import static mockit.internal.util.Utilities.getClassType;

final class InterfaceResolution
{
   @Nonnull private final NavigableMap<ParameterizedType, Method> interfaceResolutionMethods;

   InterfaceResolution()
   {
      interfaceResolutionMethods = new TreeMap<ParameterizedType, Method>(new Comparator<ParameterizedType>() {
         @Override
         public int compare(ParameterizedType t1, ParameterizedType t2)
         {
            if (t1 == t2) {
               return 0;
            }

            Type targetType1 = t1.getActualTypeArguments()[0];
            Type targetType2 = t2.getActualTypeArguments()[0];

            if (targetType1 == targetType2) {
               return 0;
            }

            if (targetType1 instanceof WildcardType) {
               if (targetType2 instanceof WildcardType) {
                  return compareTypesFromResolutionMethods((WildcardType) targetType1, (WildcardType) targetType2);
               }

               return 1;
            }

            return -1;
         }
      });
   }

   private static int compareTypesFromResolutionMethods(@Nonnull WildcardType type1, @Nonnull WildcardType type2)
   {
      Type upperBound1 = type1.getUpperBounds()[0];
      Class<?> classOfUpperBound1 = getClassType(upperBound1);

      Type upperBound2 = type2.getUpperBounds()[0];
      Class<?> classOfUpperBound2 = getClassType(upperBound2);

      if (classOfUpperBound1.isAssignableFrom(classOfUpperBound2)) {
         return 1;
      }

      if (classOfUpperBound2.isAssignableFrom(classOfUpperBound1)) {
         return -1;
      }

      return classOfUpperBound1.getName().compareTo(classOfUpperBound2.getName());
   }

   boolean canResolveInterfaces() { return !interfaceResolutionMethods.isEmpty(); }

   void addInterfaceResolutionMethod(@Nonnull ParameterizedType interfaceType, @Nonnull Method resolutionMethod)
   {
      interfaceResolutionMethods.put(interfaceType, resolutionMethod);
   }

   @Nullable
   Class<?> resolveInterface(@Nonnull Class<?> anInterface, @Nonnull Object testClassInstance)
   {
      if (interfaceResolutionMethods.isEmpty()) {
         return null;
      }

      for (Entry<ParameterizedType, Method> typeAndMethod : interfaceResolutionMethods.entrySet()) {
         ParameterizedType acceptedType = typeAndMethod.getKey();
         Method method = typeAndMethod.getValue();
         Type targetType = acceptedType.getActualTypeArguments()[0];

         if (
            targetType == anInterface ||
            targetType instanceof WildcardType && satisfiesUpperBounds(anInterface, (WildcardType) targetType)
         ) {
            Class<?> implementationClass = invoke(testClassInstance, method, anInterface);

            if (implementationClass != null) {
               return implementationClass;
            }
         }
      }

      return null;
   }

   private static boolean satisfiesUpperBounds(@Nonnull Class<?> interfaceType, @Nonnull WildcardType targetType)
   {
      for (Type upperBound : targetType.getUpperBounds()) {
         Class<?> classOfUpperBound = getClassType(upperBound);

         if (!classOfUpperBound.isAssignableFrom(interfaceType)) {
            return false;
         }
      }

      return true;
   }
}
