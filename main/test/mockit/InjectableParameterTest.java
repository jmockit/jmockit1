/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.math.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class InjectableParameterTest
{
   @Test
   public void injectableParametersOfWrapperTypes(
      @Injectable("123") Integer i, @Injectable("5") Long l, @Injectable("-45 ") Short s, @Injectable(" 127") Byte b,
      @Injectable("true") Boolean f1, @Injectable(" TRUE ") Boolean f2, @Injectable("A") Character c,
      @Injectable(" 1.23") Float f, @Injectable("-1.23") Double d)
   {
      assertEquals(123, (int) i);
      assertEquals(5L, (long) l);
      assertEquals(-45, (short) s);
      assertEquals(127, (byte) b);
      assertTrue(f1);
      assertTrue(f2);
      assertEquals('A', (char) c);
      assertEquals(1.23F, f, 0.0F);
      assertEquals(-1.23, d, 0.0);
   }

   @Test
   public void injectableParametersOfOtherNumberSubtypes(
      @Injectable(" 10.234") BigDecimal d, @Injectable("123 ") BigInteger i,
      @Injectable(" 4567 ") AtomicInteger ai, @Injectable(" 23434") AtomicLong al)
   {
      assertEquals(10.234, d.doubleValue(), 0.0);
      assertEquals(123, i.intValue());
      assertEquals(4567, ai.intValue());
      assertEquals(23434L, al.longValue());
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToDeclarePrimitiveInjectableWithoutAValue(@Injectable int i) {}

   @Test(expected = IllegalArgumentException.class)
   public void attemptToDeclareNonMockedInjectableWithoutAValue(@Injectable Integer i) {}
}
