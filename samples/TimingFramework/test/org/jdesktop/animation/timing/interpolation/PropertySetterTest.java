/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.jdesktop.animation.timing.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class PropertySetterTest
{
   private static final class Animated
   {
      private int value;

      Animated() {}
      Animated(int value) { this.value = value; }

      public int getValue() { return value; }
      public void setValue(int value) { this.value = value; }
   }

   @Test
   public void createAnimatorWithGivenParams()
   {
      int duration = 500;

      Animator animator = PropertySetter.createAnimator(duration, new Animated(), "value", 1, 3);

      assertEquals(duration, animator.getDuration());
   }

   @Test
   public void createAnimatorWithGivenEvaluatorAndParams()
   {
      int duration = 500;
      Evaluator<Double> evaluator = Evaluator.create(Double.class);

      Animator animator = PropertySetter.createAnimator(duration, new Animated(), "value", evaluator, 1.0, 3.0);

      assertEquals(duration, animator.getDuration());
   }

   @Test
   public void createAnimatorWithGivenKeyFrames()
   {
      int duration = 500;
      KeyFrames<Integer> keyFrames = new KeyFrames<>(KeyValues.create(0, 2, 4));

      Animator animator = PropertySetter.createAnimator(duration, new Animated(), "value", keyFrames);

      assertEquals(duration, animator.getDuration());
   }

   @Test(expected = IllegalArgumentException.class)
   public void createAnimatorWithNonExistentProperty()
   {
      PropertySetter.createAnimator(10, new Animated(), "none", 3);
   }

   @Test
   public void beginAnimationWithoutInitialPropertyValueWhenNotToAnimation()
   {
      Animated animated = new Animated();
      PropertySetter<Integer> setter = new PropertySetter<>(animated, "value", 1, 3);

      setter.begin();

      assertEquals(0, animated.value);
   }

   @Test
   public void beginToAnimation()
   {
      final KeyValues<Integer> keyValues = KeyValues.create(3);
      final Integer startValue = 2;
      final Animated animated = new Animated(startValue);
      PropertySetter<Integer> setter = new PropertySetter<>(animated, "value", new KeyFrames<>(keyValues));

      new Expectations(KeyValues.class, Animated.class) {{
         keyValues.isToAnimation(); result = true;
         animated.getValue(); result = startValue;
      }};

      setter.begin();

      new Verifications() {{ keyValues.setStartValue(startValue); }};
   }

   @Test(expected = RuntimeException.class)
   public void beginToAnimationWithFailingProperty(@Mocked final Animated animated)
   {
      new Expectations() {{
         animated.getValue(); result = new IllegalStateException("test");
      }};

      TimingTarget setter = new PropertySetter<>(animated, "value", 3);
      setter.begin();
   }

   @Test
   public void signalTimingEventAtHalfTheTimingRange()
   {
      Animated animated = new Animated();
      PropertySetter<Integer> setter = new PropertySetter<>(animated, "value", 1, 3);

      setter.timingEvent(0.5f);

      assertEquals(2, animated.value);
   }

   @Test(expected = RuntimeException.class)
   public void signalTimingEventWithFailingProperty(@Mocked final Animated animated)
   {
      new Expectations() {{
         animated.setValue(anyInt); result = new IllegalStateException("test");
      }};

      PropertySetter<Integer> setter = new PropertySetter<>(animated, "value", 1, 3);
      setter.timingEvent(0.1f);
   }
}
