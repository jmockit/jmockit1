/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public final class JUnit4ParametersTest
{
   @Parameterized.Parameters
   public static List<Integer[]> parameters()
   {
      Integer[][] data = {{1, 1}, {2, 4}, {3, 9}};
      return Arrays.asList(data);
   }

   final int input;
   final int expected;

   public JUnit4ParametersTest(int input, int expected)
   {
      this.input = input;
      this.expected = expected;
   }

   @Test
   public void useParameters()
   {
      int result = input * input;
      assertEquals(expected, result);
   }
}
