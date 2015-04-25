/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.lang.instrument.*;
import java.security.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.modification.*;
import mockit.coverage.standalone.*;

public final class CodeCoverage implements ClassFileTransformer
{
   private static CodeCoverage instance;

   @Nonnull private final ClassModification classModification;
   @Nonnull private final OutputFileGenerator outputGenerator;
   private boolean inactive;

   public static void main(@Nonnull String[] args)
   {
      if (args.length == 1) {
         String pid = args[0];

         try {
            Integer.parseInt(pid);
            new AgentLoader(pid).loadAgent();
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

   @SuppressWarnings("unused")
   public CodeCoverage() { this(true, true); }

   private CodeCoverage(boolean checkIfAlreadyInitialized, final boolean generateOutputOnShutdown)
   {
      if (checkIfAlreadyInitialized && Startup.isInitialized()) {
         throw new IllegalStateException("JMockit: coverage tool already initialized");
      }
      else if (
         generateOutputOnShutdown &&
         ("none".equals(Configuration.getProperty("output")) ||
          "none".equals(Configuration.getProperty("classes")) ||
          "none".equals(Configuration.getProperty("metrics")))
      ) {
         throw new IllegalStateException("JMockit: coverage tool disabled");
      }

      classModification = new ClassModification();
      outputGenerator = createOutputFileGenerator(classModification);
      instance = this;

      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run()
         {
            TestRun.terminate();

            if (generateOutputOnShutdown) {
               if (outputGenerator.isOutputToBeGenerated()) {
                  outputGenerator.generate(CodeCoverage.this);
               }

               new CoverageCheck().verifyThresholds();
            }

            Startup.instrumentation().removeTransformer(CodeCoverage.this);
         }
      });
   }

   @Nonnull
   public static CodeCoverage create(boolean generateOutputOnShutdown)
   {
      instance = new CodeCoverage(false, generateOutputOnShutdown);
      return instance;
   }

   public static void resetConfiguration()
   {
      Startup.instrumentation().removeTransformer(instance);
      CoverageData.instance().clear();
      Startup.instrumentation().addTransformer(create(false));
   }

   public static void generateOutput(boolean resetState)
   {
      instance.outputGenerator.generate(null);

      if (resetState) {
         CoverageData.instance().reset();
      }
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

      return classModification.modifyClass(className, protectionDomain, originalClassfile);
   }

   void deactivate() { inactive = true; }
}
