/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class MouseTriggerTest
{
   @Test
   public void addTrigger()
   {
      Component button = new JButton("Test");

      MouseTrigger trigger = MouseTrigger.addTrigger(button, null, MouseTriggerEvent.ENTER);

      MouseListener[] mouseListeners = button.getMouseListeners();
      MouseListener listenerAdded = mouseListeners[mouseListeners.length - 1];
      assertSame(trigger, listenerAdded);
   }

   @Test
   public void addTriggerWithAutoReverse()
   {
      Component button = new JButton("Test");

      MouseTrigger trigger = MouseTrigger.addTrigger(button, null, MouseTriggerEvent.ENTER, true);

      MouseListener[] mouseListeners = button.getMouseListeners();
      MouseListener listenerAdded = mouseListeners[mouseListeners.length - 1];
      assertSame(trigger, listenerAdded);
   }

   @Mocked Trigger base;

   @Test
   public void mouseEntered()
   {
      MouseListener trigger = new MouseTrigger(null, MouseTriggerEvent.ENTER);

      trigger.mouseEntered(null);

      new MouseTriggerVerifications(MouseTriggerEvent.ENTER);
   }

   final class MouseTriggerVerifications
   {
      MouseTriggerVerifications(final MouseTriggerEvent event) {{ base.fire(event); }}
   }

   @Test
   public void mouseExited()
   {
      MouseListener trigger = new MouseTrigger(null, MouseTriggerEvent.EXIT);

      trigger.mouseExited(null);

      new MouseTriggerVerifications(MouseTriggerEvent.EXIT);
   }

   @Test
   public void mousePressed()
   {
      MouseListener trigger = new MouseTrigger(null, MouseTriggerEvent.PRESS);

      trigger.mousePressed(null);

      new MouseTriggerVerifications(MouseTriggerEvent.PRESS);
   }

   @Test
   public void mouseReleased()
   {
      MouseListener trigger = new MouseTrigger(null, MouseTriggerEvent.RELEASE);

      trigger.mouseReleased(null);

      new MouseTriggerVerifications(MouseTriggerEvent.RELEASE);
   }

   @Test
   public void mouseClicked()
   {
      MouseListener trigger = new MouseTrigger(null, MouseTriggerEvent.CLICK);

      trigger.mouseClicked(null);

      new MouseTriggerVerifications(MouseTriggerEvent.CLICK);
   }
}
