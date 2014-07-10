/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
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
import static org.junit.Assert.*;

public final class ScreenTransitionContainerResizeTest
{
   JComponent container;
   Dimension newSize;
   @Mocked BufferedImage transitionImage;
   ScreenTransition transition;

   @Before
   public void setUp()
   {
      container = new JPanel();
      newSize = new Dimension(100, 80);
   }

   @Test
   public void resizeTransitionContainerOnce()
   {
      new CreationOfTransitionImageExpectations();

      createTransition();

      simulateContainerResizeThenAssertCreationOfTransitionImage();
   }

   final class CreationOfTransitionImageExpectations extends NonStrictExpectations
   {
      CreationOfTransitionImageExpectations()
      {
         super(container);

         container.createImage(newSize.width, newSize.height);
         minTimes = 1;
         result = transitionImage;
      }
   }

   void createTransition()
   {
      // Creates ScreenTransition, which adds itself as listener to the container:
      transition = new ScreenTransition(container, null, 100);
   }

   void simulateContainerResizeThenAssertCreationOfTransitionImage()
   {
      // Get the internal listener:
      ComponentListener containerSizeListener = container.getListeners(ComponentListener.class)[0];

      // Simulate a resizing of the container, including the event that would be triggered.
      container.setSize(newSize);
      containerSizeListener.componentResized(null);

      assertSame(transitionImage, transition.getTransitionImage());
   }

   @Test
   public void changeWidthOfTransitionContainerAlreadyWithTransitionImage()
   {
      createTransition();
      setField(transition, transitionImage);

      new CreationOfTransitionImageExpectations();

      simulateContainerResizeThenAssertCreationOfTransitionImage();
   }

   @Test
   public void changeHeightOfTransitionContainerAlreadyWithTransitionImage()
   {
      createTransition();
      setField(transition, transitionImage);

      new NonStrictExpectations() {{ transitionImage.getWidth(); result = newSize.width; }};
      new CreationOfTransitionImageExpectations();

      simulateContainerResizeThenAssertCreationOfTransitionImage();
   }
}
