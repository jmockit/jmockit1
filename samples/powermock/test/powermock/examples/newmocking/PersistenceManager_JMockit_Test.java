/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.newmocking;

import java.io.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/newmocking/PersistenceManagerTest.java">PowerMock version</a>
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/newmocking/PersistenceManagerWithReplayAllAndVerifyAllTest.java">PowerMock version</a>
 */
public final class PersistenceManager_JMockit_Test
{
   @Tested PersistenceManager tested;

   @Test
   public void createDirectoryStructure_usingDynamicPartialMocking()
   {
      final String path = "directoryPath";

      new Expectations(File.class) {{
         File fileMock = new File(path);
         fileMock.exists(); result = false;
         fileMock.mkdirs(); result = true;
      }};

      assertTrue(tested.createDirectoryStructure(path));
   }

   @Test
   public void createDirectoryStructure_usingRegularMocking(@Mocked final File fileMock)
   {
      new Expectations() {{
         fileMock.exists(); result = false;
         fileMock.mkdirs(); result = true;
      }};

      assertTrue(tested.createDirectoryStructure("directoryPath"));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createDirectoryStructure_fails(@Mocked final File fileMock)
   {
      new Expectations() {{ fileMock.exists(); result = true; }};

      tested.createDirectoryStructure("directoryPath");
   }
}
