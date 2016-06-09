/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.instrument.*;
import javax.annotation.*;

import mockit.*;
import mockit.coverage.*;
import mockit.integration.junit4.internal.*;
import mockit.internal.*;
import mockit.internal.util.*;

final class JMockitInitialization
{
   @Nonnull private final StartupConfiguration config;

   JMockitInitialization() throws IOException
   {
      config = new StartupConfiguration();
      MockingBridge.preventEventualClassLoadingConflicts();
   }

   void initialize(@Nonnull Instrumentation inst)
   {
      applyInternalStartupMocksAsNeeded();

      if (CodeCoverage.active()) {
         inst.addTransformer(new CodeCoverage());
      }

      applyUserSpecifiedStartupMocksIfAny();
   }

   private void applyInternalStartupMocksAsNeeded()
   {
      if (MockFrameworkMethod.hasDependenciesInClasspath()) {
         new RunNotifierDecorator();
         new BlockJUnit4ClassRunnerDecorator();
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
