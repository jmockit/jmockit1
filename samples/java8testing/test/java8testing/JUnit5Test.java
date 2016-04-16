/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import org.junit.gen5.api.*;
import static org.junit.gen5.api.Assertions.*;

final class JUnit5Test
{
   @Test
   void basicExample(TestInfo testInfo)
   {
      assertNotNull(testInfo);
   }
}
