/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;

import org.jetbrains.annotations.*;

import mockit.coverage.data.*;

final class AccretionFile
{
   @NotNull private final File outputFile;
   @NotNull private final CoverageData newData;

   AccretionFile(@NotNull String outputDir, @NotNull CoverageData newData)
   {
      String parentDir = outputDir.isEmpty() ? null : outputDir;
      outputFile = new File(parentDir, "coverage.ser");

      newData.fillLastModifiedTimesForAllClassFiles();
      this.newData = newData;
   }

   void mergeDataFromExistingFileIfAny() throws IOException
   {
      if (outputFile.exists()) {
         CoverageData previousData = CoverageData.readDataFromFile(outputFile);
         newData.merge(previousData);
      }
   }

   void generate() throws IOException
   {
      newData.writeDataToFile(outputFile);
      System.out.println("JMockit: Coverage data written to " + outputFile.getCanonicalPath());
   }
}
