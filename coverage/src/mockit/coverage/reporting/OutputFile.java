/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   private final boolean withPrettyPrint;

   public OutputFile(@Nonnull File file) throws IOException
   {
      super(file, "UTF-8");
      relPathToOutDir = "";
      withPrettyPrint = false;
   }

   public OutputFile(@Nonnull String outputDir, @Nonnull String sourceFilePath) throws IOException
   {
      super(getOutputFileCreatingDirIfNeeded(outputDir, sourceFilePath));
      relPathToOutDir = getRelativeSubPathToOutputDir(sourceFilePath);
      withPrettyPrint = true;
   }

   @Nonnull
   private static File getOutputFileCreatingDirIfNeeded(@Nonnull String outputDir, @Nonnull String sourceFilePath)
   {
      File outputFile = getOutputFile(outputDir, sourceFilePath);
      File parentDir = outputFile.getParentFile();

      if (!parentDir.exists()) {
         boolean outputDirCreated = parentDir.mkdirs();
         assert outputDirCreated : "Failed to create output dir: " + outputDir;
      }

      return outputFile;
   }

   @Nonnull
   static File getOutputFile(@Nonnull String outputDir, @Nonnull String sourceFilePath)
   {
      int p = sourceFilePath.lastIndexOf('.');
      String outputFileName = sourceFilePath.substring(0, p) + ".html";
      File outputFile = new File(outputDir, outputFileName);
      return outputFile;
   }

   @Nonnull
   private static String getRelativeSubPathToOutputDir(@Nonnull String filePath)
   {
      StringBuilder cssRelPath = new StringBuilder();
      int n = PATH_SEPARATOR.split(filePath).length;

      for (int i = 1; i < n; i++) {
         cssRelPath.append("../");
      }

      return cssRelPath.toString();
   }

   public void writeCommonHeader(@Nonnull String pageTitle)
   {
      println("<!DOCTYPE html>");
      println("<html>");
      println("<head>");
      println("  <title>" + pageTitle + "</title>");
      println("  <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>");
      println("  <link rel='stylesheet' type='text/css' href='" + relPathToOutDir + "coverage.css'/>");
      println("  <link rel='shortcut icon' type='image/png' href='" + relPathToOutDir + "logo.png'/>");
      println("  <script type='text/javascript' src='" + relPathToOutDir + "coverage.js'></script>");

      if (withPrettyPrint) {
         println("  <script type='text/javascript' src='" + relPathToOutDir + "prettify.js'></script>");
      }

      println("</head>");
      println(withPrettyPrint ? "<body onload='prettyPrint()'>" : "<body>");
   }

   public void writeCommonFooter()
   {
      println("</body>");
      println("</html>");
   }
}
