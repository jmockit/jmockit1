/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

import mockit.external.asm.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public final class CascadingTypes
{
   @NotNull private final Map<String, MockedTypeCascade> cascadingTypes;

   CascadingTypes() { cascadingTypes = new ConcurrentHashMap<String, MockedTypeCascade>(4); }

   public void add(boolean fromMockField, @NotNull java.lang.reflect.Type mockedType, @Nullable Object cascadedInstance)
   {
      Class<?> mockedClass = getClassType(mockedType);
      String mockedTypeDesc = Type.getInternalName(mockedClass);
      add(mockedTypeDesc, fromMockField, mockedType, cascadedInstance);
   }

   void add(
      @NotNull String mockedTypeDesc, boolean fromMockField, @NotNull java.lang.reflect.Type mockedType,
      @Nullable Object cascadedInstance)
   {
      if (!cascadingTypes.containsKey(mockedTypeDesc)) {
         cascadingTypes.put(mockedTypeDesc, new MockedTypeCascade(fromMockField, mockedType, cascadedInstance));
      }
   }

   @Nullable
   public MockedTypeCascade getCascade(@NotNull String mockedTypeDesc, @Nullable Object mockInstance)
   {
      if (cascadingTypes.isEmpty()) {
         return null;
      }

      MockedTypeCascade cascade = getCascade(mockedTypeDesc);

      if (cascade != null || mockInstance == null) {
         return cascade;
      }

      return getCascade(mockedTypeDesc, mockInstance.getClass());
   }

   @Nullable
   private MockedTypeCascade getCascade(@NotNull String mockedTypeDesc)
   {
      MockedTypeCascade cascade = cascadingTypes.get(mockedTypeDesc);
      if (cascade != null) return cascade;

      for (Entry<String, MockedTypeCascade> cascadeEntry : cascadingTypes.entrySet()) {
          String cascadingTypeDesc = cascadeEntry.getKey();
          int p = cascadingTypeDesc.indexOf('<');

          if (p > 0 && cascadingTypeDesc.regionMatches(0, mockedTypeDesc, 0, p)) {
            return cascadeEntry.getValue();
         }
      }

      return null;
   }

   @Nullable
   private MockedTypeCascade getCascade(@NotNull String invokedTypeDesc, @NotNull Class<?> mockedClass)
   {
      Class<?> typeToLookFor = mockedClass;

      do {
         String typeDesc = Type.getInternalName(typeToLookFor);

         if (invokedTypeDesc.equals(typeDesc)) {
            return null;
         }

         MockedTypeCascade cascade = cascadingTypes.get(typeDesc);

         if (cascade != null) {
            cascade.mockedClass = mockedClass;
            return cascade;
         }

         typeToLookFor = typeToLookFor.getSuperclass();
      }
      while (typeToLookFor != Object.class);

      return null;
   }

   void clearNonSharedCascadingTypes()
   {
      if (!cascadingTypes.isEmpty()) {
         Iterator<MockedTypeCascade> itr = cascadingTypes.values().iterator();

         while (itr.hasNext()) {
            MockedTypeCascade cascade = itr.next();

            if (cascade.fromMockField) {
               cascade.discardCascadedMocks();
            }
            else {
               itr.remove();
            }
         }
      }
   }

   public void clear() { cascadingTypes.clear(); }
}
