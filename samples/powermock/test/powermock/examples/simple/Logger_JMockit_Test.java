/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.simple;

import java.io.*;

import org.junit.*;

import mockit.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/simple/src/test/java/demo/org/powermock/examples/simple/LoggerTest.java">PowerMock version</a>
 */
public final class Logger_JMockit_Test
{
   @Test(expected = IllegalStateException.class)
   public void testException(@Mocked FileWriter fileWriter) throws Exception
   {
      new Expectations() {{
         new FileWriter("target/logger.log"); result = new IOException();
      }};

      new Logger();
   }

   @Test
   public void testLogger(@Mocked FileWriter fileWriter)
   {
      new Expectations(PrintWriter.class) {{
         PrintWriter pw = new PrintWriter((Writer) any);
         pw.println("qwe"); times = 1;
      }};

      Logger logger = new Logger();
      logger.log("qwe");
   }
}
