/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.*;

public final class KeyValuesTest
{
   @Test
   public void createKeyValues()
   {
      KeyValues<Integer> keyValues = KeyValues.create(1, 2, 3);

      assertEquals(3, keyValues.getSize());
      assertSame(Integer.class, keyValues.getType());
      assertFalse(keyValues.isToAnimation());
   }

   @Test(expected = IllegalArgumentException.class)
   public void createKeyValuesWithNullParams()
   {
      //noinspection NullArgumentToVariableArgMethod
      KeyValues.create(null, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void createKeyValuesWithZeroParams()
   {
      Evaluator<Byte> evaluator = null;
      KeyValues.create(evaluator);
   }

   @Test
   public void setStartValueOnToAnimation()
   {
      KeyValues<Integer> keyValues = KeyValues.create(5);
      Integer startValue = 2;

      keyValues.setStartValue(startValue);

      Integer value = keyValues.getValue(0, 0, 0);
      assertEquals(startValue, value);
   }

   @Test
   public void setStartValueOnAnimationThatIsNotAToAnimation()
   {
      KeyValues<Integer> keyValues = KeyValues.create(1, 2, 3);

      keyValues.setStartValue(2);

      Integer startValue = Deencapsulation.getField(keyValues, "startValue");
      assertNull(startValue);
   }

   @Test
   public void getValueForToAnimationWithLowerIndexEqualToUpperIndex()
   {
      KeyValues<Integer> keyValues = KeyValues.create(5);
      keyValues.setStartValue(1);

      Integer value = keyValues.getValue(0, 0, 0);
      assertEquals(1, value.intValue());
   }

   @Test
   public void getValueForNotToAnimationWithLowerIndexEqualToUpperIndex()
   {
      KeyValues<Integer> keyValues = KeyValues.create(1, 5);

      Integer value = keyValues.getValue(0, 0, 0);
      assertEquals(1, value.intValue());
   }

   @Test
   public void getValueForToAnimationWithLowerIndexDifferentThanUpperIndex(@Mocked final Evaluator<Float> evaluator)
   {
      final float toValue = 5.0f;
      KeyValues<Float> keyValues = KeyValues.create(evaluator, toValue);
      final float startValue = 1.0f;
      keyValues.setStartValue(startValue);
      final float fraction = 0.5f;
      final float expectedValue = 2.0f;

      new Expectations() {{
         evaluator.evaluate(startValue, toValue, fraction); result = expectedValue;
      }};

      Float value = keyValues.getValue(0, 1, fraction);
      assertEquals(expectedValue, value, 0);
   }

   @Test
   public void getValueForNotToAnimationWithLowerIndexDifferentThanUpperIndex(@Mocked final Evaluator<Float> evaluator)
   {
      final float lowerValue = 1.0f;
      final float upperValue = 5.0f;
      KeyValues<Float> keyValues = KeyValues.create(evaluator, lowerValue, upperValue);
      final float fraction = 0.5f;
      final float expectedValue = 2.0f;

      new Expectations() {{
         evaluator.evaluate(lowerValue, upperValue, fraction); result = expectedValue;
      }};

      Float value = keyValues.getValue(0, 1, fraction);
      assertEquals(expectedValue, value, 0);
   }
}
