/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.util.*;
import java.util.regex.Pattern;
import javax.annotation.*;

final class StartupConfiguration
{
   private static final Pattern COMMA_OR_SPACES = Pattern.compile("\\s*,\\s*|\\s+");

   @Nonnull final Collection<String> fakeClasses;

   StartupConfiguration()
   {
      String commaOrSpaceSeparatedValues = System.getProperty("fakes");
      
      if (commaOrSpaceSeparatedValues == null) {
         fakeClasses = Collections.emptyList();
      }
      else {
         List<String> allValues = Arrays.asList(COMMA_OR_SPACES.split(commaOrSpaceSeparatedValues));
         Set<String> uniqueValues = new HashSet<String>(allValues);
         uniqueValues.remove("");
         fakeClasses = uniqueValues;
      }
   }
}
