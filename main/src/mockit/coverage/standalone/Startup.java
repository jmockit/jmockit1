/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.io.*;
import java.lang.instrument.*;
import javax.annotation.*;

import mockit.coverage.*;

public final class Startup
{
   private static boolean inATestRun = true;

   private Startup() {}

   public static void premain(String agentArgs, @Nonnull Instrumentation inst) throws IOException
   {
      discoverOptionalDependenciesThatAreAvailableInClassPath();

      if (!inATestRun) {
         CoverageControl.create();
      }

      ClassFileTransformer coverageTransformer = CodeCoverage.create(inATestRun);
      inst.addTransformer(coverageTransformer);

      mockit.internal.startup.Startup.initialize(inst);
   }

   @SuppressWarnings("unused")
   public static void agentmain(String agentArgs, @Nonnull Instrumentation inst) throws IOException
   {
      inATestRun = false;

      try {
         CoverageControl.create();
         inst.addTransformer(CodeCoverage.create(false));
      }
      catch (Throwable t) {
         PrintWriter out = new PrintWriter("coverage-failure.txt");
         t.printStackTrace(out);
         out.close();
      }
   }

   private static void discoverOptionalDependenciesThatAreAvailableInClassPath()
   {
      inATestRun = isAvailableInClassPath("org.junit.Assert") || isAvailableInClassPath("org.testng.Assert");
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

   public static boolean isTestRun() { return inATestRun; }
}
