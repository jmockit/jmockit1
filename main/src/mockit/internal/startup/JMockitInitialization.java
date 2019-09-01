/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.util.*;
import javax.annotation.*;

import static java.util.Arrays.asList;

import mockit.*;
import mockit.coverage.*;
import mockit.integration.junit4.*;
import mockit.internal.reflection.*;
import mockit.internal.util.*;
import static mockit.internal.util.ClassLoad.*;

final class JMockitInitialization
{
   private JMockitInitialization() {}

   static void initialize(@Nonnull Instrumentation inst, boolean activateCoverage) {
      if (activateCoverage || CodeCoverage.active()) {
         inst.addTransformer(new CodeCoverage());
      }

      applyInternalStartupFakesAsNeeded();
      applyUserSpecifiedStartupFakesIfAny();
   }

   private static void applyInternalStartupFakesAsNeeded() {
      if (searchTypeInClasspath("org.junit.runners.model.FrameworkMethod", true) != null) {
         new FakeRunNotifier();
         new FakeFrameworkMethod();
      }

      if (searchTypeInClasspath("org.junit.jupiter.api.extension.Extension", true) != null) {
         System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");
      }
   }

   private static void applyUserSpecifiedStartupFakesIfAny() {
      Collection<String> fakeClasses = getFakeClasses();

      for (String fakeClassName : fakeClasses) {
         applyStartupFake(fakeClassName);
      }
   }

   @Nonnull
   private static Collection<String> getFakeClasses() {
      String commaOrSpaceSeparatedValues = System.getProperty("fakes");

      if (commaOrSpaceSeparatedValues == null) {
         return Collections.emptyList();
      }

      //noinspection DynamicRegexReplaceableByCompiledPattern
      String[] fakeClassNames = commaOrSpaceSeparatedValues.split("\\s*,\\s*|\\s+");
      Set<String> uniqueClassNames = new HashSet<>(asList(fakeClassNames));
      uniqueClassNames.remove("");
      return uniqueClassNames;
   }

   private static void applyStartupFake(@Nonnull String fakeClassName) {
      String argument = null;
      int p = fakeClassName.indexOf('=');

      if (p > 0) {
         argument = fakeClassName.substring(p + 1);
         fakeClassName = fakeClassName.substring(0, p);
      }

      try {
         Class<?> fakeClass = loadClassAtStartup(fakeClassName);

         if (MockUp.class.isAssignableFrom(fakeClass)) {
            if (argument == null) {
               ConstructorReflection.newInstanceUsingDefaultConstructor(fakeClass);
            }
            else {
               ConstructorReflection.newInstanceUsingCompatibleConstructor(fakeClass, argument);
            }
         }
      }
      catch (UnsupportedOperationException ignored) {}
      catch (Throwable unexpectedFailure) {
         StackTrace.filterStackTrace(unexpectedFailure);
         unexpectedFailure.printStackTrace();
      }
   }
}