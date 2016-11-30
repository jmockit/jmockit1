/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class JUnit4Test
{
   @Mocked ClassWithObjectOverrides mock;

   @Test
   public void useMockedInstance()
   {
      assertFalse(mock.toString().isEmpty());
   }
}
