/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.net.*;
import java.security.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.standalone.*;
import mockit.internal.util.*;

final class ClassSelection
{
   private static final String THIS_CLASS_NAME = ClassSelection.class.getName();
   private static final Pattern CSV = Pattern.compile(",");

   final boolean loadedOnly;
   @Nullable private final Matcher classesToInclude;
   @Nullable private final Matcher classesToExclude;
   @Nullable private final Matcher testCode;

   ClassSelection()
   {
      String classes = Configuration.getProperty("classes", "");
      loadedOnly = "loaded".equals(classes);
      classesToInclude = loadedOnly ? null : newMatcherForClassSelection(classes);

      String excludes = Configuration.getProperty("excludes", "");
      classesToExclude = newMatcherForClassSelection(excludes);

      testCode = Startup.isTestRun() ? Pattern.compile(".+Test(\\$.+)?").matcher("") : null;
   }

   @Nullable
   private static Matcher newMatcherForClassSelection(@Nonnull String specification)
   {
      if (specification.isEmpty()) {
         return null;
      }

      String[] specs = CSV.split(specification);
      StringBuilder finalRegexBuilder = new StringBuilder();
      String sep = "";

      for (String spec : specs) {
         String regex = null;

         if (spec.indexOf('\\') >= 0) {
            regex = spec;
         }
         else if (!spec.isEmpty()) {
            regex = spec.replace(".", "\\.").replace("*", ".*").replace('?', '.');
         }

         if (regex != null) {
            finalRegexBuilder.append(sep).append(regex);
            sep = "|";
         }
      }

      String finalRegex = finalRegexBuilder.toString();
      return finalRegex.isEmpty() ? null : Pattern.compile(finalRegex).matcher("");
   }

   boolean isSelected(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain)
   {
      CodeSource codeSource = protectionDomain.getCodeSource();

      if (
         codeSource == null || className.charAt(0) == '[' || className.startsWith("mockit.") ||
         className.startsWith("org.junit.") || className.startsWith("junit.") || className.startsWith("org.testng.") ||
         ClassLoad.isGeneratedSubclass(className)
      ) {
         return false;
      }

      if (!canAccessJMockitFromClassToBeMeasured(protectionDomain.getClassLoader())) {
         return false;
      }

      if (
         classesToExclude != null && classesToExclude.reset(className).matches() ||
         testCode != null && testCode.reset(className).matches()
      ) {
         return false;
      }
      else if (classesToInclude != null) {
         return classesToInclude.reset(className).matches();
      }

      URL codeSourceLocation = codeSource.getLocation();
      boolean selected = codeSourceLocation != null && !isClassFromExternalLibrary(codeSourceLocation.getPath());
      return selected;
   }

   private boolean canAccessJMockitFromClassToBeMeasured(@Nonnull ClassLoader loaderOfClassToBeMeasured)
   {
      try {
         Class<?> thisClass = loaderOfClassToBeMeasured.loadClass(THIS_CLASS_NAME);
         return thisClass == getClass();
      }
      catch (ClassNotFoundException ignore) {
         return false;
      }
   }

   private boolean isClassFromExternalLibrary(@Nonnull String location)
   {
      return
         location.endsWith(".jar") || location.endsWith("/.cp/") ||
         testCode != null && (location.endsWith("/test-classes/") || location.endsWith("/jmockit1.org/main/classes/"));
   }
}
