/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package end2end;

import java.awt.*;
import javax.swing.*;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.timing.Animator.*;
import org.jdesktop.animation.timing.interpolation.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class EndToEndTest
{
   static final int DURATION = Integer.parseInt(System.getProperty("duration", "5000"));
   static Component animatedComponent;
   static Dimension initialSize;
   static Point initialLocation;

   Animator animator;

   @BeforeClass
   public static void createUI() throws Exception
   {
      ImageIcon icon = new ImageIcon("../../www/javadoc/resources/logo.png");
      animatedComponent = new JButton("JMockit", icon);

      initialSize = new Dimension(icon.getIconWidth() + 90, icon.getIconHeight() + 10);
      initialLocation = new Point(150, 200);

      JFrame mainWindow = new JFrame("Timing Framework");
      mainWindow.setLayout(null);
      mainWindow.add(animatedComponent);
      mainWindow.setBounds(300, 200, 650, 500);
      mainWindow.setVisible(true);
   }

   @Before
   public void initializeSizeAndLocationOfAnimatedComponent()
   {
      animatedComponent.setSize(initialSize);
      animatedComponent.setLocation(initialLocation);
   }

   @Test
   public void defaultAnimationThatsMovesAComponentAlongSeveralPoints() throws Exception
   {
      animator = new Animator(DURATION);
      Point finalLocation = new Point(200, 320);
      animator.addTarget(
         new PropertySetter<Point>(
            animatedComponent, "location",
            new Point(10, 10), new Point(100, 60), new Point(450, 200), finalLocation));

      runAnimationUntilCompletion();

      assertEquals(finalLocation, animatedComponent.getLocation());
   }

   void runAnimationUntilCompletion()
   {
      animator.start();

      int totalDuration = (int) (animator.getRepeatCount() * animator.getDuration());
      try { Thread.sleep(totalDuration + 70); } catch (InterruptedException ignore) {}

      assertFalse(animator.isRunning());
   }

   @Test
   public void reversingAnimationThatPulsesAComponentSeveralTimes()
   {
      animator =
         PropertySetter.createAnimator(
            DURATION / 3, animatedComponent, "size",
            new Dimension(100, 80), new Dimension(150, 120), new Dimension(250, 200));

      animator.setEndBehavior(EndBehavior.RESET);
      animator.setInterpolator(new SplineInterpolator(0.2f, 0.3f, 0.8f, 0.6f));
      animator.setRepeatCount(3);

      runAnimationUntilCompletion();

      assertEquals(new Dimension(100, 80), animatedComponent.getSize());
   }
}
