/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.lang.instrument.*;
import java.security.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.modification.*;
import mockit.internal.startup.*;

public final class CodeCoverage implements ClassFileTransformer
{
   private static CodeCoverage instance;

   @Nonnull private final ClassModification classModification;
   @Nonnull private final OutputFileGenerator outputGenerator;
   private boolean outputPendingForShutdown;
   private boolean inactive;

   public static void main(@Nonnull String[] args)
   {
      if (args.length == 1) {
         String pid = args[0];

         try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(pid);
            new AgentLoader(pid).loadAgent("coverage");
            return;
         }
         catch (NumberFormatException ignore) {}
      }

      OutputFileGenerator generator = createOutputFileGenerator(null);
      generator.generateAggregateReportFromInputFiles(args);
   }

   @Nonnull
   private static OutputFileGenerator createOutputFileGenerator(@Nullable ClassModification classModification)
   {
      OutputFileGenerator generator = new OutputFileGenerator(classModification);
      CoverageData.instance().setWithCallPoints(generator.isWithCallPoints());
      return generator;
   }

   public CodeCoverage()
   {
      classModification = new ClassModification();
      outputGenerator = createOutputFileGenerator(classModification);
      outputPendingForShutdown = true;
      instance = this;

      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run()
         {
            TestRun.terminate();

            if (outputPendingForShutdown) {
               if (outputGenerator.isOutputToBeGenerated()) {
                  outputGenerator.generate(CodeCoverage.this);
               }

               new CoverageCheck().verifyThresholds();
            }

            Startup.instrumentation().removeTransformer(CodeCoverage.this);
         }
      });
   }

   public static boolean active()
   {
      String coverageOutput  = Configuration.getProperty("output");
      String coverageClasses = Configuration.getProperty("classes");
      String coverageMetrics = Configuration.getProperty("metrics");

      return
         (coverageOutput != null || coverageClasses != null || coverageMetrics != null) &&
         !("none".equals(coverageOutput) || "none".equals(coverageClasses) || "none".equals(coverageMetrics));
   }

   @Nonnull
   public static CodeCoverage create(boolean generateOutputOnShutdown)
   {
      instance = new CodeCoverage();
      instance.outputPendingForShutdown = generateOutputOnShutdown;
      return instance;
   }

   public static void generateOutput()
   {
      instance.outputGenerator.generate(null);
      instance.outputPendingForShutdown = false;
   }

   @Nullable @Override
   public byte[] transform(
      @Nullable ClassLoader loader, @Nonnull String internalClassName, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] originalClassfile)
   {
      if (loader == null || classBeingRedefined != null || protectionDomain == null || inactive) {
         return null;
      }

      String className = internalClassName.replace('/', '.');

      byte[] modifiedClassfile = classModification.modifyClass(className, protectionDomain, originalClassfile);
      return modifiedClassfile;
   }

   void deactivate() { inactive = true; }
}
