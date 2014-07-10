/**
 * Provides a mechanism for animating object properties between different values.
 * <p>
 * This package provides classes for defining object properties to animate, via the
 * {@link PropertySetter} class.  {@link KeyFrames} encapsulates the definition of the times (using
 * {@link KeyTimes}) and values (using {@link KeyValues}) to interpolate between, as well as the
 * type of interpolation to use between these values.  {@link Interpolator} is an interface that is
 * implemented by {@link DiscreteInterpolator}, {@link LinearInterpolator}, and
 * {@link SplineInterpolator} for built-in interpolations, but applications can define their own
 * custom interpolation as well.
 */
package org.jdesktop.animation.timing.interpolation;
