/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.net.*;
import java.security.*;
import java.util.regex.*;
import javax.annotation.*;
import static java.util.regex.Pattern.*;

import mockit.coverage.*;
import mockit.coverage.standalone.*;
import mockit.internal.util.*;

final class ClassSelection
{
   private static final String THIS_CLASS_NAME = ClassSelection.class.getName();
   private static final ClassLoader THIS_CLASS_LOADER = ClassSelection.class.getClassLoader();
   private static final Pattern CSV = compile(",");
   private static final Pattern DOT = compile("\\.");
   private static final Pattern STAR = compile("\\*");
   private static final Pattern TEST_CLASS_NAME = compile(".+Test(\\$.+)?");

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

      testCode = Startup.isTestRun() ? TEST_CLASS_NAME.matcher("") : null;
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
            regex = DOT.matcher(spec).replaceAll("\\.");
            regex = STAR.matcher(regex).replaceAll(".*");
            regex = regex.replace('?', '.');
         }

         if (regex != null) {
            finalRegexBuilder.append(sep).append(regex);
            sep = "|";
         }
      }

      String finalRegex = finalRegexBuilder.toString();
      return finalRegex.isEmpty() ? null : compile(finalRegex).matcher("");
   }

   boolean isSelected(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain)
   {
      CodeSource codeSource = protectionDomain.getCodeSource();

      if (codeSource == null || isIneligibleForSelection(className)) {
         return false;
      }

      ClassLoader loaderOfClassToBeMeasured = protectionDomain.getClassLoader();

      if (
         !canAccessJMockitFromClassToBeMeasured(loaderOfClassToBeMeasured) ||
         !isClassAllowedByIncludesAndExcludes(className)
      ) {
         return false;
      }

      URL codeSourceLocation = codeSource.getLocation();

      if (codeSourceLocation == null) {
         if (loaderOfClassToBeMeasured == THIS_CLASS_LOADER) {
            return false; // it's likely a dynamically generated class
         }

         // It's from a custom class loader, so it may exist in the classpath.
         String classFileName = className.replace('.', '/') + ".class";
         codeSourceLocation = THIS_CLASS_LOADER.getResource(classFileName);

         if (codeSourceLocation == null) {
            return false;
         }
      }

      return !isClassFromExternalLibrary(codeSourceLocation);
   }

   private static boolean isIneligibleForSelection(@Nonnull String className)
   {
      return
         className.charAt(0) == '[' ||
         className.startsWith("mockit.") ||
         className.startsWith("org.junit.") || className.startsWith("junit.") ||
         className.startsWith("org.testng.") ||
         ClassLoad.isGeneratedSubclass(className);
   }

   private boolean canAccessJMockitFromClassToBeMeasured(@Nullable ClassLoader loaderOfClassToBeMeasured)
   {
      if (loaderOfClassToBeMeasured != null) {
         try {
            Class<?> thisClass = loaderOfClassToBeMeasured.loadClass(THIS_CLASS_NAME);
            return thisClass == getClass();
         }
         catch (ClassNotFoundException ignore) {}
      }

      return false;
   }

   private boolean isClassAllowedByIncludesAndExcludes(@Nonnull String className)
   {
      if (
         classesToExclude != null && classesToExclude.reset(className).matches() ||
         testCode != null && testCode.reset(className).matches()
      ) {
         return false;
      }
      else if (classesToInclude != null) {
         return classesToInclude.reset(className).matches();
      }

      return true;
   }

   private boolean isClassFromExternalLibrary(@Nonnull URL location)
   {
      if ("jar".equals(location.getProtocol())) {
         return true;
      }

      String path = location.getPath();

      return
         path.endsWith(".jar") || path.endsWith("/.cp/") ||
         testCode != null && (path.endsWith("/test-classes/") || path.endsWith("/jmockit1.org/main/classes/"));
   }
}
