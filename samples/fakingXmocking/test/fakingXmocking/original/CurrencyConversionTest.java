/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.original;

import java.math.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.*;
import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class CurrencyConversionTest
{
   @Capturing // so that any class implementing the base type gets mocked
   @Mocked
   HttpClient httpClient;

   @Mocked
   HttpEntity httpEntity; // provides access to the intermediate (cascaded) object

   @After
   public void resetSUT()
   {
      CurrencyConversion.allCurrenciesCache = null;
   }

   @Test
   public void loadCurrencySymbolsFromWebSite() throws Exception
   {
      new NonStrictExpectations() {{
         httpEntity.getContent(); times = 1;
         result =
            "<table class='currencyTable'>\r\n" +
            "<td><a href=\"/currency/x\">USD</a></td><td class=\"x\">Dollar</td>\r\n" +
            "<td><a href=\"/currency/x\">EUR</a></td><td class=\"x\">Euro</td>";
      }};

      Map<String, String> symbols = CurrencyConversion.currencySymbols();

      assertEquals(2, symbols.size());
      assertTrue(symbols.containsKey("USD"));
      assertTrue(symbols.containsKey("EUR"));
   }

   @Test
   public void convertFromOneCurrencyToAnother() throws Exception
   {
      CurrencyConversion.allCurrenciesCache = new HashMap<String, String>() {{ put("X", ""); put("Y", ""); }};

      new NonStrictExpectations() {{
         httpEntity.getContent(); times = 1;
         result = "<div id=\"converter_results\"><ul><li><b>1 X = 1.3 Y</b>";
      }};

      BigDecimal rate = CurrencyConversion.convertFromTo("X", "Y");

      assertEquals("1.3", rate.toPlainString());
   }
}
