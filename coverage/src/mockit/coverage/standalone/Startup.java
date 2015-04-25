/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.io.*;
import java.lang.instrument.*;
import javax.annotation.*;

import mockit.coverage.*;

public final class Startup
{
   private static Instrumentation instrumentation;
   private static boolean inATestRun = true;
   private static boolean jmockitAvailable = true;

   private Startup() {}

   public static void premain(String agentArgs, @Nonnull Instrumentation inst) throws IOException
   {
      instrumentation = inst;
      discoverOptionalDependenciesThatAreAvailableInClassPath();

      if (!inATestRun) {
         CoverageControl.create();
      }

      ClassFileTransformer coverageTransformer = CodeCoverage.create(inATestRun);
      inst.addTransformer(coverageTransformer);

      if (jmockitAvailable) {
         mockit.internal.startup.Startup.initialize(inst);
      }
   }

   @SuppressWarnings("unused")
   public static void agentmain(String agentArgs, @Nonnull Instrumentation inst)
   {
      instrumentation = inst;
      inATestRun = false;
      jmockitAvailable = false;
      CoverageControl.create();
      inst.addTransformer(CodeCoverage.create(false));
   }

   private static void discoverOptionalDependenciesThatAreAvailableInClassPath()
   {
      inATestRun = isAvailableInClassPath("org.junit.Assert") || isAvailableInClassPath("org.testng.Assert");
      jmockitAvailable = isAvailableInClassPath("mockit.Invocations");
   }

   private static boolean isAvailableInClassPath(@Nonnull String className)
   {
      ClassLoader currentLoader = Startup.class.getClassLoader();

      try {
         Class.forName(className, false, currentLoader);
         return true;
      }
      catch (ClassNotFoundException ignore) {
         return false;
      }
   }

   @Nonnull
   public static Instrumentation instrumentation()
   {
      if (instrumentation == null) {
         instrumentation = mockit.internal.startup.Startup.instrumentation();
      }

      return instrumentation;
   }

   public static boolean isTestRun() { return inATestRun; }
   public static boolean isJMockitAvailable() { return jmockitAvailable; }
   public static boolean isInitialized() { return instrumentation != null; }
}
