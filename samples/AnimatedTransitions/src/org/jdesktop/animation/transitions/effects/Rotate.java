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

package org.jdesktop.animation.transitions.effects;

import java.awt.Graphics2D;
import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.animation.transitions.ComponentState;
import org.jdesktop.animation.transitions.Effect;

/**
 * This Effect rotates a component through a given number of degrees during the animated transition.
 *
 * @author Chet Haase
 */
public final class Rotate extends Effect
{
   // The x coordinate of the center location of the component.
   private final int xCenter;

   // The y coordinate of the center location of the component.
   private final int yCenter;

   // The total number of radians to sweep through during the transition.
   private final double endRadians;

   // The property animated during the transition.
   private double radians;

   // The animation target used to animate the radians property.
   private PropertySetter<Double> ps;

   /**
    * Constructs a Rotate effect with the number of degrees to rotate through during the transition.
    * This constructor will result in an effect that rotates around the center of the given
    * component.
    *
    * @param degrees   The number of degrees to rotate through during the transition
    * @param component Used to determine the center of rotation. The rotation effect will be
    *                  created to rotate around the center of the component.
    */
   public Rotate(int degrees, JComponent component)
   {
      this(degrees, component.getWidth() / 2, component.getHeight() / 2);
   }

   /**
    * Constructs a Rotate effect with the number of degrees to rotate through during the transition.
    * This constructor will result in an effect that rotates around the point
    * <code>(xCenter, yCenter)</code>.
    *
    * @param degrees The number of degrees to rotate through during the transition
    * @param xCenter The x coordinate of the center of rotation
    * @param yCenter The y coordinate of the center of rotation
    */
   public Rotate(int degrees, int xCenter, int yCenter)
   {
      endRadians = Math.toRadians(degrees);
      this.xCenter = xCenter;
      this.yCenter = yCenter;
   }

   /**
    * Constructs a Rotate effect for a given component with the number of degrees you wish to
    * rotate through during the transition and the center of rotation to use.
    *
    * @param start   The start state of the component
    * @param end     The end state of the component
    * @param degrees The number of degrees to rotate through during the transition
    * @param xCenter The x coordinate of the center of rotation
    * @param yCenter The y coordinate of the center of rotation
    */
   public Rotate(ComponentState start, ComponentState end, int degrees, int xCenter, int yCenter)
   {
      this(degrees, xCenter, yCenter);
      setComponentStates(start, end);
   }

   /**
    * Initializes the effect, adding an animation target that will rotate the component of the
    * effect from the start to the end state during the course of the transition.
    */
   @Override
   public void init(Animator animator, Effect parentEffect)
   {
      ps = new PropertySetter<>(this, "radians", 0.0, endRadians);
      animator.addTarget(ps);
      super.init(animator, null);
   }

   /**
    * Removes the rotation target from the animation to avoid leaking resources.
    */
   @Override
   public void cleanup(Animator animator)
   {
      animator.removeTarget(ps);
   }

   /**
    * This property setting method is called during the transition by the animation target that
    * this effect sets up. It varies the value of the effect's <code>radians</code> property, which
    * is used to determine the angle at which to display the component.
    */
   public void setRadians(double radians)
   {
      this.radians = radians;
   }

   /**
    * Called during the painting process of each frame of the transition animation.
    * This method alters the transform property of <code>g2d</code> to reflect the current animated rotation value.
    *
    * @param g2d The graphics object that will have its transform property altered to rotate
    *            rendering according to the current rotation property of this effect.
    */
   @Override
   public void setup(Graphics2D g2d)
   {
      // Translate back and forth to rotate around the right point.
      g2d.translate(xCenter, yCenter);
      g2d.rotate(radians);
      g2d.translate(-xCenter, -yCenter);
      super.setup(g2d);
   }
}
