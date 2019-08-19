/*
 * Copyright (c) 2006 JMockit developers
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
   @Nonnull private final ClassModification classModification;
   @Nonnull private final OutputFileGenerator outputGenerator;
   private boolean inactive;

   public static void main(@Nonnull String[] args) {
      OutputFileGenerator generator = createOutputFileGenerator();
      generator.generateAggregateReportFromInputFiles(args);
   }

   @Nonnull
   private static OutputFileGenerator createOutputFileGenerator() {
      OutputFileGenerator generator = new OutputFileGenerator();
      CoverageData.instance().setWithCallPoints(generator.isWithCallPoints());
      return generator;
   }

   public static boolean active() {
      String coverageOutput  = Configuration.getProperty("output");
      String coverageClasses = Configuration.getProperty("classes");
      return (coverageOutput != null || coverageClasses != null) && !("none".equals(coverageOutput) || "none".equals(coverageClasses));
   }

   public CodeCoverage() {
      classModification = new ClassModification();
      outputGenerator = createOutputFileGenerator();

      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            TestRun.terminate();

            if (outputGenerator.isOutputToBeGenerated()) {
               if (classModification.shouldConsiderClassesNotLoaded()) {
                  new ClassesNotLoaded(classModification).gatherCoverageData();
               }

               inactive = true;
               outputGenerator.generate();
            }

            new CoverageCheck().verifyThresholds();
            Startup.instrumentation().removeTransformer(CodeCoverage.this);
         }
      });
   }

   @Nullable @Override
   public byte[] transform(
      @Nullable ClassLoader loader, @Nonnull String internalClassName, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] originalClassfile
   ) {
      if (loader == null || classBeingRedefined != null || protectionDomain == null || inactive) {
         return null;
      }

      String className = internalClassName.replace('/', '.');

      byte[] modifiedClassfile = classModification.modifyClass(className, protectionDomain, originalClassfile);
      return modifiedClassfile;
   }
}