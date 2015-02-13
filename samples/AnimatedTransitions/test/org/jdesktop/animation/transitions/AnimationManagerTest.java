/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.*;
import java.awt.image.*;
import java.util.List;
import java.util.*;
import javax.swing.*;

import static mockit.Deencapsulation.*;
import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

@SuppressWarnings("unchecked")
public final class AnimationManagerTest
{
   @Test
   public void recreateImageForContainerOfSizeZero(@Injectable final JComponent container)
   {
      // recreateImage() is called by the constructor.
      new AnimationManager(container);

      new Verifications() {{ container.createImage(anyInt, anyInt); times = 0; }};
   }

   @Test
   public void recreateImageForContainerOfSizeNotZeroAndBackgroundStillUndefined(
      @Injectable final JComponent container, @Injectable BufferedImage backgroundImage)
   {
      new Expectations() {{
         container.getWidth(); result = 100;
         container.getHeight(); result = 80;
         container.createImage(100, 80); times = 1;
      }};

      // recreateImage() is called by the constructor.
      new AnimationManager(container);
   }

   @Test
   public void recreateImageForContainerOfSizeNotZeroAndBackgroundAlreadyDefined(
      @Injectable final JComponent container, @Injectable final BufferedImage transitionImageBG)
   {
      AnimationManager manager = new AnimationManager(container);
      setField(manager, transitionImageBG);
      final int cw = 100;
      final int ch = 80;

      new Expectations() {{
         container.getWidth(); result = cw;
         container.getHeight(); result = ch;
      }};

      new Expectations() {{ transitionImageBG.getWidth(); result = cw + 1; }};

      manager.recreateImage();

      new Verifications() {{ container.createImage(cw, ch); }};
   }

   @Test
   public void resetWhenEmpty(@Injectable JComponent container, @Mocked AnimationState animationState)
   {
      new AnimationManager(container).reset(null);

      new FullVerifications(AnimationState.class) {};
   }

   @Test
   public void resetWhenNotEmpty(@Injectable JComponent container, @Mocked final AnimationState animationState)
   {
      AnimationManager manager = new AnimationManager(container);
      manager.addStart(new JButton());

      manager.reset(null);

      new Verifications() {{ animationState.cleanup(null); }};
   }

   @Test
   public void addInitialStartStateForComponent(@Mocked AnimationState animationState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithEmptyContainer();

      manager.addStart(component);

      new Verifications() {{ new AnimationState(component, true); }};
   }

   AnimationManager newAnimationManagerWithEmptyContainer()
   {
      JComponent container = new JPanel();
      return new AnimationManager(container);
   }

   @Test
   public void addSecondStartStateForComponent(@Mocked final AnimationState animationState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithEmptyContainer();

      manager.addStart(component);
      manager.addStart(component);

      new Verifications() {{ animationState.setStart(new ComponentState(component)); }};
   }

   @Test
   public void addInitialEndStateForComponent(@Mocked AnimationState animationState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithEmptyContainer();

      manager.addEnd(component);

      new Verifications() {{ new AnimationState(component, false); }};
   }

   @Test
   public void addSecondEndStateForComponent(@Mocked final AnimationState animationState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithEmptyContainer();

      manager.addEnd(component);
      manager.addEnd(component);

      new Verifications() {{ animationState.setEnd(new ComponentState(component)); }};
   }

   @Test
   public void setupStart()
   {
      final JButton component = new JButton();
      final AnimationManager manager = newAnimationManagerWithAComponent(component);

      new Expectations(manager) {{ manager.addStart(component); times = 1; }};

      manager.setupStart();
   }

   AnimationManager newAnimationManagerWithAComponent(JComponent component)
   {
      JComponent container = new JPanel();
      container.add(component);
      component.setVisible(true);
      return new AnimationManager(container);
   }

   @Test
   public void setupEndForComponentWithoutStartState(
      @Mocked AnimationState animationState, @Mocked ComponentState componentState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithAComponent(component);

      manager.setupEnd();

      final Map<JComponent, AnimationState> componentAnimationStates = getField(manager, Map.class);

      new Verifications() {{
         ComponentState createdComponentState = new ComponentState(component);

         List<AnimationState> createdAnimationStates = withCapture(new AnimationState(createdComponentState, false));
         AnimationState addedAnimationState = componentAnimationStates.get(component);
         assertTrue(createdAnimationStates.contains(addedAnimationState));
      }};

      List<JComponent> changingComponents = getField(manager, List.class);
      assertTrue(changingComponents.contains(component));
   }

   @Test
   public void setupEndForComponentWithSameStartAndEndStates(@Mocked final AnimationState startState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithAComponent(component);

      Map<JComponent, AnimationState> componentAnimationStates = getField(manager, Map.class);
      componentAnimationStates.put(component, startState);

      new Expectations() {{
         startState.getStart(); result = new ComponentState(component);
      }};

      manager.setupEnd();

      assertTrue(componentAnimationStates.isEmpty());
   }

   @Test
   public void setupEndForComponentWithDifferentStartAndEndStates(@Mocked final AnimationState animationState)
   {
      final JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithAComponent(component);

      Map<JComponent, AnimationState> componentAnimationStates = getField(manager, Map.class);
      componentAnimationStates.put(component, animationState);

      new Expectations() {{
         animationState.getStart(); result = new ComponentState(new JLabel());
      }};

      manager.setupEnd();

      new Verifications() {{ animationState.setEnd(new ComponentState(component)); }};

      assertEquals(1, componentAnimationStates.size());

      List<JComponent> changingComponents = getField(manager, List.class);
      assertTrue(changingComponents.contains(component));
   }

   @Test
   public void paint(@Injectable final Graphics g, @Mocked final AnimationState animationState)
   {
      JButton component = new JButton();
      AnimationManager manager = newAnimationManagerWithAComponent(component);
      manager.addStart(component);

      manager.paint(g);

      new Verifications() {{
         g.drawImage(null, 0, 0, null);
         animationState.paint(g);
      }};
   }
}
