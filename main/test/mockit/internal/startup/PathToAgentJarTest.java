package mockit.internal.startup;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import static java.lang.String.format;
import static java.util.Arrays.*;
import static java.util.Collections.sort;

import mockit.*;
import org.junit.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PathToAgentJarTest {
   @Test
   public void testRecreateJarFileFromClasspath() throws IOException, URISyntaxException {
      new MockUp<PathToAgentJar>() {
         @Mock
         File findPathToJarFileFromClasspath() {
            return null;
         }

         @Mock
         File getPathToJarFileContainingThisClass() {
            return null;
         }
      };

      new MockUp<Class<PathToAgentJar>>() {
         @Mock
         InputStream getResourceAsStream(Invocation invocation, String name) throws IOException, URISyntaxException, NoSuchAlgorithmException {
            if ("/META-INF/jmockit-jar-contents.txt".equals(name)) {
               File codeSource = new File(PathToAgentJar.class.getProtectionDomain().getCodeSource().getLocation().toURI());
               StringBuilder jarContents = new StringBuilder();
               collectFileNames(codeSource.toURI(), codeSource, jarContents);
               return new ByteArrayInputStream(jarContents.toString().getBytes("UTF-8"));
            } else if ("/META-INF/jmockit-jar-checksums.properties".equals(name)) {
               File codeSource = new File(PathToAgentJar.class.getProtectionDomain().getCodeSource().getLocation().toURI());
               StringBuilder jarContents = new StringBuilder();
               byte[] buffer = new byte[4096];
               InputStream fis = null;
               BufferedInputStream bis = null;
               MessageDigest fileDigest = MessageDigest.getInstance("MD5");
               DigestInputStream dis = null;
               for (String fileName : asList(
                     "LICENSE.txt",
                     "META-INF/services/org.junit.platform.engine.TestEngine",
                     "META-INF/services/org.testng.ITestNGListener",
                     "org/junit/runner/Runner.class")) {
                  try {
                     fis = new FileInputStream(new File(codeSource, fileName));
                     bis = new BufferedInputStream(fis);
                     fileDigest.reset();
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
                     jarContents.append(format("%s = %s\n", fileName, digestStringBuilder));
                  } finally {
                     if (fis != null)
                     {
                        try { fis.close(); } catch (IOException ignore) {}
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
               }
               return new ByteArrayInputStream(jarContents.toString().getBytes("UTF-8"));
            }
            return invocation.proceed(name);
         }
      };

      String pathToJarFile = new PathToAgentJar().getPathToJarFile();
      assertThat("path to jar file", pathToJarFile, is(notNullValue()));
      File jarFile = new File(pathToJarFile);
      assertThat("jar file exists", jarFile.exists(), is(true));
      assertThat("jar file is a file", jarFile.isFile(), is(true));

      File codeSource = new File(PathToAgentJar.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      StringBuilder expectedJarContentsBuilder = new StringBuilder();
      collectFileNames(codeSource.toURI(), codeSource, expectedJarContentsBuilder);
      List<String> expectedJarContents = new ArrayList<String>();
      CharArrayReader charArrayReader = null;
      BufferedReader bufferedReader = null;
      try {
         charArrayReader = new CharArrayReader(expectedJarContentsBuilder.toString().toCharArray());
         bufferedReader = new BufferedReader(charArrayReader);
         for (String expectedJarContentsEntry = bufferedReader.readLine(); expectedJarContentsEntry != null; expectedJarContentsEntry = bufferedReader.readLine()) {
            expectedJarContents.add(expectedJarContentsEntry);
         }
      } finally {
         if (charArrayReader != null) {
            charArrayReader.close();
         }
         if (bufferedReader != null) {
            try { bufferedReader.close(); } catch (IOException ignore) {}
         }
      }
      sort(expectedJarContents);

      List<String> jarEntries = new ArrayList<String>();
      Enumeration<JarEntry> entries = new JarFile(jarFile).entries();
      while (entries.hasMoreElements()) {
         jarEntries.add(entries.nextElement().getName());
      }
      sort(jarEntries);

      assertThat("jar contents should be correct", jarEntries, is(expectedJarContents));
   }

   private void collectFileNames(URI root, File path, Appendable jarContents) throws IOException {
      if (path.isFile()) {
         jarContents.append(root.relativize(path.toURI()).toString()).append('\n');
      } else {
         for (File dirOrFile : path.listFiles()) {
            collectFileNames(root, dirOrFile, jarContents);
         }
      }
   }
}
