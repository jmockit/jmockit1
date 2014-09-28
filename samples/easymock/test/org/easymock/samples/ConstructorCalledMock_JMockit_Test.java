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
      new Expectations() {{ tc.rate(); result = "0.20"; }};

      BigDecimal tax = tc.tax();

      assertEquals(new BigDecimal("4.00"), tax);
   }

   @Test
   public void testTax_ZeroRate()
   {
      new Expectations() {{ tc.rate(); result = 0; }};

      BigDecimal tax = tc.tax();

      assertEquals(BigDecimal.ZERO, tax);
   }
}
