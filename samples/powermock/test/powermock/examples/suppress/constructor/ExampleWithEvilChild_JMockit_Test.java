/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.suppress.constructor;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.*;

public final class ExampleWithEvilChild_JMockit_Test
{
   @Test
   public void testSuppressConstructorOfEvilChild()
   {
      new MockUp<EvilChild>() { @Mock void $init() {} };

      String message = "myMessage";
      ExampleWithEvilChild tested = new ExampleWithEvilChild(message);

      assertEquals(message, tested.getMessage());
   }

   @Test(expected = UnsatisfiedLinkError.class)
   public void testNotSuppressConstructorOfEvilChild()
   {
      String message = "myMessage";
      new ExampleWithEvilChild(message);
   }
}
