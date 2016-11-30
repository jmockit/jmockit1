/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests;

import mockit.*;

import junit.framework.*;

public final class JUnit38StyleTest extends TestCase
{
   @Override
   public void setUp()
   {
      useClassMockedInPreviousJUnit4TestClass();
   }

   public void testUseClassMockedInPreviousJUnit4TestClass()
   {
      useClassMockedInPreviousJUnit4TestClass();
   }

   void useClassMockedInPreviousJUnit4TestClass()
   {
      ClassWithObjectOverrides test = new ClassWithObjectOverrides("test");
      assertEquals("test", test.toString());
   }
}
