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

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.animation.transitions.ComponentState;
import org.jdesktop.animation.transitions.Effect;

/**
 * Effect that resizes a component during the transition.
 *
 * @author Chet Haase
 */
public final class Scale extends Effect
{
   // Two property setters to animate both the width and height of the component. Note that the
   // actual width/height properties are in Effect itself; we are merely setting up an animation
   // here to vary those existing properties.
   private PropertySetter<Integer> psWidth;
   private PropertySetter<Integer> psHeight;

   public Scale()
   {
      // Scaling effect, by default, will re-render Component every time.
      setRenderComponent(true);
   }

   /**
    * Creates a new instance of Scale with start and end states for the component.
    */
   public Scale(ComponentState start, ComponentState end)
   {
      // Call default constructor to initialize important state.
      this();
      setComponentStates(start, end);
   }

   /**
    * Initializes the effect, adding animation targets that will scale the component of the effect
    * from the start to the end sizes during the course of the transition.
    */
   @Override
   public void init(Animator animator, Effect parentEffect)
   {
      Effect targetEffect = parentEffect == null ? this : parentEffect;
      psWidth = new PropertySetter<>(targetEffect, "width", getStart().getWidth(), getEnd().getWidth());
      animator.addTarget(psWidth);
      psHeight = new PropertySetter<>(targetEffect, "height", getStart().getHeight(), getEnd().getHeight());
      animator.addTarget(psHeight);
      super.init(animator, null);
   }

   /**
    * Removes the scaling targets from the animation to avoid leaking resources.
    */
   @Override
   public void cleanup(Animator animator)
   {
      animator.removeTarget(psWidth);
      animator.removeTarget(psHeight);
   }
}
