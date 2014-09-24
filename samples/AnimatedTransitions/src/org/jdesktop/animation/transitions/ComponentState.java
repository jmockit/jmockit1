/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.animation.transitions;

import java.awt.*;
import javax.swing.*;

/**
 * This class stores the state of a component that will be used during the transition. The state
 * includes the position, the size, and an image snapshot of the component.
 *
 * @author Chet Haase
 */
public class ComponentState
{
   /**
    * The component.
    */
   private final JComponent component;

   /**
    * The x location of the component.
    */
   private final int x;

   /**
    * The y location of the component.
    */
   private final int y;

   /**
    * The width of the component.
    */
   private final int width;

   /**
    * The height of the component.
    */
   private final int height;

   /**
    * The image snapshot of the component in this state; this may be used later by effects which
    * use images to render the transitioning component.
    */
   private Image componentSnapshot;

   /**
    * The constructor takes a component and derives the state information needed (location, size,
    * and image snapshot).
    *
    * @param component the JComponent associated with this ComponentState
    */
   public ComponentState(JComponent component)
   {
      this.component = component;
      x = component.getX();
      y = component.getY();
      width = component.getWidth();
      height = component.getHeight();
      componentSnapshot = createSnapshot();
   }

   /**
    * Create an image snapshot of the component in its current state.  This may be used in an
    * Effect to render the transitioning component with an image.
    */
   private Image createSnapshot()
   {
      GraphicsConfiguration gc = component.getGraphicsConfiguration();

      if (gc == null) { // component may have null gc, get default
         gc = GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().getDefaultConfiguration();
      }

      if (width != 0 && height != 0) {
         Image snapshot =
            gc.createCompatibleImage(
               width, height,
               component.isOpaque() ? Transparency.OPAQUE : Transparency.TRANSLUCENT);
         Graphics2D gImg = (Graphics2D) snapshot.getGraphics();
         paintSingleBuffered(component, gImg);
         gImg.dispose();
         return snapshot;
      }
      else {
         // Component with zero width or height cannot produce an image, and one will not be needed
         // anyway since it is not visible in this state.
         return null;
      }
   }

   public int getX()
   {
      return x;
   }

   public int getY()
   {
      return y;
   }

   public int getWidth()
   {
      return width;
   }

   public int getHeight()
   {
      return height;
   }

   public JComponent getComponent()
   {
      return component;
   }

   /**
    * Gets the image representation of the component for this state. The image will be created if
    * it does not exist already.
    */
   public Image getSnapshot()
   {
      if (componentSnapshot == null) {
         componentSnapshot = createSnapshot();
      }

      return componentSnapshot;
   }

   /*
    * The remaining methods exist solely to support the static paintSingleBuffered() method.
    */

   /**
    * Paints the given JComponent in single-buffered mode, which is needed to avoid rendering
    * artifacts when capturing a non-opaque Swing component hierarchy into an offscreen image.
    *
    * @param component the JComponent (and its children) to be painted
    * @param g         the Graphics into which component will be painted
    */
   public static void paintSingleBuffered(JComponent component, Graphics g)
   {
      // The print() method in JComponent effectively does what we want; it paints the component to
      // the given Graphics object without sending the rendering through the Swing back buffer.
      component.print(g);
   }

   /**
    * Paints the component in single-buffered mode, including whatever is behind it. This approach
    * handles the case where the component is not opaque, which is useful for taking a snapshot of
    * the transition container background, for example.
    */
   public static void paintHierarchySingleBuffered(JComponent component, Graphics g)
   {
      // Walk the parent hierarchy to get the topmost JComponent parent.
      // Calculate the relative XY location of the original component as we go.
      int x = 0;
      int y = 0;
      int w = component.getWidth();
      int h = component.getHeight();
      JComponent topmost = component;
      JComponent prevTopmost = component;

      // We can stop when the current container is opaque or the top level JComponent (probably
      // redundant checks; a non-opaque contentPane would cause artifacts).
      while (
         !topmost.isOpaque() &&
         topmost.getParent() != null && topmost.getParent() instanceof JComponent
      ) {
         topmost = (JComponent) topmost.getParent();
         x += prevTopmost.getX();
         y += prevTopmost.getY();
         prevTopmost = topmost;
      }

      // Only want to paint the area of the original component.
      g.setClip(0, 0, w, h);
      g.translate(-x, -y);
      topmost.print(g);
   }

   /**
    * Override of {@code Object#equals(Object)} that returns {@code true} if either it is the
    * same object exactly or the objects contain the same position, size, and component information.
    * This method is used, for example, to detect whether some start state is equal to some other
    * end state, which helps in determining whether a component is changing during the course of a
    * transition.
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj) {
         return true;
      }

      if (obj instanceof ComponentState) {
         ComponentState other = (ComponentState) obj;

         if (
            x == other.x && y == other.y && width == other.width && height == other.height &&
            component == other.component) {
            return true;
         }
      }

      return false;
   }

   /**
    * When overriding {@link #equals(Object)}, it is also necessary to override
    * <code>hashCode()</code> appropriately.
    */
   @Override
   public int hashCode()
   {
      int result = 17;
      result = 37 * result + x;
      result = 37 * result + y;
      result = 37 * result + width;
      result = 37 * result + height;
      result = 37 * result + component.hashCode();
      return result;
   }

   /**
    * Utility method (useful in debugging) that returns the salient data of this
    * <code>ComponentState</code>.
    */
   @Override
   public String toString()
   {
      return
         "ComponentState: x, y, w, h, component = " +
         x + ", " + y + ", " + width + ", " + height + ", " + component;
   }
}
