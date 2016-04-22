/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import org.junit.gen5.api.*;
import org.junit.gen5.api.Test;
import org.junit.gen5.junit4.runner.*;
import org.junit.runner.*;
import static org.junit.gen5.api.Assertions.*;

import mockit.*;

@RunWith(JUnit5.class)
public final class JUnit5Test
{
   @Test
   void basicExample(TestInfo testInfo)
   {
      assertNotNull(testInfo);
      System.out.println(testInfo.getDisplayName());
   }

//   @Test
   void testWithMockedParameter(@Mocked Runnable mock)
   {
      assertNull(mock);
   }
}
