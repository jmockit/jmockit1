/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;

import static mockit.Deencapsulation.*;

public final class ScreenTransitionContainerResizeTest
{
   JComponent container;
   ScreenTransition transition;
   ComponentListener containerSizeListener;
   @Injectable BufferedImage transitionImage;
   @Mocked AnimationManager animationManager;

   @Before
   public void setUp()
   {
      container = new JPanel();

      // ScreenTransition adds itself as listener to the container:
      transition = new ScreenTransition(container, null, 100);

      // Get the internal listener:
      containerSizeListener = container.getListeners(ComponentListener.class)[0];
   }

   @After
   public void tearDown()
   {
      new Verifications() {{ animationManager.recreateImage(); }};
   }

   @Test
   public void resizeTransitionContainerOnce()
   {
      simulateContainerResize();
   }

   // Simulate a resizing of the container, including the triggering of the "componentResized" event.
   void simulateContainerResize()
   {
      container.setSize(new Dimension(100, 80));
      containerSizeListener.componentResized(null);
   }

   @Test
   public void changeWidthOfTransitionContainerWhenAlreadyWithTransitionImage()
   {
      setField(transition, transitionImage);

      simulateContainerResize();
   }

   @Test
   public void changeHeightOfTransitionContainerWhenAlreadyWithTransitionImage()
   {
      setField(transition, transitionImage);
      new Expectations() {{ transitionImage.getWidth(); result = 100; }};

      simulateContainerResize();
   }
}
