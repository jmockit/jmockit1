/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.easymock.samples;

import java.math.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class ConstructorCalledMock_JMockit_Test
{
   @Tested TaxCalculator tc;
   @Injectable BigDecimal[] values = {new BigDecimal("5"), new BigDecimal("15")};

   @Test
   public void testTax()
   {
      new NonStrictExpectations(tc) {{ tc.rate(); result = "0.20"; }};

      assertEquals(new BigDecimal("4.00"), tc.tax());
   }

   @Test
   public void testTax_ZeroRate()
   {
      new NonStrictExpectations(tc) {{ tc.rate(); result = 0; }};

      assertEquals(BigDecimal.ZERO, tc.tax());
   }
}
