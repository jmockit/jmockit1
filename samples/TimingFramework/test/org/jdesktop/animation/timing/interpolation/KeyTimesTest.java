/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.junit.*;

import static org.junit.Assert.*;

public final class KeyTimesTest
{
   @Test
   public void testCreateAndExerciseKeyTimes()
   {
      KeyTimes keyTimes = new KeyTimes(0.0f, 0.5f, 1.0f);

      assertEquals(3, keyTimes.getSize());
      assertEquals(0.0f, keyTimes.getTime(0), 0.0f);
      assertEquals(0.5f, keyTimes.getTime(1), 0.0f);
      assertEquals(1.0f, keyTimes.getTime(2), 0.0f);
      assertEquals(0, keyTimes.getInterval(0.0f));
      assertEquals(1, keyTimes.getInterval(1));
      assertEquals(2, keyTimes.getInterval(2));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCreateKeyTimesWithFirstValueNotZero()
   {
      new KeyTimes(1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCreateKeyTimesWithLastValueNotOne()
   {
      new KeyTimes(0.0f, 0.1f);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCreateKeyTimesWithValuesNotInIncreasingOrder()
   {
      new KeyTimes(0.0f, 0.5f, 0.2f, 1.0f);
   }
}
