/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.textFile;

import java.io.*;
import java.util.*;

import org.junit.*;

import mockit.*;

import integrationTests.textFile.TextFile.*;
import static org.junit.Assert.*;

public final class TextFileUsingExpectationsTest
{
   @Test
   public void createTextFile(@Mocked DefaultTextReader reader) throws Exception
   {
      // Records TextFile#TextFile(String, int):
      new Expectations() {{ new DefaultTextReader("file"); }};

      new TextFile("file", 0);
   }

   @Test
   public void createTextFileByCapturingTheTextReaderClassThroughItsBaseType(@Capturing TextReader reader)
      throws Exception
   {
      new TextFile("file", 0);
   }

   @Test
   public void parseTextFileUsingConcreteClass(@Mocked final DefaultTextReader reader) throws Exception
   {
      new Expectations() {{
         // From TextFile#parse():
         reader.skip(200); result = 200L;
         reader.readLine(); returns("line1", "another,line", null);
         reader.close();
      }};

      TextFile textFile = new TextFile("file", 200);
      List<String[]> result = textFile.parse();

      assertResultFromTextFileParsing(result);
   }

   void assertResultFromTextFileParsing(List<String[]> result)
   {
      assertEquals(2, result.size());
      String[] line1 = result.get(0);
      assertEquals(1, line1.length);
      assertEquals("line1", line1[0]);
      String[] line2 = result.get(1);
      assertEquals(2, line2.length);
      assertEquals("another", line2[0]);
      assertEquals("line", line2[1]);
   }

   @Test
   public void parseTextFileUsingInterface(@Mocked final TextReader reader) throws Exception
   {
      // Records TextFile#parse():
      new Expectations() {{
         reader.skip(200); result = 200L;
         reader.readLine(); returns("line1", "another,line", null);
         reader.close();
      }};

      // Replays recorded invocations while verifying expectations:
      TextFile textFile = new TextFile(reader, 200);
      List<String[]> result = textFile.parse();

      // Verifies result:
      assertResultFromTextFileParsing(result);
   }

   @Test
   public void parseTextFileUsingBufferedReader(@Mocked FileReader fileReader, @Mocked final BufferedReader reader)
      throws Exception
   {
      // Records TextFile#TextFile(String):
      new Expectations() {{
         new BufferedReader(new FileReader("file"));
      }};

      // Records TextFile#parse():
      new Expectations() {{
         reader.skip(0); result = 0L;
         reader.readLine(); result = "line1"; result = "another,line"; result = null;
         reader.close();
      }};

      TextFile textFile = new TextFile("file");
      List<String[]> result = textFile.parse();

      assertResultFromTextFileParsing(result);
   }
}
