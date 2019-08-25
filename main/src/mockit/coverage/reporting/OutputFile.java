/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting;

import java.io.*;
import java.util.regex.*;
import javax.annotation.*;

public final class OutputFile extends PrintWriter
{
   private static final Pattern PATH_SEPARATOR = Pattern.compile("/");

   @Nonnull private final String relPathToOutDir;
   private final boolean sourceFile;

   public OutputFile(@Nonnull File file) throws IOException {
      super(file, "UTF-8");
      relPathToOutDir = "";
      sourceFile = false;
   }

   public OutputFile(@Nonnull String outputDir, @Nonnull String sourceFilePath) throws IOException {
      super(getOutputFileCreatingDirIfNeeded(outputDir, sourceFilePath));
      relPathToOutDir = getRelativeSubPathToOutputDir(sourceFilePath);
      sourceFile = true;
   }

   @Nonnull
   private static File getOutputFileCreatingDirIfNeeded(@Nonnull String outputDir, @Nonnull String sourceFilePath) {
      File outputFile = getOutputFile(outputDir, sourceFilePath);
      File parentDir = outputFile.getParentFile();

      if (!parentDir.exists()) {
         boolean outputDirCreated = parentDir.mkdirs();
         assert outputDirCreated : "Failed to create output dir: " + outputDir;
      }

      return outputFile;
   }

   @Nonnull
   static File getOutputFile(@Nonnull String outputDir, @Nonnull String sourceFilePath) {
      int p = sourceFilePath.lastIndexOf('.');
      String outputFileName = sourceFilePath.substring(0, p) + ".html";
      File outputFile = new File(outputDir, outputFileName);
      return outputFile;
   }

   @Nonnull
   private static String getRelativeSubPathToOutputDir(@Nonnull String filePath) {
      StringBuilder cssRelPath = new StringBuilder();
      int n = PATH_SEPARATOR.split(filePath).length;

      for (int i = 1; i < n; i++) {
         cssRelPath.append("../");
      }

      return cssRelPath.toString();
   }

   public void writeCommonHeader(@Nonnull String pageTitle) {
      println("<!DOCTYPE html>");
      println("<html>");
      println("<head>");
      println("  <title>" + pageTitle + "</title>");
      println("  <meta charset='UTF-8'>");
      println("  <link rel='stylesheet' type='text/css' href='" + relPathToOutDir + (sourceFile ? "source" : "index") + ".css'>");
      println("  <link rel='shortcut icon' type='image/png' href='" + relPathToOutDir + "logo.png'>");
      println("  <script src='" + relPathToOutDir + "coverage.js'></script>");
      println("  <base target='_blank'>");

      if (sourceFile) {
         println("  <script src='" + relPathToOutDir + "prettify.js'></script>");
      }

      println("</head>");
      println(sourceFile ? "<body onload='prettyPrint()'>" : "<body>");
   }

   public void writeCommonFooter() {
      println("</body>");
      println("</html>");
   }
}