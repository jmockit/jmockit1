/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockingOfInternalInstancesTest
{
   static final String FILE_NAME = "testFile.out";

   PrintStream originalSystemOut;
   final OutputStream testOutput = new ByteArrayOutputStream();
   final FileIO fileIO = new FileIO();

   @Before
   public void redirectStandardOutput()
   {
      originalSystemOut = System.out;
      System.setOut(new PrintStream(testOutput));
   }

   @After
   public void restoreStandardOutput()
   {
      System.setOut(originalSystemOut);
   }

   private void assertExpectedFileIO()
   {
      File realFile = new File(FILE_NAME);
      boolean realFileCreated = realFile.exists();
      
      if (realFileCreated) {
         realFile.delete();
      }

      assertFalse("Real file created", realFileCreated);
      assertTrue("File not written", testOutput.toString().startsWith("File written"));
   }

   @Test
   public void stubOutFileCreationWithMockUps() throws Exception
   {
      new MockUp<FileWriter>() { @Mock void $init(String s) {} };
      new MockUp<OutputStreamWriter>() { @Mock void $init(OutputStream out, String s) {} };
      new MockUp<BufferedWriter>() { @Mock void close() {} };

      fileIO.writeToFile(FILE_NAME);
      assertExpectedFileIO();
   }

   @Test
   public void stubOutFileCreationWithStaticPartialMocking(
      @Mocked({"(String)", "(OutputStream, String)"}) FileWriter fileWriter,
      @Mocked("close") BufferedWriter bufferedWriter) throws Exception
   {
      fileIO.writeToFile(FILE_NAME);
      assertExpectedFileIO();
   }
}
