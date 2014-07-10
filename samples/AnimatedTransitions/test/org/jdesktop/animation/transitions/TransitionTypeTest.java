/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import javax.swing.*;

import org.junit.*;

import org.jdesktop.animation.transitions.effects.*;
import static org.junit.Assert.*;

public final class TransitionTypeTest
{
   final JComponent component = new JComponent() {};
   final Effect effect = new Unchanging();

   @Test
   public void setEffectAndGetEffect()
   {
      for (TransitionType transitionType : TransitionType.values()) {
         transitionType.setEffect(component, effect);
         assertSame(effect, transitionType.getEffect(component));
      }
   }

   @Test
   public void setEffectWithNull()
   {
      TransitionType transitionType = TransitionType.APPEARING;
      transitionType.setEffect(component, effect);

      transitionType.setEffect(component, null);

      assertNull(transitionType.getEffect(component));
   }

   @Test
   public void removeEffect()
   {
      for (TransitionType transitionType : TransitionType.values()) {
         transitionType.setEffect(component, effect);
         transitionType.removeEffect(component);
         assertNull(transitionType.getEffect(component));
      }
   }

   @Test
   public void clearEffects()
   {
      for (TransitionType transitionType : TransitionType.values()) {
         transitionType.setEffect(component, effect);
         transitionType.clearEffects();
         assertNull(transitionType.getEffect(component));
      }
   }

   @Test
   public void clearAllEffects()
   {
      for (TransitionType transitionType : TransitionType.values()) {
         transitionType.setEffect(component, effect);
      }

      TransitionType.clearAllEffects();

      for (TransitionType transitionType : TransitionType.values()) {
         assertNull(transitionType.getEffect(component));
      }
   }
}
