/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   @Injectable BufferedWriter writer;

   @Test
   public void recordAndReplayExpectationsOnSharedMocks()
   {
      new Expectations() {{
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
      }};

      assertTrue(mock1.doSomething());
      mock2.run();
   }

   @BeforeMethod
   public void preventAllWritesToMockedBufferedWritersFromSUT() throws Exception
   {
      new Expectations() {{ writer.write(anyString, anyInt, anyInt); result = new IOException(); minTimes = 0; }};
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

   public static class Collaborator {}
   public interface BaseType { Collaborator doSomething(); }
   public interface SubType extends BaseType {}

   @Mocked SubType mock;

   @Test
   public void cascadeFistTime()
   {
      Collaborator cascaded = mock.doSomething();
      assertNotNull(cascaded);
   }

   @Test
   public void cascadeSecondTime()
   {
      Collaborator cascaded = mock.doSomething();
      assertNotNull(cascaded);
   }
}
