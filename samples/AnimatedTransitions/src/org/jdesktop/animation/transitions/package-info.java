/**
 * Core classes of the Animated Transitions library; these classes provide the base functionality
 * that all transitions will use.
 * <p>
 * This package provides the fundamental capabilities of Animated Transitions. The core class of the
 * entire framework is {@link ScreenTransition}, which is responsible for setting up and running
 * transitions.  Also, the {@link TransitionTarget} interface is necessary for applications to
 * implement in order to provide a callback mechanism that {@link ScreenTransition} requires.
 * The other classes in this package are optional, depending on whether applications need to
 * implement or use custom effects.
 */
package org.jdesktop.animation.transitions;
