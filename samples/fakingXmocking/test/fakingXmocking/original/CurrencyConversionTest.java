/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.original;

import java.math.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import org.apache.http.*;
import org.apache.http.impl.client.*;

public final class CurrencyConversionTest
{
   @Mocked // mocking cascades to other reference types returned by methods of this type
   HttpClientBuilder httpClientBuilder;

   @Mocked
   HttpEntity httpEntity; // automatically used as a cascaded mock instance

   @After
   public void resetSUT()
   {
      CurrencyConversion.allCurrenciesCache = null;
   }

   @Test
   public void loadCurrencySymbolsFromWebSite() throws Exception
   {
      String content =
         "<table class='currencyTable'>\r\n" +
         "<td><a href=\"/currency/x\">USD</a></td><td class=\"x\">Dollar</td>\r\n" +
         "<td><a href=\"/currency/x\">EUR</a></td><td class=\"x\">Euro</td>";
      new Expectations() {{ httpEntity.getContent(); result = content; times = 1; }};

      Map<String, String> symbols = CurrencyConversion.currencySymbols();

      assertEquals(2, symbols.size());
      assertTrue(symbols.containsKey("USD"));
      assertTrue(symbols.containsKey("EUR"));
   }

   @Test
   public void convertFromOneCurrencyToAnother() throws Exception
   {
      CurrencyConversion.allCurrenciesCache = new HashMap<String, String>() {{ put("X", ""); put("Y", ""); }};

      String content = "<div id=\"converter_results\"><ul><li><b>1 X = 1.3 Y</b>";
      new Expectations() {{ httpEntity.getContent(); result = content; times = 1; }};

      BigDecimal rate = CurrencyConversion.convertFromTo("X", "Y");

      assertEquals("1.3", rate.toPlainString());
   }
}
