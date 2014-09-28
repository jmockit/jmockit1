/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import java.awt.*;
import java.awt.image.*;
import java.util.List;

import org.junit.*;

import mockit.*;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.*;
import static org.junit.Assert.*;

public final class CompositeEffectTest
{
   @Mocked ComponentState start;
   @Mocked ComponentState end;

   CompositeEffect composite;
   Effect effect1;
   Effect effect2;

   @Before
   public void createEffects()
   {
      composite = new CompositeEffect();
      effect1 = new Unchanging();
      effect2 = new Unchanging();
   }

   @Test
   public void addEffect()
   {
      Effect effect = new Move(start, end);
      effect.setRenderComponent(true);

      composite.addEffect(effect);

      assertEffectWasAdded(effect);
      assertTrue(composite.getRenderComponent());
      assertSame(start, composite.getStart());
      assertSame(end, composite.getEnd());
   }

   void assertEffectWasAdded(Effect effect)
   {
      @SuppressWarnings("unchecked") List<Effect> effects = Deencapsulation.getField(composite, List.class);
      assertTrue(effects.contains(effect));
   }

   @Test
   public void addEffectWhichDoesNotRequireRerendering()
   {
      Effect effect = new Move(start, end);

      composite.addEffect(effect);

      assertEffectWasAdded(effect);
      assertFalse(composite.getRenderComponent());
   }

   @Test
   public void addEffectWhenStartAndEndStatesAreAlreadySet(
      @Mocked ComponentState anotherStart, @Mocked ComponentState anotherEnd)
   {
      Effect effect = new Move(start, end);
      composite.setStart(anotherStart);
      composite.setEnd(anotherEnd);

      composite.addEffect(effect);

      assertEffectWasAdded(effect);
      assertSame(anotherStart, composite.getStart());
      assertSame(anotherEnd, composite.getEnd());
   }

   @Test
   public void setStart()
   {
      composite.addEffect(effect1);
      composite.addEffect(effect2);

      composite.setStart(start);

      assertSame(start, effect1.getStart());
      assertSame(start, effect2.getStart());
      assertSame(start, composite.getStart());
   }

   @Test
   public void setEnd()
   {
      composite.addEffect(effect1);
      composite.addEffect(effect2);

      composite.setEnd(end);

      assertSame(end, effect1.getEnd());
      assertSame(end, effect2.getEnd());
      assertSame(end, composite.getEnd());
   }

   @Test
   public void init(@Injectable final Animator animator, @Mocked final Effect anyEffect)
   {
      composite.addEffect(effect1);
      composite.addEffect(effect2);

      composite.init(animator, null);

      new VerificationsInOrder() {{
         onInstance(effect1).init(animator, composite);
         onInstance(effect2).init(animator, composite);
         onInstance(composite); anyEffect.init(animator, null);
      }};
   }

   @Test
   public void cleanup(@Injectable final Animator animator, @Injectable final Effect anEffect)
   {
      composite.addEffect(anEffect);

      composite.cleanup(animator);

      new Verifications() {{ anEffect.cleanup(animator); }};
   }

   @Test
   public void setup(@Mocked Effect anyEffect)
   {
      composite = new CompositeEffect(effect1);
      composite.addEffect(effect2);

      composite.setup(null);

      new CompositeSetupVerifications();
   }

   final class CompositeSetupVerifications extends VerificationsInOrder
   {
      CompositeSetupVerifications()
      {
         onInstance(effect1).setup((Graphics2D) any);
         onInstance(effect2).setup((Graphics2D) any);

         Effect effectSuper = new Unchanging();
         onInstance(composite); effectSuper.setup((Graphics2D) any);
      }
   }

   @Test
   public void setupWhenComponentNeedsRerendering(@Mocked final Effect anyEffect)
   {
      composite = new CompositeEffect(effect1);
      composite.addEffect(effect2);

      new Expectations() {{ composite.getRenderComponent(); times = 1; result = true; }};

      composite.setup(null);

      new CompositeSetupVerifications();
      new Verifications() {{ anyEffect.getComponentImage(); times = 0; }};
   }

   @Test
   public void setupWhenComponentImageHasBeenSetupAlready(@Mocked Effect anyEffect)
   {
      composite = new CompositeEffect(effect1);
      composite.addEffect(effect2);

      new Expectations() {{
         composite.getComponentImage(); times = 1;
         result = new BufferedImage(10, 5, BufferedImage.TYPE_BYTE_GRAY);
      }};

      composite.setup(null);

      new CompositeSetupVerifications();
   }

   @Test
   public void operationsWithNoSubEffects(@Mocked final Animator animator, @Mocked final Effect effectSuper)
   {
      composite.init(animator, null);
      composite.setStart(null);
      composite.setup(null);
      composite.setEnd(null);
      composite.cleanup(animator);

      new VerificationsInOrder() {{
         effectSuper.init(animator, null);
         effectSuper.setStart(null);
         effectSuper.setup(null);
         effectSuper.setEnd(null);
      }};
   }
}
