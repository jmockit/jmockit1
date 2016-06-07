/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import javax.annotation.*;

import mockit.*;
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

   void applyStartupMocks()
   {
      applyInternalStartupMocksAsNeeded();
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
      try {
         Class<?> mockClass = ClassLoad.loadClassAtStartup(mockClassName);

         if (MockUp.class.isAssignableFrom(mockClass)) {
            ConstructorReflection.newInstanceUsingDefaultConstructor(mockClass);
         }
      }
      catch (UnsupportedOperationException ignored) {}
      catch (Throwable unexpectedFailure) {
         StackTrace.filterStackTrace(unexpectedFailure);
         unexpectedFailure.printStackTrace();
      }
   }
}
