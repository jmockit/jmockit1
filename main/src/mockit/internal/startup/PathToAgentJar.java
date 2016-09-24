/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.security.*;
import java.util.jar.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.internal.*;

final class PathToAgentJar
{
   private static final Pattern JAR_REGEX = Pattern.compile(".*jmockit[-.\\d]*.jar");

   @Nonnull
   static String getPathToJarFile()
   {
      String jarFilePath = findPathToJarFileFromClasspath();

      if (jarFilePath == null) {
         jarFilePath = findOrCreateBootstrappingJarFileInTempDir();
      }

      return jarFilePath;
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

   @Nonnull
   private static String findOrCreateBootstrappingJarFileInTempDir()
   {
      String tempDir = System.getProperty("java.io.tmpdir");
      String currentVersion = currentVersion();
      File bootstrapJar = new File(tempDir, "jmockitAgent-" + currentVersion + ".jar");

      if (bootstrapJar.exists()) {
         return bootstrapJar.getPath();
      }

      createBootstrapJarInTempDir(bootstrapJar);

      return bootstrapJar.getPath();
   }

   @Nonnull
   private static String currentVersion()
   {
      Class<?> thisClass = PathToAgentJar.class;
      String currentVersion = thisClass.getPackage().getImplementationVersion();

      if (currentVersion == null) { // only happens when the class is loaded from the main/target/classes dir
         ProtectionDomain pd = thisClass.getProtectionDomain();
         String versionFile = pd.getCodeSource().getLocation().getPath() + "../../../version.txt";
         try { currentVersion = new RandomAccessFile(versionFile, "r").readLine(); }
         catch (IOException e) { throw new RuntimeException(e); }
      }

      return currentVersion;
   }

   private static void createBootstrapJarInTempDir(@Nonnull File bootstrapJar)
   {
      Manifest manifest = new Manifest();
      Attributes attrs = manifest.getMainAttributes();
      attrs.putValue("Manifest-Version", "1.0");
      attrs.putValue("Agent-Class", InstrumentationHolder.class.getName());
      attrs.putValue("Can-Redefine-Classes", "true");
      attrs.putValue("Can-Retransform-Classes", "true");

      byte[] classFile = ClassFile.readFromFile(InstrumentationHolder.class).b;

      try {
         JarOutputStream output = new JarOutputStream(new FileOutputStream(bootstrapJar), manifest);
         JarEntry classEntry = new JarEntry(InstrumentationHolder.class.getName().replace('.', '/') + ".class");
         output.putNextEntry(classEntry);
         output.write(classFile);
         output.close();
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
