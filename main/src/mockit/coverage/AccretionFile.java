/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;
import javax.annotation.*;

import mockit.coverage.data.*;

final class AccretionFile
{
   @Nonnull private final File outputFile;
   @Nonnull private final CoverageData newData;

   AccretionFile(@Nonnull String outputDir, @Nonnull CoverageData newData)
   {
      String parentDir = Configuration.getOrChooseOutputDirectory(outputDir);
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
