/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.suppress.constructorhierarchy;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/suppress/constructorhierarchy/ExampleWithEvilParentAndEvilGrandParentTest.java">PowerMock version</a>
 */
public final class ExampleWithEvilChildAndEvilGrandChild_JMockit_Test
{
   @Test
   public void testSuppressConstructorHierarchy()
   {
      new MockUp<EvilChild>() { @Mock void $init() {} };
      new MockUp<EvilGrandChild>() { @Mock void $init() {} };

      String message = "myMessage";
      ExampleWithEvilChildAndEvilGrandChild tested = new ExampleWithEvilChildAndEvilGrandChild(message);

      assertEquals(message, tested.getMessage());
   }

   @Test
   public void testSuppressConstructorOfEvilChild()
   {
      new MockUp<EvilChild>() { @Mock void $init() {} };
      new MockUp<EvilGrandChild>() { @Mock void $init() {} };

      String message = "myMessage";
      new ExampleWithEvilChildAndEvilGrandChild(message);
   }

   @Test(expected = UnsatisfiedLinkError.class)
   public void testNotSuppressConstructorOfEvilChild()
   {
      String message = "myMessage";
      new ExampleWithEvilChildAndEvilGrandChild(message);
   }
}
