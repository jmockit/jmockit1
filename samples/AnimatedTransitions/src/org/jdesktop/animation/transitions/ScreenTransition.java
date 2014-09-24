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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;

import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.jdesktop.animation.timing.interpolation.PropertySetter;

/**
 * This class is used to run animated transitions in an application.
 * <p/>
 * {@code ScreenTransition} is given a container in a Swing application.
 * When the application wishes to transition from one state of the application to another, the {@link #start()} method
 * is called, which calls back into the application to set up the next state of the GUI.
 * Then {@code ScreenTransition} runs an animation from the previous state of the application to the new state.
 *
 * @author Chet Haase
 */
public final class ScreenTransition
{
   /*
    * Implementation detail: The key to making ScreenTransition work correctly is having two
    * different views or layers of ScreenTransition under the covers.  One layer is the
    * "containerLayer", which is where the actual child components of ScreenTransition are placed.
    * The other (more hidden) layer is the "animationLayer", which exists solely for displaying the
    * animations which take place during any transition.
    *
    * The reason we cannot animate the transitions in the same container where the actual components
    * live (at least not trivially) is that we need to layout the container for both states of a
    * transition prior to actually running the animation.  Moving the components around during these
    * layout phases cannot happen onscreen (the net effect would be that the user would see the end
    * result before the transition), so we do the layout on an invisible container instead.  Then
    * the animation happens to transition between the two states, in his separate animationLayer.
    *
    * Further detail: the "animationLayer" is set to be the glass pane of the application frame.
    * Glass pane already has the functionality we need (specifically, it overlays the area that we
    * need to render during the transition); the only trick is that we must position the rendering
    * correctly since the glass pane typically covers the entire application frame, but in this case
    * the transition container may only occupy a portion of the frame.
    */

   /**
    * Handles the structure and rendering of the actual animation during the transitions.
    */
   private final AnimationManager animationManager;

   /**
    * The component where the transition animation occurs.  This component (which is set to be the
    * glass pane) is visible during the transition, but is otherwise invisible.
    */
   private final AnimationLayer animationLayer;

   /**
    * The component supplied at construction time that holds the actual components added to
    * ScreenTransition by the application. Keeping this container separate from ScreenTransition
    * allows us to render the AnimationLayer during the transitions and separate the animation
    * sequence from the actual container of the components.
    */
   private final JComponent containerLayer;

   /**
    * Image used to store the current state of the transition animation.  This image will be
    * rendered to during timingEvent() and then copied into the glass pane during the repaint
    * cycle.
    */
   private BufferedImage transitionImage;

   /**
    * The user-defined code which ScreenTransition will call to setup the next state of the GUI when
    * a transition is started.
    */
   private final TransitionTarget transitionTarget;

   /**
    * If the application has already set their own custom glass pane, we save that component here
    * before using the glass pane for our own purposes. We then restore the original glass pane when
    * the animation has completed.
    */
   private Component savedGlassPane;

   /**
    * Animation engine for the transition.
    */
   private Animator animator;

   /**
    * Constructor for ScreenTransition.  The application must supply the JComponent that they wish
    * to transition and the TransitionTarget which supplies the callback methods called during the
    * transition process.
    *
    * @param transitionComponent JComponent in which the application wishes to run the transition.
    * @param transitionTarget    Implementation of {@code TransitionTarget} interface which
    *                            will be called during transition process.
    */
   private ScreenTransition(JComponent transitionComponent, TransitionTarget transitionTarget)
   {
      containerLayer = transitionComponent;
      this.transitionTarget = transitionTarget;

      animationManager = new AnimationManager(transitionComponent);
      animationLayer = new AnimationLayer(this);
      animationLayer.setVisible(false);

      // Hack: pre-warm PropertySetter just to get some static initializers out of the way that will
      // be needed before the first transition can start.
      new PropertySetter<>(transitionComponent, "location", new Point(0, 0));

      containerLayer.addComponentListener(new ContainerSizeListener());
      createTransitionImages();
   }

   /**
    * Constructor that takes an Animator that will be used to drive the ScreenTransition. Transition
    * will start if either {@link #start} is called or {@link Animator#start} is called.
    *
    * @param transitionComponent JComponent in which the application wishes to run the transition.
    * @param transitionTarget    Implementation of {@link TransitionTarget} interface which will be
    *                            called during transition process.
    * @param animator            the animator that defines the characteristics of the transition
    *                            animation, such as its duration
    * @throws IllegalStateException if animator is already running
    * @throws IllegalArgumentException animator must be non-null
    * @see Animator#isRunning()
    * @see Animator#start()
    */
   public ScreenTransition(JComponent transitionComponent, TransitionTarget transitionTarget, Animator animator)
   {
      this(transitionComponent, transitionTarget);
      setAnimator(animator);
   }

   /**
    * Constructor that takes a simple duration.  The Animator used to drive this ScreenTransition
    * will be created internally.
    *
    * @param transitionComponent JComponent in which the application wishes to run the transition.
    * @param transitionTarget    Implementation of {@link TransitionTarget} interface which will be
    *                            called during transition process.
    * @param duration            the length of time in milliseconds that the transition will last
    */
   public ScreenTransition(JComponent transitionComponent, TransitionTarget transitionTarget, int duration)
   {
      this(transitionComponent, transitionTarget);
      setAnimator(new Animator(duration, transitionTimingTarget));
   }

   /**
    * Create the transition images here and in AnimationManager if necessary.
    */
   private void createTransitionImages()
   {
      int cw = containerLayer.getWidth();
      int ch = containerLayer.getHeight();

      if (cw > 0 && ch > 0 &&
         (transitionImage == null ||
          transitionImage.getWidth() != cw || transitionImage.getHeight() != ch)
      ) {
         // Recreate transition image and background for new dimensions.
         transitionImage = (BufferedImage) containerLayer.createImage(cw, ch);
         animationManager.recreateImage();
      }
   }

   /**
    * Listen for changes to the transition container size and recreate transition images as necessary.
    * Doing this on component size change events prevents having to do it as needed at the start of the next
    * transition, which can cause a unwanted delay in that animation.
    */
   private final class ContainerSizeListener extends ComponentAdapter
   {
      @Override
      public void componentResized(ComponentEvent ce)
      {
         createTransitionImages();
      }
   }

   /**
    * Returns {@code Animator} object that drives this ScreenTransition.
    *
    * @return the Animator that drives this ScreenTransition
    */
   public Animator getAnimator()
   {
      return animator;
   }

   /**
    * Sets animator that drives this ScreenTransition. Animator cannot be null. Animator also cannot
    * be running when this method is called (because important setup information for
    * ScreenTransition happens at Animator start time).  Transition will start if either {@link
    * #start} is called or {@link Animator#start} is called.
    *
    * @param animator non-null Animator object that will drive this ScreenTransition.  Animator
    *                 cannot be running when this is called.
    * @throws IllegalStateException if animator is already running
    * @throws IllegalArgumentException animator must be non-null
    * @see Animator#isRunning()
    */
   public void setAnimator(Animator animator)
   {
      if (animator == null) {
         throw new IllegalArgumentException("Animator must be non-null");
      }

      if (animator.isRunning()) {
         throw new IllegalStateException("Cannot perform this operation while animator is running");
      }

      this.animator = animator;
      animator.addTarget(transitionTimingTarget);
   }

   /**
    * Returns image used during timingEvent rendering.  This is called by AnimationLayer to get the
    * contents for the glass pane.
    */
   Image getTransitionImage()
   {
      return transitionImage;
   }

   /**
    * Begin the transition from the current application state to the next one.
    * This method will start the transition's {@link Animator} which will cause the transition to begin.
    * This will result in a call into the {@link TransitionTarget} specified in the {@code ScreenTransition}
    * constructor: {@code setupNextScreen()} will be called to allow the application to set up the state of the next
    * screen.
    * Then the transition animation will begin.
    */
   public void start()
   {
      if (animator.isRunning()) {
         animator.stop();
      }

      animator.start();
   }

   /**
    * This class receives the timing events from the animator and performs the appropriate
    * operations on the ScreenTransition object. This could be done by having ScreenTransition
    * implement TimingTarget methods directly, but there is no need to expose those methods as
    * public API (which would be necessary since TimingTarget needs the methods to be public).
    * Having this as an internal private class hides the methods from the ScreenTransition API while
    * allowing the same functionality.
    */
   private final TimingTarget transitionTimingTarget = new TimingTargetAdapter()
   {
      /**
       * This method is called as a result of a call to {@link ScreenTransition#start()}. The method
       * sets up appropriate state for the transition, creating any necessary background images,
       * capturing the current state of the components in the transition container, calling the
       * application's setupNextScreen() method, and capturing the new state of the components.
       * It then determines the effects to use during the transition, based on the changes taking
       * place in the components between the two screens and initializes those effects
       * appropriately.
       */
      @Override
      public void begin()
      {
         // Make sure that our background images exist and is the right size.
         createTransitionImages();

         // Capture the current state of the application into the AnimationManager;
         // this sets up the state we are transitioning from.
         animationManager.setupStart();

         // This records data in animationLayer used to copy the transition contents correctly into
         // the glass pane.
         animationLayer.setupBackground(containerLayer);

         // Make the animationLayer visible and the contentPane invisible.  This frees us to
         // validate the application state for the next screen while keeping that new state
         // invisible from the user; the animationLayer will only display contents appropriate to
         // the transition (the previous state before the transition begins, the transitioning state
         // during the transition).
         savedGlassPane = containerLayer.getRootPane().getGlassPane();
         containerLayer.getRootPane().setGlassPane(animationLayer);
         animationLayer.setVisible(true);

         // Now that the contentPane is invisible to the user, have the application setup the next
         // screen.  This will define the end state of the application for this transition.
         transitionTarget.setupNextScreen();

         // Validating the container layer component ensures correct layout for the next screen of
         // the application.
         containerLayer.validate();

         // Iterate through the visible components in the next application screen and add those end
         // states to the AnimationManager.
         animationManager.setupEnd();

         // Initialize the AnimationManager; this sets up default or custom effects for each of
         // the components involved in the transition.
         animationManager.init(animator);

         // Now that we have recorded (and rendered) the initial state, make the container invisible
         // for the duration of the transition.
         containerLayer.setVisible(false);

         // Workaround: need glass pane to reflect initial contents when we exit this function to
         // avoid flash of blank container.
         timingEvent(0);
      }

      /**
       * Implementation of the {@code TimingTarget} interface.
       * This method is called repeatedly during the transition animation.
       * We force a repaint, which causes the current transition state to be rendered.
       */
      @Override
      public void timingEvent(float fraction)
      {
         Graphics2D gImg = (Graphics2D) transitionImage.getGraphics();

         // Render this frame of the animation.
         animationManager.paint(gImg);

         gImg.dispose();

         // Force transitionImage to be copied to the glass pane.
         animationLayer.repaint();
      }

      /**
       * Override of {@code TimingTarget.end()}; switch the visibility of the containerLayer
       * and animationLayer and force repaint.
       */
      @Override
      public void end()
      {
         containerLayer.getRootPane().setGlassPane(savedGlassPane);
         savedGlassPane.setVisible(false);
         animationLayer.setVisible(false);
         containerLayer.setVisible(true);
         containerLayer.repaint();

         // Reset the AnimationManager (this releases all previous transition data structures).
         animationManager.reset(animator);
      }
   };
}
