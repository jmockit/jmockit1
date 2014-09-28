/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.effects.*;
import static org.junit.Assert.*;

public final class AnimationStateTest
{
   final JComponent component = new JButton();

   @Test
   public void createAnimationStartStateFromComponentState()
   {
      ComponentState componentState = new ComponentState(component);
      AnimationState state = new AnimationState(componentState, true);

      assertSame(component, state.getComponent());
      assertSame(componentState, state.getStart());
      assertNull(state.getEnd());
   }

   @Test
   public void createAnimationEndStateFromComponentState()
   {
      ComponentState componentState = new ComponentState(component);
      AnimationState state = new AnimationState(componentState, false);

      assertSame(component, state.getComponent());
      assertNull(state.getStart());
      assertSame(componentState, state.getEnd());
   }

   @Test
   public void createAnimationStartStateFromComponent()
   {
      AnimationState state = new AnimationState(component, true);

      assertSame(component, state.getComponent());
      assertEquals(new ComponentState(component), state.getStart());
      assertNull(state.getEnd());
   }

   @Test
   public void createAnimationEndStateFromComponent()
   {
      AnimationState state = new AnimationState(component, false);

      assertSame(component, state.getComponent());
      assertNull(state.getStart());
      assertEquals(new ComponentState(component), state.getEnd());
   }

   @Test
   public void resetStartState()
   {
      AnimationState state = new AnimationState(component, true);
      ComponentState initialStartState = state.getStart();

      state.setStart(new ComponentState(component));

      assertNotSame(initialStartState, state.getStart());
   }

   @Test
   public void resetEndState()
   {
      AnimationState state = new AnimationState(component, false);
      ComponentState initialEndState = state.getEnd();

      state.setEnd(new ComponentState(component));

      assertNotSame(initialEndState, state.getEnd());
   }

   @Test
   public void initWithStartStateOnly(@Mocked final Animator animator, @Mocked final FadeOut effect)
   {
      AnimationState state = new AnimationState(component, true);

      state.init(animator);

      new Verifications() {{ effect.init(animator, null); }};
   }

   @Test
   public void initWithEndStateOnly(@Mocked final Animator animator, @Mocked final FadeIn effect)
   {
      AnimationState state = new AnimationState(component, false);

      state.init(animator);

      new Verifications() {{ effect.init(animator, null); }};
   }

   @Test
   public void initWithBothStartAndEndStates(@Mocked final Animator animator, @Mocked final Unchanging effect)
   {
      AnimationState state = new AnimationState(component, true);
      state.setEnd(new ComponentState(component));

      state.init(animator);

      new Verifications() {{ effect.init(animator, null); }};
   }

   @Test
   public void initWithStartAndEndStatesInDifferentLocations(@Mocked final Animator animator, @Mocked final Move move)
   {
      AnimationState state = new AnimationState(component, true);

      component.setLocation(20, 15);
      state.setEnd(new ComponentState(component));

      state.init(animator);

      new Verifications() {{ move.init(animator, null); }};
   }

   @Test
   public void initWithStartAndEndStatesHavingDifferentSizes(@Mocked final Animator animator, @Mocked final Scale scale)
   {
      AnimationState state = new AnimationState(component, true);

      component.setSize(200, 150);
      state.setEnd(new ComponentState(component));

      state.init(animator);

      new Verifications() {{ scale.init(animator, null); }};
   }

   @Test
   public void initWithStartAndEndStatesHavingDifferentLocationsAndSizes(
      @Mocked final Animator animator, @Mocked final CompositeEffect composite)
   {
      AnimationState state = new AnimationState(component, true);
      component.setBounds(20, 15, 200, 150);

      final ComponentState startState = state.getStart();
      final ComponentState endState = new ComponentState(component);
      state.setEnd(endState);

      state.init(animator);

      new VerificationsInOrder() {{
         Scale addedScale;
         composite.addEffect(addedScale = withCapture());
         assertSame(startState, addedScale.getStart());
         assertSame(endState, addedScale.getEnd());

         composite.init(animator, null);
      }};
   }

   @Test
   public void initWithStartStateOnlyForComponentWithCustomEffect(
      @Mocked final Animator animator, @Mocked final Effect customEffect)
   {
      final AnimationState state = new AnimationState(component, true);

      TransitionType.DISAPPEARING.setEffect(component, customEffect);
      state.init(animator);

      new VerificationsInOrder() {{
         customEffect.setStart(state.getStart());
         customEffect.init(animator, null);
      }};
   }

   @Test
   public void initWithEndStateOnlyForComponentWithCustomEffect(
      @Mocked final Animator animator, @Mocked final Effect customEffect)
   {
      final AnimationState state = new AnimationState(component, false);

      TransitionType.APPEARING.setEffect(component, customEffect);
      state.init(animator);

      new VerificationsInOrder() {{
         customEffect.setEnd(state.getEnd());
         customEffect.init(animator, null);
      }};
   }

   @Test
   public void initWithBothStartAndEndStatesForComponentWithCustomEffect(
      @Mocked final Animator animator, @Mocked Effect effect, @Mocked final Move customEffect)
   {
      final AnimationState state = new AnimationState(component, true);
      state.setEnd(new ComponentState(component));

      TransitionType.CHANGING.setEffect(component, customEffect);
      state.init(animator);

      new VerificationsInOrder() {         {
         customEffect.setStart(state.getStart());
         customEffect.setEnd(state.getEnd());
         customEffect.init(animator, null);
      }};
   }

   @Test
   public void cleanupState(@Mocked final Animator animator, @Mocked final FadeOut effect)
   {
      AnimationState state = new AnimationState(component, true);
      Deencapsulation.setField(state, effect);

      state.cleanup(animator);

      new Verifications() {{ effect.cleanup(animator); }};
   }

   @Test
   public void paintState(@Mocked final Graphics2D graphics2D, @Mocked Animator animator, @Mocked final Effect effect)
   {
      AnimationState state = new AnimationState(component, true);

      // Does nothing when no effect is yet defined.
      state.paint(null);

      // Test painting with a defined effect:
      new Expectations() {{ graphics2D.create(); result = graphics2D; }};

      Deencapsulation.setField(state, effect);
      state.paint(graphics2D);

      new VerificationsInOrder() {{
         effect.render(graphics2D);
         graphics2D.dispose();
      }};
   }
}
