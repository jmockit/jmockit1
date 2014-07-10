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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

import org.jdesktop.animation.transitions.Effect;

/**
 * Effect that performs a Fade (in or out) on the component.
 * This is done by using an image of the component and altering the translucency (or <code>AlphaComposite</code>) of
 * the <code>Graphics2D</code> object according to how far along the transition animation is.
 * <p/>
 * This is an abstract class that relies on the FadeIn or FadeOut subclasses to set up the end (FadeIn) or start
 * (FadeOut) states appropriately.
 *
 * @author Chet Haase
 */
public abstract class Fade extends Effect
{
   // Property used to set the degree of opacity of the effect. This property is used later in
   // setup() to create an appropriate AlphaComposite object.
   private float opacity;

   /**
    * This method is called by an animation set up by subclasses to vary the opacity during the transition.
    */
   public void setOpacity(float opacity)
   {
      this.opacity = opacity;
   }

   /**
    * This method is called prior to <code>paint()</code> during every frame of the transition animation.
    * It sets up an <code>AlphaComposite</code> object based on the current opacity and sets that composite on the
    * <code>Graphics2D</code> object appropriately.
    */
   @Override
   public void setup(Graphics2D g2d)
   {
      AlphaComposite newComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
      g2d.setComposite(newComposite);
      super.setup(g2d);
   }
}
