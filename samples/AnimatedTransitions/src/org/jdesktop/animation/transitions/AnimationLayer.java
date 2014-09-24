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

import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * This is the component where the transition animation is displayed. During a transition, this
 * layer becomes visible as the GlassPane of the application window that contains the transition
 * container. Repaints of this component happen during the transition, which become paintComponent()
 * events here. The paintComponent method simply copies the current transition image (into which the
 * current frame of the transition animation was rendered) to its component Graphics and Swing
 * copies it onto window.
 *
 * @author Chet Haase
 */
final class AnimationLayer extends JComponent
{
   // Need to keep track of where the transition container lives in the window so that we copy the
   // transition image to the proper place in the window-wide GlassPane.
   private Point componentLocation = new Point();

   // We call into ScreenTransition to get the current transition image which holds each frame's
   // rendering during the transition.
   private final ScreenTransition screenTransition;

   /**
    * Construct the AnimationLayer with a reference to the ScreenTransition object, which will be
    * used later at paintComponent() time.
    */
   AnimationLayer(ScreenTransition screenTransition)
   {
      setOpaque(false);
      this.screenTransition = screenTransition;
   }

   /**
    * Called from ScreenTransition to set up the correct location to copy the animation to in the
    * glass pane.
    */
   public void setupBackground(JComponent targetComponent)
   {
      componentLocation.setLocation(0, 0);
      componentLocation =
         SwingUtilities.convertPoint(
            targetComponent, componentLocation, targetComponent.getRootPane().getGlassPane());
   }

   /**
    * Called during the Swing repaint process for this component. This simply copies the
    * transitionImage from ScreenTransition into the appropriate location in the glass pane.
    */
   @Override
   public void paintComponent(Graphics g)
   {
      g.translate(componentLocation.x, componentLocation.y);
      g.drawImage(screenTransition.getTransitionImage(), 0, 0, null);
   }
}
