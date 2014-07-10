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

/**
 * Class to test and partially mock.
 */
public abstract class TaxCalculator
{
   private final BigDecimal[] values;

   protected TaxCalculator(BigDecimal... values)
   {
      this.values = values;
   }

   protected abstract BigDecimal rate();

   public final BigDecimal tax()
   {
      BigDecimal result = BigDecimal.ZERO;

      for (BigDecimal d : values) {
         result = result.add(d);
      }

      return result.multiply(rate());
   }
}
