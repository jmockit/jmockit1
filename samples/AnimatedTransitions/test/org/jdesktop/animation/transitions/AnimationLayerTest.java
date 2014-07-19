/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.*;
import javax.swing.*;

import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class AnimationLayerTest
{
   @Test
   public void setupLocationInGlassPaneOfTargetComponentWhereToDrawAnimation()
   {
      AnimationLayer animationLayer = new AnimationLayer(null);

      JFrame window = new JFrame();
      window.setBounds(20, 10, 200, 150);
      JComponent targetComponent = new JButton("Testing");
      targetComponent.setLocation(15, 23);
      window.add(targetComponent);

      animationLayer.setupBackground(targetComponent);

      Point componentLocation = Deencapsulation.getField(animationLayer, Point.class);
      assertEquals(new Point(15, 23), componentLocation);
   }

   @Test
   public void drawsCurrentTransitionImageWhenPaintingComponent(
      @Mocked final ScreenTransition screenTransition, @Mocked final Graphics graphics)
   {
      AnimationLayer animationLayer = new AnimationLayer(screenTransition);

      animationLayer.paintComponent(graphics);

      new Verifications() {{
         graphics.drawImage(screenTransition.getTransitionImage(), 0, 0, null);
      }};
   }
}
