/*
 * Copyright 2003-2009 OFFIS, Henri Tremblay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.easymock.samples;

import java.math.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.easymock.*;
import static org.easymock.EasyMock.*;

/**
 * Example of how to partial mock with actually calling a constructor.
 */
public final class ConstructorCalledMockTest extends EasyMockSupport
{
   TaxCalculator tc;

   @Before
   public void setUp()
   {
      BigDecimal[] taxValues = {new BigDecimal("5"), new BigDecimal("15")};

      // No need to mock any methods, abstract ones are mocked by default:
      tc = createMockBuilder(TaxCalculator.class).withConstructor((Object) taxValues).createMock();
   }

   @After
   public void tearDown()
   {
      verifyAll();
   }

   @Test
   public void testTax()
   {
      expect(tc.rate()).andStubReturn(new BigDecimal("0.20"));
      replayAll();

      BigDecimal tax = tc.tax();

      assertEquals(new BigDecimal("4.00"), tax);
   }

   @Test
   public void testTax_ZeroRate()
   {
      expect(tc.rate()).andStubReturn(BigDecimal.ZERO);
      replayAll();

      BigDecimal tax = tc.tax();

      assertEquals(BigDecimal.ZERO, tax);
   }
}
