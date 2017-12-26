/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.util.*;
import javax.annotation.*;

import static java.util.Arrays.asList;

import mockit.*;
import mockit.coverage.*;
import mockit.integration.junit4.internal.*;
import mockit.internal.reflection.*;
import mockit.internal.util.*;

final class JMockitInitialization
{
   static void initialize(@Nonnull Instrumentation inst)
   {
      preventEventualClassLoadingConflicts();
      applyInternalStartupFakesAsNeeded();

      if (CodeCoverage.active()) {
         inst.addTransformer(new CodeCoverage());
      }

      applyUserSpecifiedStartupFakesIfAny();
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   private static void preventEventualClassLoadingConflicts()
   {
      // Ensure the proper loading of data files by the JRE, whose names depend on calls to the System class,
      // which may get @Mocked.
      TimeZone.getDefault();
      Locale.getDefault();
      Currency.getInstance(Locale.CANADA);

      DefaultValues.computeForReturnType("()J");
      Utilities.calledFromSpecialThread();
   }

   private static void applyInternalStartupFakesAsNeeded()
   {
      if (FakeFrameworkMethod.hasDependenciesInClasspath()) {
         new RunNotifierDecorator();
         new FakeFrameworkMethod();
      }
   }

   private static void applyUserSpecifiedStartupFakesIfAny()
   {
      Collection<String> fakeClasses = getFakeClasses();

      for (String fakeClassName : fakeClasses) {
         applyStartupFake(fakeClassName);
      }
   }

   @Nonnull
   private static Collection<String> getFakeClasses()
   {
      String commaOrSpaceSeparatedValues = System.getProperty("fakes");

      if (commaOrSpaceSeparatedValues == null) {
         return Collections.emptyList();
      }

      String[] fakeClassNames = commaOrSpaceSeparatedValues.split("\\s*,\\s*|\\s+");
      Set<String> uniqueClassNames = new HashSet<String>(asList(fakeClassNames));
      uniqueClassNames.remove("");
      return uniqueClassNames;
   }

   private static void applyStartupFake(@Nonnull String fakeClassName)
   {
      String argument = null;
      int p = fakeClassName.indexOf('=');

      if (p > 0) {
         argument = fakeClassName.substring(p + 1);
         fakeClassName = fakeClassName.substring(0, p);
      }

      try {
         Class<?> fakeClass = ClassLoad.loadClassAtStartup(fakeClassName);

         if (MockUp.class.isAssignableFrom(fakeClass)) {
            if (argument == null) {
               ConstructorReflection.newInstanceUsingDefaultConstructor(fakeClass);
            }
            else {
               ConstructorReflection.newInstance(fakeClass, argument);
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
