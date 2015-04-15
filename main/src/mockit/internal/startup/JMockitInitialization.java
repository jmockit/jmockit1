/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import javax.annotation.*;

import mockit.*;
import mockit.integration.junit4.internal.*;
import mockit.integration.robolectric.internal.*;
import mockit.internal.*;
import mockit.internal.util.*;

final class JMockitInitialization
{
   @Nonnull private final StartupConfiguration config;

   JMockitInitialization() throws IOException { config = new StartupConfiguration(); }

   void initialize()
   {
      MockingBridge.preventEventualClassLoadingConflicts();

      if (MockFrameworkMethod.hasDependenciesInClasspath()) {
         loadInternalStartupMocksForJUnitIntegration();
      }

      try {
         new MockRobolectricSetup();
         new MockJREClass();
      }
      catch (ClassNotFoundException ignore) {}

      loadExternalToolsIfAny();
      setUpStartupMocksIfAny();
   }

   private static void loadInternalStartupMocksForJUnitIntegration()
   {
      new RunNotifierDecorator();
      new BlockJUnit4ClassRunnerDecorator();
      new MockFrameworkMethod();
   }

   private void loadExternalToolsIfAny()
   {
      for (String toolClassName : config.externalTools) {
         try {
            new ToolLoader(toolClassName).loadTool();
         }
         catch (Throwable unexpectedFailure) {
            StackTrace.filterStackTrace(unexpectedFailure);
            unexpectedFailure.printStackTrace();
         }
      }
   }

   private void setUpStartupMocksIfAny()
   {
      for (String mockClassName : config.mockClasses) {
         setUpStartupMock(mockClassName);
      }
   }

   private static void setUpStartupMock(@Nonnull String mockClassName)
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
