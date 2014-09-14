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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;

/**
 * This class manages the animated rendering of the various components during the transitions.  For
 * each component in the start and end screens of the transitioning component, there is a call to
 * <code>addStart</code> or <code>addEnd</code>.  Information for the start and end states are
 * stored in individual <code>AnimationState</code> objects on a per-component basis.
 * <p/>
 * During the transition, animation timing events trigger calls to {@link #paint(Graphics) paint()},
 * which asks each of the <code>AnimationState</code> structures to render themselves in their
 * current, animating state.
 *
 * @author Chet Haase
 */
final class AnimationManager
{
   // The set of start/end states for each component in the transition.
   private final Map<JComponent, AnimationState> componentAnimationStates = new HashMap<>();

   // The set of components that change between their start and end states.
   private final List<JComponent> changingComponents = new ArrayList<>();

   // The container in which the transition will occur.
   private final JComponent container;

   /**
    * Background that will be copied into the transitionImage on every frame.  This represents the
    * default (empty) state of the containerLayer; copying this into the transitionImage is like
    * erasing to the background of the real container in the application.
    */
   private BufferedImage transitionImageBG;

   AnimationManager(JComponent container)
   {
      this.container = container;
      recreateImage();
   }

   /**
    * Causes background image to be recreated if the container is not of size (0,0) and if the
    * current image is either null or of a different size than the current container.
    */
   void recreateImage()
   {
      int cw = container.getWidth();
      int ch = container.getHeight();

      if (cw > 0 && ch > 0 &&
         (transitionImageBG == null ||
          cw != transitionImageBG.getWidth() || ch != transitionImageBG.getHeight())
      ) {
         transitionImageBG = (BufferedImage) container.createImage(cw, ch);
      }
   }

   /**
    * Utility method, used to check whether the given component has a state set already.
    */
   private AnimationState getExistingAnimationState(Component component)
   {
      return componentAnimationStates.get(component);
   }

   /**
    * Reset the AnimationStates; this clears out the old structure of states after we are done with
    * a transition.
    */
   void reset(Animator animator)
   {
      for (AnimationState state : componentAnimationStates.values()) {
         state.cleanup(animator);
      }

      componentAnimationStates.clear();
      changingComponents.clear();
   }

   /**
    * Initialize the animation. This sets up all of the individual animations based on default or
    * custom effects for each component.  This method also creates the background image for the
    * transition, which includes the background of the transition container, but also any
    * components that do not change between their start and end states (we render them once into
    * the background image and skip rendering them each per-frame).
    */
   void init(Animator animator)
   {
      // Create the background image for the transition if necessary.
      recreateImage();

      // First, make sure that we don't run animations for components that aren't even visible.
      List<Component> componentsToRemove = new ArrayList<>();

      for (AnimationState state : componentAnimationStates.values()) {
         ComponentState start = state.getStart();
         ComponentState end = state.getEnd();
         Rectangle bounds = null;

         if (start != null) {
            bounds = new Rectangle(start.getX(), start.getY(), start.getWidth(), start.getHeight());
         }

         if (end != null) {
            Rectangle boundsEnd =
               new Rectangle(end.getX(), end.getY(), end.getWidth(), end.getHeight());

            if (bounds == null) {
               bounds = boundsEnd;
            }
            else {
               bounds = bounds.union(boundsEnd);
            }
         }

         assert bounds != null;
         Rectangle componentBounds = container.getBounds();

         if (!bounds.intersects(componentBounds)) {
            componentsToRemove.add(state.getComponent());
         }
      }

      for (Component component : componentsToRemove) {
         componentAnimationStates.remove(component);
         changingComponents.remove(component);
      }

      // Don't paint the changing components into the background image.
      for (JComponent child : changingComponents) {
         child.setVisible(false);
      }

      // Paint the background image for the transition. This will include the background of the
      // container itself, but also any components that do not change between the screens.
      Graphics gImg = transitionImageBG.getGraphics();
      gImg.clearRect(0, 0, transitionImageBG.getWidth(), transitionImageBG.getHeight());
      ComponentState.paintHierarchySingleBuffered(container, gImg);
      gImg.dispose();

      // Reset visibility of changing components, now that we're done painting the background.
      for (JComponent child : changingComponents) {
         child.setVisible(true);
      }

      // Initialize the animation states that we're going to use.
      for (AnimationState state : componentAnimationStates.values()) {
         state.init(animator);
      }
   }

   /**
    * Save the start state for all components in this container.
    */
   void setupStart()
   {
      for (Component child : container.getComponents()) {
         if (child.isVisible() && child instanceof JComponent) {
            addStart((JComponent) child);
         }
      }
   }

   /**
    * Save the end state for all components in this container. Any components in the container that
    * are in the same state at start and end will be removed from the list of components that need
    * to be animated and will, instead, be rendered to the background image.
    */
   void setupEnd()
   {
      for (Component child : container.getComponents()) {
         if (child.isVisible() && child instanceof JComponent) {
            ComponentState end = new ComponentState((JComponent) child);
            AnimationState animState = getExistingAnimationState(child);

            if (animState != null) {
               ComponentState start = animState.getStart();

               if (start != null && start.equals(end)) {
                  componentAnimationStates.remove(child);
               }
               else {
                  changingComponents.add((JComponent) child);
                  animState.setEnd(end);
               }
            }
            else {
               changingComponents.add((JComponent) child);
               AnimationState endState = new AnimationState(end, false);
               componentAnimationStates.put((JComponent) child, endState);
            }
         }
      }
   }

   /**
    * Add a start state for the given component.
    *
    * @param component The individual component to be animated
    */
   void addStart(JComponent component)
   {
      AnimationState existingAnimState = getExistingAnimationState(component);

      if (existingAnimState != null) {
         // Already have an end state, add this start state to existing structure.
         existingAnimState.setStart(new ComponentState(component));
      }
      else {
         AnimationState animState = new AnimationState(component, true);
         componentAnimationStates.put(component, animState);
      }
   }

   /**
    * Add an end state for the given component.
    *
    * @param component The individual component to be animated
    */
   void addEnd(JComponent component)
   {
      AnimationState existingAnimState = getExistingAnimationState(component);

      if (existingAnimState != null) {
         // Already have a start state, add this end state to existing structure.
         existingAnimState.setEnd(new ComponentState(component));
      }
      else {
         AnimationState animState = new AnimationState(component, false);
         componentAnimationStates.put(component, animState);
      }
   }

   /**
    * This method is called during the transition animation. Iterate through the various
    * <code>AnimationState</code> objects asking each one to paint itself into the
    * <code>Graphics</code>.
    *
    * @param g The <code>Graphics</code> object that the animating objects need to render
    *          themselves into.
    */
   void paint(Graphics g)
   {
      g.drawImage(transitionImageBG, 0, 0, null);

      for (AnimationState state : componentAnimationStates.values()) {
         state.paint(g);
      }
   }
}
