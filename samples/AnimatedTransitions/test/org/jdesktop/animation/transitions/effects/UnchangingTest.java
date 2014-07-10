/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import org.jdesktop.animation.transitions.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class UnchangingTest
{
   @Test
   public void createUnchangingEffectWithGivenStates(@Injectable ComponentState start, @Injectable ComponentState end)
   {
      Effect created = new Unchanging(start, end);

      assertSame(start, created.getStart());
      assertSame(end, created.getEnd());
   }
}
