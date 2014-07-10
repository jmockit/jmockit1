/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import static mockit.Deencapsulation.*;

public final class RaceConditionsOnJREMocksTest
{
   static class StuffReader
   {
      private String getStuffType() { return "stuff"; }

      String readStuff()
      {
         StringWriter out = new StringWriter();

         try {
            String stuffType = getStuffType();
            out.append(stuffType);
         }
         catch (Exception ignore) {
            out.append("Error:can't determine stuff type");
            return out.toString();
         }

         Properties props = new Properties();

         try {
            props.load(new FileInputStream("myfile.properties"));
            out.append(props.getProperty("one"));
            out.append(props.getProperty("two"));
            out.append(props.getProperty("three"));
         }
         catch (FileNotFoundException ignore) {
            out.append(" FileNotFoundException");
         }
         catch (IOException ignore) {
            out.append(" IOException");
         }

         return out.toString();
      }
   }

   static final StuffReader stuffHandler = new StuffReader();

   @Test
   public void throwsExceptionFromGetStuffType()
   {
      new Expectations(stuffHandler) {{
         invoke(stuffHandler, "getStuffType"); result = new Exception();
      }};

      String result = stuffHandler.readStuff();

      assertEquals("Error:can't determine stuff type", result);
   }

   @Test
   public void throwsFileNotFoundExceptionWhenOpeningInputFile() throws Exception
   {
      new Expectations(stuffHandler, FileInputStream.class) {{
         invoke(stuffHandler, "getStuffType"); result = "*mocked*";
         new FileInputStream(anyString); result = new FileNotFoundException();
      }};

      String result = stuffHandler.readStuff();

      assertEquals("*mocked* FileNotFoundException", result);
   }

   @Test
   public void throwsIOExceptionWhileReadingProperties(
      @Mocked("load") final Properties mockProps, @Mocked("(String)") FileInputStream mockFIS)
   {
      new Expectations(stuffHandler) {{
         invoke(stuffHandler, "getStuffType"); result = "*mocked*";

         invoke(mockProps, "load", withAny(FileInputStream.class));
         result = new IOException();
      }};

      String result = stuffHandler.readStuff();

      assertEquals("*mocked* IOException", result);
   }

   @Test
   public void getCompleteStuff(
      @Mocked({"load", "getProperty"}) final Properties props, @Mocked("(String)") FileInputStream mockFIS)
   {
      new NonStrictExpectations(stuffHandler) {{
         invoke(stuffHandler, "getStuffType"); result = "*mocked*";
         invoke(props, "getProperty", withAny("")); result = " *mocked*";
      }};

      String result = stuffHandler.readStuff();

      assertEquals("*mocked* *mocked* *mocked* *mocked*", result);
   }
}
