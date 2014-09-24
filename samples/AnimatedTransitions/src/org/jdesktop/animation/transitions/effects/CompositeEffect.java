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
import java.util.ArrayList;
import java.util.List;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.transitions.ComponentState;
import org.jdesktop.animation.transitions.Effect;

/**
 * This Effect combines one or more sub-effects to create a more complex and interesting effect.
 * For example, you could create an effect that both moves and scales by creating a CompositeEffect with
 * the Move and Scale effects.
 * <p/>
 * Composite effects are created by simply adding effects in the order that you want them combined.
 *
 * @author Chet Haase
 */
public final class CompositeEffect extends Effect
{
   /**
    * The list of effects in the CompositeEffect.
    */
   private final List<Effect> effects = new ArrayList<>();

   /**
    * Creates a CompositeEffect with no sub-effects.  Additional sub-effects should be added via the
    * <code>addEffect</code> method.
    */
   public CompositeEffect() {}

   /**
    * Creates a CompositeEffect with the given effect as the first sub-effect.  Additional
    * sub-effects should be added via the <code>addEffect</code> method.
    */
   public CompositeEffect(Effect effect)
   {
      addEffect(effect);
   }

   /**
    * Adds an additional effect to this CompositeEffect.  This effect is added to the end of the
    * existing list of effects, and will be processed after the other effects have been processed.
    */
   public void addEffect(Effect effect)
   {
      effects.add(effect);

      if (effect.getRenderComponent()) {
         setRenderComponent(true);
      }

      if (getStart() == null) {
         setStart(effect.getStart());
      }

      if (getEnd() == null) {
         setEnd(effect.getEnd());
      }
   }

   /**
    * This method is called during the initialization process of a transition and allows the effects
    * to set up the start state for each effect.
    */
   @Override
   public void setStart(ComponentState start)
   {
      for (Effect effect : effects) {
         effect.setStart(start);
      }

      super.setStart(start);
   }

   /**
    * Initializes all child effects at the start of a transition.
    */
   @Override
   public void init(Animator animator, Effect parentEffect)
   {
      for (Effect effect : effects) {
         effect.init(animator, this);
      }

      super.init(animator, null);
   }

   /**
    * Calls <code>cleanup()</code> on all child effects.
    */
   @Override
   public void cleanup(Animator animator)
   {
      for (Effect effect : effects) {
         effect.cleanup(animator);
      }
   }

   /**
    * This method is called during the initialization process of a transition and allows the effects
    * to set up the end state for each effect.
    */
   @Override
   public void setEnd(ComponentState end)
   {
      for (Effect effect : effects) {
         effect.setEnd(end);
      }

      super.setEnd(end);
   }

   /**
    * This method is called during each frame of the transition animation and allows the effect to
    * set up the Graphics state according to the various sub-effects in this CompositeEffect.
    */
   @Override
   public void setup(Graphics2D g2d)
   {
      boolean hasNoComponentImageYet = !getRenderComponent() && getComponentImage() == null;

      for (Effect effect : effects) {
         effect.setup(g2d);

         // Grab the image for this effect from one of the sub-effects.
         // REMIND: This breaks down if we have a CompositeEffect composed of several
         // non-image-based effects.
         if (hasNoComponentImageYet) {
            setComponentImage(effect.getComponentImage());
            hasNoComponentImageYet = false;
         }
      }

      super.setup(g2d);
   }
}
