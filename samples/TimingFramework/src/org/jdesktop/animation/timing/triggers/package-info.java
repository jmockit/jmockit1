/**
 * Provides simple mechanism for starting Animators when specific events occur.
 * <p>
 * This package provides classes for both using and subclassing that simplify the process of
 * associating animations with events.
 * {@link Trigger} and its subclasses associate specific events (subclasses of {@link TriggerEvent})
 * with listeners (as defined in {@link Trigger} subclasses).
 * These listeners are then added (by the application) to appropriate objects.
 * The animations are started when a Trigger detects that a specified event has occurred (through
 * its listener).
 */
package org.jdesktop.animation.timing.triggers;
