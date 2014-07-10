/*
 * Copyright (c) 2007, Sun Microsystems, Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following 
 *     disclaimer in the documentation and/or other materials provided 
 *     with the distribution.
 *   * Neither the name of the TimingFramework project nor the names of its
 *     contributors may be used to endorse or promote products derived 
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.animation.timing.triggers;

/**
 * Mouse Enter/Exit/Press/Release/Click events.
 *
 * @author Chet
 * @author Rogerio
 */
public enum MouseTriggerEvent implements TriggerEvent
{
   /**
    * Event fired when mouse enters.
    */
   ENTER,

   /**
    * Event fired when mouse exits.
    */
   EXIT,

   /**
    * Event fired when mouse button is pressed.
    */
   PRESS,

   /**
    * Event fired when mouse button is released.
    */
   RELEASE,

   /**
    * Event fired when mouse is clicked.
    */
   CLICK;

   /**
    * Finds the opposite of the current event: <BR/>
    * ENTER -> EXIT <BR/>
    * EXIT -> ENTER <BR/>
    * PRESS -> RELEASE <BR/>
    * RELEASE -> PRESS <BR/>
    * Note that CLICK has no obvious opposite so it simply returns CLICK (this method should probably not be called for
    * that case).
    */
   @Override
   public TriggerEvent getOppositeEvent()
   {
      if (this == ENTER) {
         return EXIT;
      }
      else if (this == EXIT) {
         return ENTER;
      }
      else if (this == PRESS) {
         return RELEASE;
      }
      else if (this == RELEASE) {
         return PRESS;
      }

      // Possible to reach here for REPEAT action (but probably should not have been called with this event).
      return this;
   }
}
