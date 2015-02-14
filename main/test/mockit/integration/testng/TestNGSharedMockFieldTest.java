/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import java.io.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;

public final class TestNGSharedMockFieldTest
{
   public interface Dependency
   {
      boolean doSomething();
      void doSomethingElse();
   }

   @Mocked Dependency mock1;
   @Capturing Runnable mock2;
   @Mocked BufferedWriter writer;

   @Test
   public void recordAndReplayExpectationsOnSharedMocks()
   {
      // Strict mocking:
      new StrictExpectations() {{
         mock1.doSomething(); result = true;
         mock2.run();
      }};

      assertTrue(mock1.doSomething());
      mock2.run();
   }

   @Test
   public void recordAndReplayExpectationsOnSharedMocksAgain()
   {
      new Expectations() {{
         mock1.doSomething(); result = true;
         mock1.doSomethingElse(); minTimes = 0;
      }};

      assertTrue(mock1.doSomething());
      mock2.run();
   }

   @BeforeMethod
   public void preventAllWritesToMockedBufferedWritersFromSUT() throws Exception
   {
      new NonStrictExpectations() {{ writer.write(anyString, anyInt, anyInt); result = new IOException(); }};
   }

   @Test
   public void useMockedBufferedWriter() throws Exception
   {
      writer.newLine();

      try {
         writer.write("test", 0, 4);
         fail();
      }
      catch (IOException ignore) {}
   }
}
