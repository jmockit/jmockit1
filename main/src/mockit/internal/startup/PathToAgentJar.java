/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.annotation.*;

import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.util.jar.JarFile.MANIFEST_NAME;

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

      FileOutputStream fos = null;
      BufferedOutputStream bos = null;
      DigestOutputStream dos = null;
      JarOutputStream jos = null;
      FileInputStream fis = null;
      BufferedInputStream bis = null;
      DigestInputStream dis = null;
      try
      {
         // calculate MD5 hash of bootstrapping jar file
         MessageDigest jarDigest = MessageDigest.getInstance("MD5");
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         dos = new DigestOutputStream(baos, jarDigest);
         jos = new JarOutputStream(dos);

         // write the manifest entry manually to be able to set the time
         ZipEntry manifestEntry = new ZipEntry(MANIFEST_NAME);
         // set the time to a fixed value, so that the generated JAR does not change each time
         manifestEntry.setTime(0);
         jos.putNextEntry(manifestEntry);
         manifest.write(jos);
         jos.closeEntry();

         ZipEntry classEntry = new ZipEntry(InstrumentationHolder.class.getName().replace('.', '/') + ".class");
         // set the time to a fixed value, so that the generated JAR does not change each time
         classEntry.setTime(0);
         jos.putNextEntry(classEntry);
         jos.write(classFile);
         jos.closeEntry();
         jos.close();

         // check whether the file is already present, then do not use the newly created file
         // this prevents overflowing of the temp directory with generated jar files and the try
         // to replace a jar that may be locked by filesystem means like on Windows
         byte[] digest = jarDigest.digest();
         StringBuilder digestStringBuilder = new StringBuilder();
         for (byte digestByte : digest)
         {
            digestStringBuilder.append(format("%02x", digestByte));
         }
         File jarFile = new File(System.getProperty("java.io.tmpdir"), format("jmockit-%s.jar", digestStringBuilder));
         if (!jarFile.exists())
         {
            fos = new FileOutputStream(jarFile);
            bos = new BufferedOutputStream(fos);
            bos.write(baos.toByteArray());
         }
         else
         {
            jarDigest.reset();
            fis = new FileInputStream(jarFile);
            bis = new BufferedInputStream(fis);
            dis = new DigestInputStream(bis, jarDigest);
            while (dis.read() != -1);
            fis.close();
            if (!Arrays.equals(digest, jarDigest.digest()))
            {
               if (jarFile.delete())
               {
                  fos = new FileOutputStream(jarFile);
                  bos = new BufferedOutputStream(fos);
                  bos.write(baos.toByteArray());
               }
               else
               {
                  // create temp file
                  File tempJarFile = createTempFile("jmockit_", ".jar");
                  // as the JAR is used as agent JAR, this will not work on Oracle JVM currently,
                  // but maybe it will work on other JVMs or later versions,
                  // so leave this call here optimistically, at worst it has no effect
                  tempJarFile.deleteOnExit();
                  fos = new FileOutputStream(tempJarFile);
                  bos = new BufferedOutputStream(fos);
                  bos.write(baos.toByteArray());
                  return tempJarFile.getPath();
               }
            }
         }

         return jarFile.getPath();
      }
      catch (NoSuchAlgorithmException e)
      {
         // MD5 is not supported, this is against the specification
         throw new AssertionError(e);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      finally
      {
         if (bos != null)
         {
            try { bos.close(); } catch (IOException ignore) {}
         }
         if (fos != null)
         {
            try { fos.close(); } catch (IOException ignore) {}
         }
         if (jos != null)
         {
            try { jos.close(); } catch (IOException ignore) {}
         }
         if (dos != null)
         {
            try { dos.close(); } catch (IOException ignore) {}
         }
         if (dis != null)
         {
            try { dis.close(); } catch (IOException ignore) {}
         }
         if (bis != null)
         {
            try { bis.close(); } catch (IOException ignore) {}
         }
         if (fis != null)
         {
            try { fis.close(); } catch (IOException ignore) {}
         }
      }
   }
}
