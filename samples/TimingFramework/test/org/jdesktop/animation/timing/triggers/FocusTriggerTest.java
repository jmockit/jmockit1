/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class FocusTriggerTest
{
   @Test
   public void addTrigger()
   {
      Component button = new JButton("Test");

      FocusTrigger trigger = FocusTrigger.addTrigger(button, null, FocusTriggerEvent.IN);

      FocusListener[] focusListeners = button.getFocusListeners();
      FocusListener lastListenerAdded = focusListeners[focusListeners.length - 1];
      assertSame(trigger, lastListenerAdded);
   }

   @Test
   public void addTriggerWithAutoReverse()
   {
      Component label = new JLabel();

      FocusTrigger trigger = FocusTrigger.addTrigger(label, null, FocusTriggerEvent.IN, true);

      FocusListener listenerAdded = label.getFocusListeners()[0];
      assertSame(trigger, listenerAdded);
   }

   @Test
   public void focusGained(@Mocked final Trigger base)
   {
      FocusTrigger focusTrigger = new FocusTrigger(null, FocusTriggerEvent.IN);

      focusTrigger.focusGained(null);

      new Verifications() {{ base.fire(FocusTriggerEvent.IN); }};
   }

   @Test
   public void focusLost(@Mocked final Trigger base)
   {
      FocusTrigger focusTrigger = new FocusTrigger(null, FocusTriggerEvent.OUT);

      focusTrigger.focusLost(null);

      new Verifications() {{ base.fire(FocusTriggerEvent.OUT); }};
   }
}
