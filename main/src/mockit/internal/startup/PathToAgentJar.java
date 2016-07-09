/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.util.jar.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.internal.*;

final class PathToAgentJar
{
   private static final Pattern JAR_REGEX = Pattern.compile(".*jmockit[-.\\d]*.jar");

   @Nonnull
   String getPathToJarFile()
   {
      String jarFilePath = findPathToJarFileFromClasspath();

      if (jarFilePath == null) {
         jarFilePath = createBootstrappingJarFileInTempDir();
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
   private String createBootstrappingJarFileInTempDir()
   {
      Manifest manifest = new Manifest();
      Attributes attrs = manifest.getMainAttributes();
      attrs.putValue("Manifest-Version", "1.0");
      attrs.putValue("Agent-Class", InstrumentationHolder.class.getName());
      attrs.putValue("Can-Redefine-Classes", "true");
      attrs.putValue("Can-Retransform-Classes", "true");

      byte[] classFile = ClassFile.readFromFile(InstrumentationHolder.class).b;

      try {
         File tempJar = File.createTempFile("jmockit", ".jar");
         tempJar.deleteOnExit();

         JarOutputStream output = new JarOutputStream(new FileOutputStream(tempJar), manifest);
         JarEntry classEntry = new JarEntry(InstrumentationHolder.class.getName().replace('.', '/') + ".class");
         output.putNextEntry(classEntry);
         output.write(classFile);
         output.close();

         return tempJar.getPath();
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
