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
      applyInternalStartupMocksAsNeeded();

      if (CodeCoverage.active()) {
         inst.addTransformer(new CodeCoverage());
      }

      applyUserSpecifiedStartupMocksIfAny();
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

   private void applyInternalStartupMocksAsNeeded()
   {
      if (MockFrameworkMethod.hasDependenciesInClasspath()) {
         new RunNotifierDecorator();
         new MockFrameworkMethod();
      }
   }

   private void applyUserSpecifiedStartupMocksIfAny()
   {
      for (String mockClassName : config.mockClasses) {
         applyStartupMock(mockClassName);
      }
   }

   private static void applyStartupMock(@Nonnull String mockClassName)
   {
      String argument = null;
      int p = mockClassName.indexOf('=');

      if (p > 0) {
         argument = mockClassName.substring(p + 1);
         mockClassName = mockClassName.substring(0, p);
      }

      try {
         Class<?> mockClass = ClassLoad.loadClassAtStartup(mockClassName);

         if (MockUp.class.isAssignableFrom(mockClass)) {
            if (argument == null) {
               ConstructorReflection.newInstanceUsingDefaultConstructor(mockClass);
            }
            else {
               ConstructorReflection.newInstance(mockClass, argument);
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
