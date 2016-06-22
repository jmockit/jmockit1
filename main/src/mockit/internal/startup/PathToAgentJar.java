/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.annotation.*;
import static java.io.File.*;
import static java.lang.String.*;

final class PathToAgentJar
{
   private static final Pattern JAR_REGEX = Pattern.compile(".*jmockit[-.\\d]*.jar");

   private URL thisClassLocation;

   @Nonnull
   String getPathToJarFile()
   {
      File jarFilePath = findPathToJarFileFromClasspath();

      if ((jarFilePath == null) || !jarFilePath.exists()) {
         // This can fail for a remote URL, so it is used as a fallback only:
         jarFilePath = getPathToJarFileContainingThisClass();
      }

      if ((jarFilePath == null) || !jarFilePath.exists()) {
         // This does recreate the JAR on the file system from the classpath
         // This is more costly than the other methods and may fail,
         // e. g. on a diskless JEE system, so it is used as last fallback only
         jarFilePath = recreateJarFileFromClasspath();
      }

      return jarFilePath == null ? null : jarFilePath.getPath();
   }

   @Nullable
   private static File findPathToJarFileFromClasspath()
   {
      String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);

      for (String cpEntry : classPath) {
         if (JAR_REGEX.matcher(cpEntry).matches()) {
            return new File(cpEntry);
         }
      }

      return null;
   }

   @Nullable
   private File getPathToJarFileContainingThisClass()
   {
      CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();

      if (codeSource == null) {
         return null;
      }

      thisClassLocation = codeSource.getLocation();
      String jarFilePath;

      if (thisClassLocation.getPath().endsWith("/main/classes/")) {
         jarFilePath = findLocalJarFromThisClassLocation();
      }
      else {
         jarFilePath = findJarFileContainingThisClass();
      }

      return new File(jarFilePath);
   }

   @Nullable
   private String findLocalJarFromThisClassLocation()
   {
      String locationPath = thisClassLocation.getPath();
      File localJarFile = new File(locationPath.replace("main/classes/", "jmockit.jar"));
      return localJarFile.exists() ? localJarFile.getPath() : null;
   }

   @Nonnull
   private String findJarFileContainingThisClass()
   {
      // URI is used to deal with spaces and non-ASCII characters.
      URI jarFileURI;
      try { jarFileURI = thisClassLocation.toURI(); } catch (URISyntaxException e) { throw new RuntimeException(e); }

      // Certain environments (JBoss) use something other than "file:", which is not accepted by File.
      if (!"file".equals(jarFileURI.getScheme())) {
         String locationPath = thisClassLocation.toExternalForm();
         int p = locationPath.indexOf(':');
         return locationPath.substring(p + 2);
      }

      return new File(jarFileURI).getPath();
   }

   @Nonnull
   private File recreateJarFileFromClasspath()
   {
      File tempJarFile = null;
      FileOutputStream fos = null;
      BufferedOutputStream bos = null;
      DigestOutputStream dos = null;
      JarOutputStream jos = null;
      InputStream jarContentsInputStream = null;
      InputStreamReader jarContentsInputStreamReader = null;
      BufferedReader jarContentsReader = null;
      InputStream checksumsInputStream = null;
      try
      {
         // create temp file
         tempJarFile = createTempFile("jmockit_", ".jar");
         tempJarFile.deleteOnExit();

         // calculate MD5 hash during file creation
         MessageDigest jarDigest = MessageDigest.getInstance("MD5");
         fos = new FileOutputStream(tempJarFile);
         bos = new BufferedOutputStream(fos);
         dos = new DigestOutputStream(bos, jarDigest);
         jos = new JarOutputStream(dos);

         // get the list of files and create the JAR file on disk
         Class<? extends PathToAgentJar> clazz = getClass();
         jarContentsInputStream = clazz.getResourceAsStream("/META-INF/jmockit-jar-contents.txt");
         jarContentsInputStreamReader = new InputStreamReader(jarContentsInputStream);
         jarContentsReader = new BufferedReader(jarContentsInputStreamReader);
         byte[] buffer = new byte[4096];
         Properties checksums = new Properties();
         checksumsInputStream = clazz.getResourceAsStream("/META-INF/jmockit-jar-checksums.properties");
         checksums.load(checksumsInputStream);
         for (String jarEntryPath = jarContentsReader.readLine(); jarEntryPath != null; jarEntryPath = jarContentsReader.readLine())
         {
            Enumeration<URL> jarEntryResources = clazz.getClassLoader().getResources(jarEntryPath);

            // resource not found
            if (!jarEntryResources.hasMoreElements()) {
               throw new RuntimeException("JMockit JAR resource '" + jarEntryPath + "' not found on classpath");
            }

            URL jarEntryUrl = null;
            URL jarEntryUrlCandidate = jarEntryResources.nextElement();

            // multiple resources found (e. g. "META-INF/MANIFEST.MF")
            if (jarEntryResources.hasMoreElements()) {
               // search for the correct file by MD5 sum
               while (true) {
                  InputStream jarEntryUrlCandidateStream = null;
                  BufferedInputStream bis = null;
                  DigestInputStream dis = null;
                  try {
                     MessageDigest fileDigest = MessageDigest.getInstance("MD5");
                     jarEntryUrlCandidateStream = jarEntryUrlCandidate.openStream();
                     bis = new BufferedInputStream(jarEntryUrlCandidateStream);
                     dis = new DigestInputStream(bis, fileDigest);
                     int count = dis.read(buffer);
                     while (count != -1) {
                        count = dis.read(buffer);
                     }
                     byte[] digest = fileDigest.digest();
                     StringBuilder digestStringBuilder = new StringBuilder();
                     for (byte digestByte : digest) {
                        digestStringBuilder.append(format("%02x", digestByte));
                     }
                     if (digestStringBuilder.toString().equals(checksums.getProperty(jarEntryPath))) {
                        jarEntryUrl = jarEntryUrlCandidate;
                        break;
                     }
                  } finally {
                     if (jarEntryUrlCandidateStream != null)
                     {
                        try { jarEntryUrlCandidateStream.close(); } catch (IOException ignore) {}
                     }
                     if (bis != null)
                     {
                        try { bis.close(); } catch (IOException ignore) {}
                     }
                     if (dis != null)
                     {
                        try { dis.close(); } catch (IOException ignore) {}
                     }
                  }

                  // try the next resource if there is one available
                  if (jarEntryResources.hasMoreElements()) {
                     jarEntryUrlCandidate = jarEntryResources.nextElement();
                  } else {
                     break;
                  }
               }
            } else {
               jarEntryUrl = jarEntryUrlCandidate;
            }

            // no matching resource found
            if (jarEntryUrl == null) {
               throw new RuntimeException("No matching resource for JMockit JAR resource '" + jarEntryPath + "' found on classpath");
            }

            InputStream jarEntryStream = jarEntryUrl.openStream();
            try
            {
               ZipEntry jarEntry = new ZipEntry(jarEntryPath);
               // set the time to a fixed value, so that the generated JAR does not change each time
               jarEntry.setTime(0);
               jos.putNextEntry(jarEntry);
               for (int count = jarEntryStream.read(buffer); count != -1; count = jarEntryStream.read(buffer))
               {
                  jos.write(buffer, 0, count);
               }
               jos.closeEntry();
            }
            finally
            {
               try { jarEntryStream.close(); } catch (IOException ignore) {}
            }
         }
         jos.close();

         // check whether the file is already present, then do not use the newly created file
         // this prevents overflowing of the temp directory with generated jar files and the try
         // to replace a jar that may be locked by filesystem means like on Windows
         byte[] digest = jarDigest.digest();
         StringBuilder digestStringBuilder = new StringBuilder();
         for (byte digestByte : digest) {
            digestStringBuilder.append(format("%02x", digestByte));
         }
         File jarFile = new File(System.getProperty("java.io.tmpdir"), format("jmockit-%s.jar", digestStringBuilder));
         if (!jarFile.exists())
         {
            tempJarFile.renameTo(jarFile);
         }

         return jarFile;
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
         if (tempJarFile != null)
         {
            tempJarFile.delete();
         }
         if (fos != null)
         {
            try { fos.close(); } catch (IOException ignore) {}
         }
         if (bos != null)
         {
            try { bos.close(); } catch (IOException ignore) {}
         }
         if (dos != null)
         {
            try { dos.close(); } catch (IOException ignore) {}
         }
         if (jos != null)
         {
            try { jos.close(); } catch (IOException ignore) {}
         }
         if (jarContentsInputStream != null)
         {
            try { jarContentsInputStream.close(); } catch (IOException ignore) {}
         }
         if (jarContentsInputStreamReader != null)
         {
            try { jarContentsInputStreamReader.close(); } catch (IOException ignore) {}
         }
         if (jarContentsReader != null)
         {
            try { jarContentsReader.close(); } catch (IOException ignore) {}
         }
         if (checksumsInputStream != null)
         {
            try { checksumsInputStream.close(); } catch (IOException ignore) {}
         }
      }
   }
}
