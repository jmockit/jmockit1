/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting;

import java.io.*;
import java.security.*;
import javax.annotation.*;

import mockit.internal.util.*;

final class StaticFiles
{
   @Nonnull private final String outputDir;
   private long lastModifiedTimeOfCoverageJar;

   StaticFiles(@Nonnull String outputDir) { this.outputDir = outputDir; }

   void copyToOutputDir(boolean forSourceFilePages) throws IOException
   {
      copyFile("coverage.css");
      copyFile("coverage.js");
      copyFile("logo.png");
      copyFile("package.png");
      copyFile("class.png");
      copyFile("abstractClass.png");
      copyFile("interface.png");
      copyFile("annotation.png");
      copyFile("exception.png");
      copyFile("enum.png");

      if (forSourceFilePages) {
         copyFile("prettify.js");
      }
   }

   private void copyFile(@Nonnull String fileName) throws IOException
   {
      File outputFile = new File(outputDir, fileName);

      if (outputFile.exists() && outputFile.lastModified() > getLastModifiedTimeOfCoverageJar()) {
         return;
      }

      //noinspection IOResourceOpenedButNotSafelyClosed
      OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
      InputStream input = new BufferedInputStream(StaticFiles.class.getResourceAsStream(fileName));

      try {
         int b;

         while ((b = input.read()) != -1) {
            output.write(b);
         }
      }
      finally {
         try {
            input.close();
         }
         finally {
            output.close();
         }
      }
   }

   private long getLastModifiedTimeOfCoverageJar()
   {
      if (lastModifiedTimeOfCoverageJar == 0) {
         CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();

         if (codeSource == null) {
            lastModifiedTimeOfCoverageJar = -1;
         }
         else {
            String pathToThisJar = Utilities.getClassFileLocationPath(codeSource);
            lastModifiedTimeOfCoverageJar = new File(pathToThisJar).lastModified();
         }
      }

      return lastModifiedTimeOfCoverageJar;
   }
}
