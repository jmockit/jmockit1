/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.textFile;

import java.io.*;
import java.util.*;

public final class TextFile
{
   private final BufferedReader bufferedInput;
   private final TextReader input;
   private final long headerLength;

   public TextFile(String fileName) throws FileNotFoundException
   {
      bufferedInput = new BufferedReader(new FileReader(fileName));
      input = null;
      headerLength = 0;
   }

   public TextFile(String fileName, long headerLength) throws FileNotFoundException
   {
      this(new DefaultTextReader(fileName), headerLength);
   }

   public TextFile(TextReader input, long headerLength)
   {
      bufferedInput = null;
      this.input = input;
      this.headerLength = headerLength;
   }

   public List<String[]> parse()
   {
      skipHeader();

      List<String[]> result = new ArrayList<String[]>();

      while(true) {
         String  strLine = nextLine();

         if (strLine == null) {
            closeReader();
            break;
         }

         String[] parsedLine = strLine.split(",");
         result.add(parsedLine);
      }

      return result;
   }

   private void skipHeader()
   {
      try {
         if (bufferedInput != null) {
            bufferedInput.skip(headerLength);
         }
         else {
            input.skip(headerLength);
         }
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private String nextLine()
   {
      try {
         return bufferedInput != null ? bufferedInput.readLine() : input.readLine();
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void closeReader()
   {
      try {
         if (bufferedInput != null) {
            bufferedInput.close();
         }
         else {
            input.close();
         }
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public interface TextReader
   {
      long skip(long n) throws IOException;
      String readLine() throws IOException;
      void close() throws IOException;
   }

   static final class DefaultTextReader implements TextReader
   {
      private final Reader reader;

      DefaultTextReader(String fileName) throws FileNotFoundException
      {
         reader = new FileReader(fileName);
      }

      public long skip(long n) throws IOException
      {
         return reader.skip(n);
      }

      public String readLine() throws IOException
      {
         StringBuilder buf = new StringBuilder();

         while (true) {
            int c = reader.read();

            if (c < 0 || c == '\n') {
               break;
            }

            buf.append((char) c);
         }

         return buf.toString();
      }

      public void close() throws IOException
      {
         reader.close();
      }
   }
}
