/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.regex.*;

import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

final class AgentInitialization
{
   private static final Pattern JAR_REGEX = Pattern.compile(".*jmockit[-.\\d]*.jar");

   private AgentInitialization() {}

   static void loadAgentFromLocalJarFile()
   {
      if (!JAVA6 && !JAVA7 && !JAVA8) {
         throw new IllegalStateException("JMockit requires a Java 6+ VM");
      }

      String jarFilePath = discoverPathToJarFile();
      new AgentLoader(jarFilePath).loadAgent();
   }

   @NotNull
   private static String discoverPathToJarFile()
   {
      String jarFilePath = findPathToJarFileFromClasspath();

      if (jarFilePath == null) {
         // This can fail for a remote URL, so it is used as a fallback only:
         jarFilePath = getPathToJarFileContainingThisClass();
      }

      if (jarFilePath != null) {
         return jarFilePath;
      }

      throw new IllegalStateException(
         "No jar file with name ending in \"jmockit.jar\" or \"jmockit-nnn.jar\" (where \"nnn\" is a version number) " +
         "found in the classpath");
   }

   @Nullable
   private static String findPathToJarFileFromClasspath()
   {
      String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);

      for (String cpEntry : classPath) {
         if (JAR_REGEX.matcher(cpEntry).matches()) {
            return cpEntry;
         }
      }

      return null;
   }

   @Nullable
   private static String getPathToJarFileContainingThisClass()
   {
      CodeSource codeSource = AgentInitialization.class.getProtectionDomain().getCodeSource();

      if (codeSource == null) {
         return null;
      }

      URL location = codeSource.getLocation();
      String locationPath = location.getPath();
      String jarFilePath;

      if (locationPath.endsWith("/main/classes/")) {
         jarFilePath = findLocalJarOrZipFileFromLocationOfCurrentClassFile(locationPath);
      }
      else {
         jarFilePath = findJarFileContainingCurrentClass(location);
      }

      return jarFilePath;
   }

   @NotNull
   private static String findLocalJarOrZipFileFromLocationOfCurrentClassFile(@NotNull String locationPath)
   {
      File localJarFile = new File(locationPath.replace("main/classes/", "jmockit.jar"));

      if (localJarFile.exists()) {
         return localJarFile.getPath();
      }

      File localMETAINFFile = new File(locationPath.replace("classes/", "META-INF.zip"));
      return localMETAINFFile.getPath();
   }

   @NotNull
   private static String findJarFileContainingCurrentClass(@NotNull URL location)
   {
      // URI is used to deal with spaces and non-ASCII characters.
      URI jarFileURI;
      try { jarFileURI = location.toURI(); } catch (URISyntaxException e) { throw new RuntimeException(e); }

      // Certain environments (JBoss) use something other than "file:", which is not accepted by File.
      if (!"file".equals(jarFileURI.getScheme())) {
         String locationPath = location.toExternalForm();
         int p = locationPath.indexOf(':');
         return locationPath.substring(p + 2);
      }

      return new File(jarFileURI).getPath();
   }
}
