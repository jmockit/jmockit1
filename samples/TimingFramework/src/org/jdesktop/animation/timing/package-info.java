/**
 * Core classes of the Timing Framework; these classes provide the base functionality that all
 * animations will use.
 * <p>
 * This package provides the fundamental capabilities of the Timing Framework. The core class of the
 * entire framework is {@link Animator}, which is responsible for setting up and running animations.
 * The other elements of this package are {@link TimingTarget}, which is the interface used by
 * {@link Animator} to report timing events during the animation, and {@link TimingTargetAdapter},
 * which is a utility class that users may subclass to pick and choose the {@link TimingTarget}
 * events they are interested in receiving.
 */
package org.jdesktop.animation.timing;

