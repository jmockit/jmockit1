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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;

/**
 * This is the base class for all effects that are used during screen transitions.
 * <p/>
 * Subclasses of this base class may override the {@link #init(Animator, Effect) init()},
 * {@link #setup(Graphics2D) setup()}, and {@link #paint(Graphics2D) paint()} methods to achieve the desired effect.
 *
 * @author Chet Haase
 */
@SuppressWarnings({"ClassWithTooManyMethods", "AbstractClassWithoutAbstractMethods"})
public abstract class Effect
{
   /**
    * Information about the start state used by this effect.
    */
   private ComponentState start;

   /**
    * Information about the end state used by this effect.
    */
   private ComponentState end;

   /**
    * Flag to indicate whether effect needs to re-render Component.
    */
   private boolean renderComponent;

   /**
    * The image that will be used during the transition, for effects that opt to not re-render the
    * components directly.  The image will be set when the start and end states are set.
    */
   private Image componentImage;

   /**
    * Current width.
    */
   private int width;

   /**
    * Current height.
    */
   private int height;

   // The bounds and location fields are used as utility objects to be able to change the x/y and
   // width/height fields from single PropertySetter objects.
   private Rectangle bounds = new Rectangle();
   private final Point location = new Point();

   /**
    * Set the location and size of the component state being animated by this effect.
    */
   public final void setBounds(int x, int y, int width, int height)
   {
      bounds.x = location.x = x;
      bounds.y = location.y = y;
      setWidth(width);
      setHeight(height);
   }

   /**
    * Set the location and size of the component state being animated by this effect.
    */
   public final void setBounds(Rectangle bounds)
   {
      setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
   }

   /**
    * Set the location of the component state being animated by this effect.
    */
   public final void setLocation(Point location)
   {
      setX(location.x);
      setY(location.y);
   }

   /**
    * Set the x location of the component state being animated by this effect.
    */
   public final void setX(int x) { location.x = bounds.x = x; }

   /**
    * Set the y location of the component state being animated by this effect.
    */
   public final void setY(int y) { location.y = bounds.y = y; }

   /**
    * Set the width of the component state being animated by this effect.
    */
   public final void setWidth(int width) { bounds.width = this.width = width; }

   /**
    * Set the height of the component state being animated by this effect.
    */
   public final void setHeight(int height) { bounds.height = this.height = height; }

   /**
    * Get the component being animated by this effect.
    */
   protected final JComponent getComponent()
   {
      if (start != null) {
         return start.getComponent();
      }
      else if (end != null) {
         return end.getComponent();
      }

      assert false : "Should not get here";
      return null;
   }

   /**
    * Initialize this effect. This method is called at transition start time, to enable the effect
    * to set up any necessary state prior to the animation, such as animations that vary properties
    * of the Effect during the transition.
    * <p/>
    * Subclasses of <code>Effect</code> will typically call this superclass method if they override
    * <code>init()</code>, as many effects will depend on the state that is set up in this method.
    */
   public void init(Animator animator, Effect parentEffect)
   {
      bounds = new Rectangle();

      if (start != null) {
         setBounds(start.getX(), start.getY(), start.getWidth(), start.getHeight());
      }
      else {
         setBounds(end.getX(), end.getY(), end.getWidth(), end.getHeight());
      }

      // If this effect already has a snapshot image of the component, but it's not the size that we
      // need, flush it now and it will be created later during the first setup() call.
      if (
         componentImage != null &&
         (start != null && start.getWidth() != componentImage.getWidth(null) ||
          end != null && end.getWidth() != componentImage.getWidth(null))
      ) {
         componentImage.flush();
         componentImage = null;
      }
   }

   /**
    * Effect subclasses that create temporary objects for the transition (such as in the
    * <code>init()</code> method) should override this method and clean up those resources. For
    * example, TimingTarget e.g., PropertySetter) objects added to the animator used in the
    * transition should be removed afterwards to avoid leaking resources that may otherwise be
    * retained by those objects.
    */
   @SuppressWarnings("NoopMethodInAbstractClass")
   public void cleanup(Animator animator) {}

   /**
    * Tells the Effect to re-render the component during the transition instead of using an image
    * representation of the component. This is necessary for some animations which may change how a
    * component looks internally during the transition. For example, components that are being
    * scaled during a transition which contain text should probably be redrawn rather than simply
    * scaling an image, as scaling an image of the text does not generally look the same as text
    * rendered directly at a particular size.
    *
    * @param renderComponent whether the component should be re-rendered during the transition. If
    *                        <code>true</code>, then the component will be re-rendered during the
    *                        animation. If <code>false</code>, the system may choose to render an
    *                        image representation of the component instead.
    */
   public final void setRenderComponent(boolean renderComponent) { this.renderComponent = renderComponent; }

   /**
    * Returns whether the effect will re-render its component during transitions, as opposed to
    * using an image representation of it.
    *
    * @return whether the effect will re-render the component during the transition
    */
   public final boolean getRenderComponent() { return renderComponent; }

   /**
    * Sets both the start and end states of this Effect.
    */
   public final void setComponentStates(ComponentState start, ComponentState end)
   {
      this.start = start;
      this.end = end;
   }

   /**
    * Sets the start state of this Effect.
    */
   public void setStart(ComponentState start) { this.start = start; }

   /**
    * Gets the start state of this Effect.
    */
   public final ComponentState getStart() { return start; }

   /**
    * Sets the end state of this Effect.
    */
   public void setEnd(ComponentState end) { this.end = end; }

   /**
    * Gets the end state of this Effect.
    */
   public final ComponentState getEnd() { return end; }

   /**
    * Gets the image representation of this Effect. This is not intended to be called by
    * application code, but rather by custom effects or other parts of the system.
    */
   public final Image getComponentImage() { return componentImage; }

   /**
    * Sets the image representation of this Effect.
    */
   protected final void setComponentImage(Image componentImage) { this.componentImage = componentImage; }

   /**
    * Creates and renders an image representation of the component.
    */
   @SuppressWarnings("ConstantConditions")
   private void createComponentImage()
   {
      if (start != null && end == null) {
         componentImage = start.getSnapshot();
      }
      else if (start == null && end != null) {
         componentImage = end.getSnapshot();
      }
      else if (start.getWidth() != end.getWidth() || start.getHeight() != end.getHeight()) {
         // This block grabs the targetImage that best represents the component; the larger the better.
         float widthFraction = (float) end.getWidth() / start.getWidth();
         float heightFraction = (float) end.getHeight() / start.getHeight();

         if (Math.abs(widthFraction - 1.0f) > Math.abs(heightFraction - 1.0f)) {
            // difference greater in width
            if (widthFraction < 1.0f) {
               // start size larger then end size
               componentImage = start.getSnapshot();
            }
            else {
               componentImage = end.getSnapshot();
            }
         }
         else {
            // difference greater in height
            if (heightFraction < 1.0f) {
               // start size larger than end size
               componentImage = start.getSnapshot();
            }
            else {
               componentImage = end.getSnapshot();
            }
         }
      }
      else {
         componentImage = start.getSnapshot();
      }
   }

   /**
    * This method is called during each frame of the transition animation, prior to the call to
    * {@link #paint(Graphics2D) paint()}. Subclasses will implement this method to set up the
    * Graphic state, or other related state, that will be used in the ensuing call to the
    * <code>paint()</code> method.  Note that changes to the <code>Graphics2D</code> object here
    * will still be present in the <code>Graphics2D</code> object that is passed into the
    * <code>paint()</code> method, so this is a good time to set up things such as transform state
    * that should be active during the rendering calls.
    * <p/>
    * Subclasses that override this method should call this superclass method, because it may set
    * up state used later during rendering.
    *
    * @param g2d the Graphics2D destination for this rendering
    */
   public void setup(Graphics2D g2d)
   {
      if (!renderComponent && componentImage == null) {
         createComponentImage();
      }
   }

   /**
    * This method is called during each frame of the transition animation, after the call to
    * {@link #setup(Graphics2D) setup()}. Subclasses may override this method to perform whatever
    * rendering is necessary to paint the transitioning component into the <code>Graphics2D</code>
    * object with the desired effect.
    * <p/>
    * Most subclasses may elect to not override the method, since this version of the method already
    * handles the basic painting operation of a component.
    * Only subclasses that need facilities beyond the basic drawing of the component should consider
    * overriding.
    *
    * @param g2d The Graphics2D destination for this rendering.  Note that the state of this
    *            Graphics2D object is affected by the previous call to <code>setup()</code> so
    *            there may be no more need to perturb the graphics state further. Functionality in
    *            this method should focus, instead, on the rendering details instead of the
    *            graphics state.
    */
   public void paint(Graphics2D g2d)
   {
      if (!renderComponent && componentImage != null) {
         g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
         g2d.drawImage(componentImage, 0, 0, width, height, null);
      }
      else {
         getComponent().setBounds(bounds);
         getComponent().validate();
         ComponentState.paintSingleBuffered(getComponent(), g2d);
      }
   }

   /**
    * Called by EffectsManager on each effect during every frame of the transition, this method
    * calls setup() and paint().
    */
   final void render(Graphics2D g2d)
   {
      // First, translate to where we need to render.
      g2d.translate(location.x, location.y);

      // Now call setup and paint.  Splitting rendering into these two operations allows custom
      // effects to have multiple sub-effects combine their efforts into the Graphics2D object prior
      // to calling paint with that altered graphics object.
      setup(g2d);
      paint(g2d);
   }
}
