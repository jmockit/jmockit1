/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.image.*;
import java.util.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;
import static mockit.Deencapsulation.*;

import org.jdesktop.animation.timing.*;

public final class AnimationManagerInitTest
{
   @Mocked Animator animator;
   @Mocked AnimationState animationState;
   @Mocked ComponentState componentState;

   AnimationManager manager;
   JComponent component;

   @Mocked BufferedImage bgImage;

   @Before
   public void setUp()
   {
      final JComponent container = new JPanel();
      container.setSize(100, 100);

      manager = new AnimationManager(container);

      @SuppressWarnings("unchecked") Map<JComponent, AnimationState> animationStates = getField(manager, Map.class);
      component = new JButton();
      animationStates.put(component, animationState);

      // Common stubbings:
      new NonStrictExpectations(container) {{
         container.createImage(100, 100); result = bgImage;
      }};
   }

   @After
   public void verifyInitializationOfAnimationState()
   {
      new Verifications() {{ animationState.init(animator); }};
   }

   @Test
   public void initForComponentWithStartStateOnly()
   {
      new Expectations() {{
         // Expect checking of states to remove those components completely outside the container:
         animationState.getStart(); result = componentState;
      }};

      manager.init(animator);
   }

   @Test
   public void initForComponentWithEndStateOnly()
   {
      // Expect checking of states to remove those components completely outside the container:
      new Expectations() {{
         animationState.getEnd(); result = componentState;
      }};

      manager.init(animator);
   }

   @Test
   public void initForComponentWithStartAndEndStates()
   {
      // Expect checking of states to remove those components completely outside the container:
      new Expectations() {{
         animationState.getStart(); result = componentState;
         animationState.getEnd(); result = componentState;
      }};

      manager.init(animator);
   }

   @Test
   public void initForComponentCompletelyOutsideTheContainer()
   {
      new Expectations() {{
         animationState.getStart(); result = componentState;
         componentState.getX(); result = -10;
         componentState.getY(); result = -8;
         componentState.getWidth(); result = 8;
         componentState.getHeight(); result = 6;
      }};

      manager.init(animator);
   }

   @Test
   public void initForChangingComponent()
   {
      @SuppressWarnings("unchecked") List<JComponent> changingComponents = getField(manager, List.class);
      changingComponents.add(component);

      new Expectations() {{ animationState.getStart(); result = componentState; }};

      manager.init(animator);
   }
}
