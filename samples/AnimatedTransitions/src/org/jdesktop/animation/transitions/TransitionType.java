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

import java.util.*;
import javax.swing.*;

/**
 * The type of transition that this effect should be used for.
 * <p>
 * This enum class manages the cache of effects for the application.  Users wishing to have specific
 * effects occur on particular components should cache those effects here.  These effects will be
 * retrieved at transition time, based on which components are transitioning and which
 * transition type is needed.
 *
 * @author Chet Haase
 * @author Rogério Liesenfeld
 */
public enum TransitionType
{
   /**
    * Applies to components that exist in both the start and end states of the transition.
    */
   CHANGING,

   /**
    * Applies to components that do not exist in the start state, but exist in the end state.
    */
   APPEARING,

   /**
    * Applies to components that do not exist in the end state, but exist in the start state.
    */
   DISAPPEARING;

   private final Map<JComponent, Effect> cachedEffects = new HashMap<>();

   /**
    * This method is used to cache a custom effect on a per-component basis for the application.
    * Note that these custom effects are application wide for the duration of the process, or until
    * a new or null effect is set for this component.  Note also that custom effects are registered
    * according to the transition type. So a custom <code>TransitionType.CHANGING</code> effect for
    * a given component will have no bearing on the effect used in a transition where the component
    * either appears or disappers between the transition states.
    *
    * @param component The JComponent that this effect should be applied to.
    * @param effect The custom effect desired.  A null argument effectively cancels any prior
    *               custom value for this component and this transition type; it is equivalent to
    *               calling {@link #removeEffect(javax.swing.JComponent)}.
    */
   public void setEffect(JComponent component, Effect effect)
   {
      if (effect == null) {
         removeEffect(component);
         return;
      }

      cachedEffects.put(component, effect);
   }

   /**
    * This method is called during the setup phase for any transition.  It queries this transition
    * type's cache for a custom effect associated with a given component.
    *
    * @param component The component we are querying on behalf of.
    * @return The custom effect associated with this component. A null return value indicates that
    *         there is no custom effect associated with this component and transition type.
    */
   public Effect getEffect(JComponent component) { return cachedEffects.get(component); }

   /**
    * Removes an effect for the specified component in the cache for this transition type, if an
    * effect exists.
    *
    * @param component The component associated with the effect that should be removed.
    */
   public void removeEffect(JComponent component) { cachedEffects.remove(component); }

   /**
    * Clears all effects for this transition type.
    */
   public void clearEffects() { cachedEffects.clear(); }

   /**
    * Clears the cache for each transition type of all custom effects currently set.
    */
   public static void clearAllEffects()
   {
      for (TransitionType transitionType : values()) {
         transitionType.cachedEffects.clear();
      }
   }
}
