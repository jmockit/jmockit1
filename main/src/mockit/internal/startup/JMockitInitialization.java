/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.coverage.*;
import mockit.integration.junit4.internal.*;
import mockit.internal.reflection.*;
import mockit.internal.util.*;

final class JMockitInitialization
{
   @Nonnull private final StartupConfiguration config;

   JMockitInitialization() { config = new StartupConfiguration(); }

   void initialize(@Nonnull Instrumentation inst)
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

   private void applyInternalStartupFakesAsNeeded()
   {
      if (FakeFrameworkMethod.hasDependenciesInClasspath()) {
         new RunNotifierDecorator();
         new FakeFrameworkMethod();
      }
   }

   private void applyUserSpecifiedStartupFakesIfAny()
   {
      for (String fakeClassName : config.fakeClasses) {
         applyStartupFake(fakeClassName);
      }
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
