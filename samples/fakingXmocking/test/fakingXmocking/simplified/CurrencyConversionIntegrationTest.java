/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.simplified;

import java.math.*;
import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * These tests are the same as those in {@link fakingXmocking.original.CurrencyConversionIntegrationTest}, except that
 * they exercise the refactored and modified version of the tested class, {@link CurrencyConversion}.
 * <p/>
 * To execute these tests without network access, add "<code>fakingXmocking.simplified.CurrencyConversionURLFake</code>"
 * in the "<code>jmockit-mocks</code>" system property.
 */
public final class CurrencyConversionIntegrationTest
{
   @Before @After
   public void emptiesTheLocalCache()
   {
      CurrencyConversion.allCurrenciesCache = null;
   }

   @Test
   public void loadAllCurrencySymbolsFromWebSite()
   {
      List<String> allSymbols = new CurrencyConversion().currencySymbols();

      assertTrue(allSymbols.containsAll(asList("USD", "EUR", "BRL", "CNY")));
      assertSame(allSymbols, CurrencyConversion.allCurrenciesCache);
   }

   @Test
   public void recoverCurrencySymbolsFromLocalCache()
   {
      List<String> cachedSymbols = asList("ABC", "XYZ");
      CurrencyConversion.allCurrenciesCache = cachedSymbols;
      CurrencyConversion.lastCacheRead = System.currentTimeMillis();

      List<String> allSymbols = new CurrencyConversion().currencySymbols();

      assertSame(cachedSymbols, allSymbols);
   }

   @Test
   public void loadCurrencySymbolsFromWebSiteUponDetectingExpiredCache()
   {
      CurrencyConversion.allCurrenciesCache = Collections.emptyList();
      CurrencyConversion.lastCacheRead = System.currentTimeMillis() - CurrencyConversion.CACHE_DURATION;

      List<String> allSymbols = new CurrencyConversion().currencySymbols();

      assertFalse(allSymbols.isEmpty());
      assertSame(allSymbols, CurrencyConversion.allCurrenciesCache);
      assertEquals(System.currentTimeMillis(), CurrencyConversion.lastCacheRead, 20);
   }

   @Test(expected = IllegalArgumentException.class)
   public void convertFromInvalidCurrency()
   {
      new CurrencyConversion().convertFromTo("invalid", "USD");
   }

   @Test(expected = IllegalArgumentException.class)
   public void convertToInvalidCurrency()
   {
      new CurrencyConversion().convertFromTo("EUR", "invalid");
   }

   @Test
   public void convertToSameCurrency()
   {
      String currency = "USD";

      BigDecimal identityRate = new CurrencyConversion().convertFromTo(currency, currency);

      assertEquals(BigDecimal.ONE, identityRate);
   }

   @Test
   public void convertFromDollarToCheapCurrency()
   {
      double rate = new CurrencyConversion().convertFromTo("USD", "CNY").doubleValue();

      assertTrue(rate > 1.0);
   }

   @Test
   public void convertFromOneCurrencyToAnotherAndBack()
   {
      String fromCurrency = "USD";
      String toCurrency = "EUR";

      BigDecimal rate = new CurrencyConversion().convertFromTo(fromCurrency, toCurrency);
      assertTrue(rate.doubleValue() > 0.0);

      BigDecimal inverseRate = new CurrencyConversion().convertFromTo(toCurrency, fromCurrency);
      assertTrue(inverseRate.doubleValue() > 0.0);

      assertEquals(1.0, rate.multiply(inverseRate).doubleValue(), 0.005);
   }
}
