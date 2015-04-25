/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.external.asm.*;

final class MockingConfiguration
{
   @Nonnull private final List<RegexMockFilter> filtersToApply;

   MockingConfiguration(@Nonnull String[] filters)
   {
      filtersToApply = parseMockFilters(filters);
   }

   @Nonnull
   private static List<RegexMockFilter> parseMockFilters(@Nonnull String[] mockFilters)
   {
      List<RegexMockFilter> filters = new ArrayList<RegexMockFilter>(mockFilters.length);

      for (String mockFilter : mockFilters) {
         filters.add(new RegexMockFilter(mockFilter));
      }

      return filters;
   }

   boolean matchesFilters(@Nonnull String name, @Nonnull String desc)
   {
      for (RegexMockFilter filter : filtersToApply) {
         if (filter.matches(name, desc)) {
            return true;
         }
      }

      return false;
   }

   private static final class RegexMockFilter
   {
      private static final Pattern CONSTRUCTOR_NAME_REGEX = Pattern.compile("<init>");
      private static final Pattern COMMA_SEPARATOR_REGEX = Pattern.compile(",");
      private static final String[] ANY_PARAMS = {};

      @Nonnull private final Pattern nameRegex;
      @Nullable private final String[] paramTypeNames;

      private RegexMockFilter(@Nonnull String filter)
      {
         int lp = filter.indexOf('(');
         int rp = filter.indexOf(')');

         if (lp < 0 && rp >= 0 || lp >= 0 && lp >= rp) {
            throw new IllegalArgumentException("Invalid filter: " + filter);
         }

         if (lp == 0) {
            nameRegex = CONSTRUCTOR_NAME_REGEX;
         }
         else {
            nameRegex = Pattern.compile(lp < 0 ? filter : filter.substring(0, lp));
         }

         paramTypeNames = parseParameterTypes(filter, lp, rp);
      }

      @Nullable
      private static String[] parseParameterTypes(@Nonnull String filter, int lp, int rp)
      {
         if (lp < 0) {
            return ANY_PARAMS;
         }
         else if (lp == rp - 1) {
            return null;
         }

         String[] typeNames = COMMA_SEPARATOR_REGEX.split(filter.substring(lp + 1, rp));

         for (int i = 0; i < typeNames.length; i++) {
            typeNames[i] = typeNames[i].trim();
         }

         return typeNames;
      }

      boolean matches(@Nonnull String name, @Nonnull String desc)
      {
         if (!nameRegex.matcher(name).matches()) {
            return false;
         }

         if (paramTypeNames == ANY_PARAMS) {
            return true;
         }
         else if (paramTypeNames == null) {
            return desc.charAt(1) == ')';
         }

         Type[] argTypes = Type.getArgumentTypes(desc);

         if (argTypes.length != paramTypeNames.length) {
            return false;
         }

         for (int i = 0; i < paramTypeNames.length; i++) {
            Type argType = argTypes[i];
            String paramTypeName = argType.getClassName();
            assert paramTypeName != null;

            if (!paramTypeName.endsWith(paramTypeNames[i])) {
               return false;
            }
         }

         return true;
      }
   }
}
