/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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

   @Test
   public void stubOutFileCreation() throws Exception
   {
      new MockUp<FileWriter>() { @Mock void $init(String s) {} };
      new MockUp<OutputStreamWriter>() { @Mock void $init(OutputStream out, String s) {} };
      new MockUp<BufferedWriter>() { @Mock void close() {} };

      new FileIO().writeToFile(FILE_NAME);

      assertExpectedFileIO();
   }

   void assertExpectedFileIO()
   {
      File realFile = new File(FILE_NAME);
      boolean realFileCreated = realFile.exists();

      if (realFileCreated) {
         realFile.delete();
      }

      assertFalse("Real file created", realFileCreated);
      assertTrue("File not written", testOutput.toString().startsWith("File written"));
   }
}
