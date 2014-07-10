/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.textFile;

import java.io.*;
import java.util.*;

import org.junit.*;

import mockit.*;

import integrationTests.textFile.TextFile.*;
import static org.junit.Assert.*;

public final class TextFileUsingMockUpsTest
{
   @Test
   public void createTextFileUsingNamedMockUp() throws Exception
   {
      new MockTextReaderConstructor();

      new TextFile("file", 0);
   }

   static final class MockTextReaderConstructor extends MockUp<DefaultTextReader>
   {
      @Mock(invocations = 1)
      void $init(String fileName)
      {
         assertEquals("file", fileName);
      }
   }

   @Test
   public void parseTextFileUsingDefaultTextReader() throws Exception
   {
      new MockTextReaderConstructor();
      new MockTextReaderForParse<DefaultTextReader>() {};

      TextFile textFile = new TextFile("file", 200);
      List<String[]> result = textFile.parse();

      assertResultFromTextFileParsing(result);
   }

   static class MockTextReaderForParse<T extends TextReader> extends MockUp<T>
   {
      static final String[] LINES = { "line1", "another,line", null};
      int invocation;

      @Mock(invocations = 1)
      long skip(long n)
      {
         assertEquals(200, n);
         return n;
      }

      @Mock(invocations = 3)
      String readLine() throws IOException { return LINES[invocation++]; }

      @Mock(invocations = 1)
      void close() {}
   }

   private void assertResultFromTextFileParsing(List<String[]> result)
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
   public void doesNotCloseTextReaderInCaseOfIOFailure() throws Exception
   {
      new MockTextReaderConstructor();
      new MockTextReaderForParse<DefaultTextReader>(){
         @Override @Mock
         String readLine() throws IOException { throw new IOException(); }

         @Override @Mock(invocations = 0)
         void close() {}
      };

      TextFile textFile = new TextFile("file", 200);

      try {
         textFile.parse();
         fail();
      }
      catch (RuntimeException e) {
         assertTrue(e.getCause() instanceof IOException);
      }
   }

   @Test
   public void parseTextFileUsingProvidedTextReader() throws Exception
   {
      TextReader textReader = new MockTextReaderForParse<TextReader>(){}.getMockInstance();

      TextFile textFile = new TextFile(textReader, 200);
      List<String[]> result = textFile.parse();

      assertResultFromTextFileParsing(result);
   }
}