/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.data;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.jar.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.internal.util.*;

/**
 * Coverage data captured for all source files exercised during a test run.
 */
public final class CoverageData implements Serializable
{
   private static final long serialVersionUID = -4860004226098360259L;
   @Nonnull private static final CoverageData instance = new CoverageData();

   @Nonnull public static CoverageData instance() { return instance; }

   private boolean withCallPoints;

   @Nonnull private final Map<String, FileCoverageData> fileToFileData = new LinkedHashMap<String, FileCoverageData>();
   @Nonnull private final List<FileCoverageData> indexedFileData = new ArrayList<FileCoverageData>(100);

   public boolean isWithCallPoints() { return withCallPoints; }
   public void setWithCallPoints(boolean withCallPoints) { this.withCallPoints = withCallPoints; }

   @Nonnull public Map<String, FileCoverageData> getRawFileToFileData() { return fileToFileData; }

   /**
    * Returns an immutable map containing all source files with the corresponding coverage data gathered for each
    * file during a test run.
    */
   @Nonnull
   public Map<String, FileCoverageData> getFileToFileDataMap()
   {
      Map<String, FileCoverageData> copy = new LinkedHashMap<String, FileCoverageData>(fileToFileData);

      for (Iterator<Entry<String, FileCoverageData>> itr = copy.entrySet().iterator(); itr.hasNext(); ) {
         Entry<String, FileCoverageData> fileAndFileData = itr.next();
         FileCoverageData fileData = fileAndFileData.getValue();

         if (fileData.getTotalItemsForAllMetrics() == 0) {
            itr.remove();
         }
      }

      return Collections.unmodifiableMap(copy);
   }

   @Nonnull
   public FileCoverageData getOrAddFile(@Nonnull String file, @Nullable String kindOfTopLevelType)
   {
      FileCoverageData fileData = fileToFileData.get(file);

      // For a class with nested/inner classes, a previous class in the same source file may already have been added.
      if (fileData == null) {
         int fileIndex = indexedFileData.size();
         fileData = new FileCoverageData(fileIndex, kindOfTopLevelType);
         indexedFileData.add(fileData);
         fileToFileData.put(file, fileData);
      }
      else if (kindOfTopLevelType != null) {
         fileData.kindOfTopLevelType = kindOfTopLevelType;
      }

      return fileData;
   }

   @Nonnull public FileCoverageData getFileData(@Nonnull String file) { return fileToFileData.get(file); }
   @Nonnull public FileCoverageData getFileData(int fileIndex) { return indexedFileData.get(fileIndex); }

   public boolean isEmpty() { return fileToFileData.isEmpty(); }
   public void clear() { fileToFileData.clear(); }

   /**
    * Computes the coverage percentage for a given metric, over a subset of the available source files.
    *
    * @param fileNamePrefix a regular expression for matching the names of the source files to be considered, or
    *                       <tt>null</tt> to consider <em>all</em> files
    *
    * @return the computed percentage from <tt>0</tt> to <tt>100</tt> (inclusive), or <tt>-1</tt> if no
    * meaningful value could be computed for the metric
    */
   public int getPercentage(@Nonnull Metrics metric, @Nullable String fileNamePrefix)
   {
      int coveredItems = 0;
      int totalItems = 0;

      for (Entry<String, FileCoverageData> fileAndFileData : fileToFileData.entrySet()) {
         String sourceFile = fileAndFileData.getKey();

         if (fileNamePrefix == null || sourceFile.startsWith(fileNamePrefix)) {
            FileCoverageData fileData = fileAndFileData.getValue();
            PerFileCoverage coverageInfo = fileData.getPerFileCoverage(metric);
            coveredItems += coverageInfo.getCoveredItems();
            totalItems += coverageInfo.getTotalItems();
         }
      }

      return CoveragePercentage.calculate(coveredItems, totalItems);
   }

   /**
    * Finds the source file with the smallest coverage percentage for a given metric.
    *
    * @return the percentage value for the file found, or <tt>Integer.MAX_VALUE</tt> if no file is found with a
    * meaningful coverage percentage
    */
   public int getSmallestPerFilePercentage(@Nonnull Metrics metric)
   {
      int minPercentage = Integer.MAX_VALUE;

      for (FileCoverageData fileData : fileToFileData.values()) {
         if (!fileData.wasLoadedAfterTestCompletion()) {
            PerFileCoverage coverageInfo = fileData.getPerFileCoverage(metric);
            int percentage = coverageInfo.getCoveragePercentage();

            if (percentage >= 0 && percentage < minPercentage) {
               minPercentage = percentage;
            }
         }
      }

      return minPercentage;
   }

   public void fillLastModifiedTimesForAllClassFiles()
   {
      for (Iterator<Entry<String, FileCoverageData>> itr = fileToFileData.entrySet().iterator(); itr.hasNext(); ) {
         Entry<String, FileCoverageData> fileAndFileData = itr.next();
         long lastModified = getLastModifiedTimeForClassFile(fileAndFileData.getKey());

         if (lastModified > 0L) {
            FileCoverageData fileCoverageData = fileAndFileData.getValue();
            fileCoverageData.lastModified = lastModified;
            continue;
         }

         itr.remove();
      }
   }

   private long getLastModifiedTimeForClassFile(@Nonnull String sourceFilePath)
   {
      String sourceFilePathNoExt = sourceFilePath.substring(0, sourceFilePath.lastIndexOf('.'));
      String className = sourceFilePathNoExt.replace('/', '.');

      Class<?> coveredClass = findCoveredClass(className);

      if (coveredClass == null) {
         return 0L;
      }

      String locationPath = Utilities.getClassFileLocationPath(coveredClass);

      if (locationPath.endsWith(".jar")) {
         try { return getLastModifiedTimeFromJarEntry(sourceFilePathNoExt, locationPath); }
         catch (IOException ignore) { return 0L; }
      }

      String pathToClassFile = locationPath + sourceFilePathNoExt + ".class";

      return new File(pathToClassFile).lastModified();
   }

   private static long getLastModifiedTimeFromJarEntry(
      @Nonnull String sourceFilePathNoExt, @Nonnull String locationPath)
      throws IOException
   {
      JarFile jarFile = new JarFile(locationPath);

      try {
         JarEntry classEntry = jarFile.getJarEntry(sourceFilePathNoExt + ".class");
         return classEntry.getTime();
      }
      finally {
         jarFile.close();
      }
   }

   @Nullable
   private Class<?> findCoveredClass(@Nonnull String className)
   {
      ClassLoader currentCL = getClass().getClassLoader();
      Class<?> coveredClass = loadClass(className, currentCL);

      if (coveredClass == null) {
         ClassLoader systemCL = ClassLoader.getSystemClassLoader();

         if (systemCL != currentCL) {
            coveredClass = loadClass(className, systemCL);
         }

         if (coveredClass == null) {
            ClassLoader contextCL = Thread.currentThread().getContextClassLoader();

            if (contextCL != null && contextCL != systemCL) {
               coveredClass = loadClass(className, contextCL);
            }
         }
      }

      return coveredClass;
   }

   @Nullable
   private static Class<?> loadClass(@Nonnull String className, @Nullable ClassLoader loader)
   {
      try {
         return Class.forName(className, false, loader);
      }
      catch (ClassNotFoundException ignore) { return null; }
      catch (NoClassDefFoundError ignored) { return null; }
   }

   /**
    * Reads a serialized <tt>CoverageData</tt> object from the given file (normally, a "<tt>coverage.ser</tt>" file
    * generated at the end of a previous test run).
    *
    * @param dataFile the ".ser" file containing a serialized <tt>CoverageData</tt> instance
    *
    * @return a new object containing all coverage data resulting from a previous test run
    */
   @Nonnull
   public static CoverageData readDataFromFile(@Nonnull File dataFile) throws IOException
   {
      ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));

      try {
         return (CoverageData) input.readObject();
      }
      catch (ClassNotFoundException e) {
         throw new RuntimeException(
            "Serialized class in coverage data file \"" + dataFile + "\" not found in classpath", e);
      }
      finally {
         input.close();
      }
   }

   public void writeDataToFile(@Nonnull File dataFile) throws IOException
   {
      ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));

      try {
         output.writeObject(this);
      }
      finally {
         output.close();
      }
   }

   public void merge(@Nonnull CoverageData previousData)
   {
      withCallPoints |= previousData.withCallPoints;

      for (Entry<String, FileCoverageData> previousFileAndFileData : previousData.fileToFileData.entrySet()) {
         String previousFile = previousFileAndFileData.getKey();
         FileCoverageData previousFileData = previousFileAndFileData.getValue();
         FileCoverageData fileData = fileToFileData.get(previousFile);

         if (fileData == null) {
            fileToFileData.put(previousFile, previousFileData);
         }
         else if (fileData.lastModified > 0 && previousFileData.lastModified == fileData.lastModified) {
            fileData.mergeWithDataFromPreviousTestRun(previousFileData);
         }
      }
   }
}
