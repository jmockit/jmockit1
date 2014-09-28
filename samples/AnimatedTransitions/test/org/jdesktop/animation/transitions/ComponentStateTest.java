/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import java.awt.*;
import javax.swing.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class ComponentStateTest
{
   final JComponent component = new JButton();

   @Test
   public void createComponentStateForComponentOfZeroSize()
   {
      ComponentState state = new ComponentState(component);

      assertComponentState(state);
      assertNull(state.getSnapshot());
   }

   void assertComponentState(ComponentState state)
   {
      assertSame(component, state.getComponent());
      assertEquals(component.getX(), state.getX());
      assertEquals(component.getY(), state.getY());
      assertEquals(component.getWidth(), state.getWidth());
      assertEquals(component.getHeight(), state.getHeight());
   }

   @Test
   public void createComponentStateForSizedComponent()
   {
      component.setSize(80, 60);

      new Expectations(component) {{
         component.print(withInstanceOf(Graphics.class)); times = 1;
      }};

      ComponentState state = new ComponentState(component);

      assertComponentState(state);
      Image snapshotImg = state.getSnapshot();
      assertNotNull(snapshotImg);
      assertEquals(component.getWidth(), snapshotImg.getWidth(null));
      assertEquals(component.getHeight(), snapshotImg.getHeight(null));
   }

   @Test
   public void paintHierarchySingleBuffered(@Mocked final Graphics graphics)
   {
      JComponent container = new JPanel();
      container.setOpaque(true);
      container.setBounds(10, 5, 120, 90);

      component.setOpaque(false);
      component.setBounds(15, 12, 80, 60);
      container.add(component);

      new Expectations(JComponent.class) {{
         component.print(graphics); times = 1;
      }};

      ComponentState.paintHierarchySingleBuffered(component, graphics);

      new Verifications() {{
         graphics.setClip(0, 0, 80, 60);
         graphics.translate(-15, -12);
      }};
   }

   @Test
   public void componentStateEquals()
   {
      ComponentState state1 = new ComponentState(component);
      ComponentState state2 = new ComponentState(component);

      //noinspection ObjectEqualsNull
      assertFalse(state1.equals(null));
      assertEquals(state1, state1);
      assertEquals(state1, state2);
   }

   @Test
   public void componentStateHashCode()
   {
      ComponentState state1 = new ComponentState(component);
      ComponentState state2 = new ComponentState(component);

      assertEquals(state1.hashCode(), state2.hashCode());
   }

   @Test
   public void componentStateToString()
   {
      ComponentState state1 = new ComponentState(component);
      ComponentState state2 = new ComponentState(component);
      
      assertEquals(state1.toString(), state2.toString());
   }
}
