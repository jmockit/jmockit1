/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.suppress.staticinitializer;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/suppress/staticinitializer/ExampleWithEvilStaticInitializerTest.java">PowerMock version</a>
 */
public final class ExampleWithEvilStaticInitializer_JMockit_Test
{
   @BeforeClass
   public static void setUp()
   {
      new MockUp<ExampleWithEvilStaticInitializer>() {
         @Mock void $clinit() {}
      };
   }

   @Test
   public void suppressStaticInitializer()
   {
      String message = "myMessage";
      ExampleWithEvilStaticInitializer tested = new ExampleWithEvilStaticInitializer(message);
      assertEquals(message, tested.getMessage());
   }
}
