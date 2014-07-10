/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import java.awt.event.*;
import javax.swing.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class ActionTriggerTest
{
   @Test
   public void addTriggerAsListenerToActionEventSource()
   {
      JButton button = new JButton("Test");

      ActionTrigger trigger = ActionTrigger.addTrigger(button, null);

      ActionListener addedListener = button.getActionListeners()[0];
      assertSame(trigger, addedListener);
   }

   @Test(expected = IllegalArgumentException.class)
   public void addTriggerFailsOnObjectWithoutAddActionListenerMethod()
   {
      ActionTrigger.addTrigger(new Object(), null);
   }

   @Test
   public void onActionPerformedTheTriggerGetsFired(@Mocked final Trigger base)
   {
      ActionTrigger actionTrigger = new ActionTrigger(null);

      actionTrigger.actionPerformed(null);

      new Verifications() {{ base.fire(); }};
   }
}
