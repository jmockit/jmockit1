/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.simplified;

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;

import static java.util.Arrays.*;

import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class CurrencyConversionTest
{
   @Tested @Mocked CurrencyConversion conversion;

   @Test
   public void loadCurrencySymbolsFromWebSite(@Mocked URL url) throws Exception
   {
      new Expectations() {{
         new URL(withMatch(".+iso4217.*")).openStream();
         result =
            "<table class='currencyTable'>\r\n" +
            "<a href='/currency/x'>USD</a>\r\n" +
            "<a href='/currency/x'>EUR</a>";
      }};

      List<String> allSymbols = conversion.currencySymbols();

      assertEquals(asList("USD", "EUR"), allSymbols);
   }

   @Test
   public void convertFromOneCurrencyToAnother() throws Exception
   {
      // Partial mocking of the tested object is acceptable here, since the mocked method
      // ("currencySymbols") is public and already exercised in a different test.
      new Expectations() {{
         conversion.currencySymbols();
         returns("X", "Y");
      }};

      new Expectations(URL.class) {{
         new URL(withMatch(".+&from=X&to=Y.*")).openStream();
         result = "<div id=\"converter_results\"><ul><li><b>1 X = 1.3 Y</b>";
      }};

      BigDecimal rate = conversion.convertFromTo("X", "Y");

      assertEquals("1.3", rate.toPlainString());
   }

   @Test
   public void useURLMockedUpAtStartup() throws Exception
   {
      URL url = new URL("http://www.xe.com");
      InputStream stream = url.openStream();
      assertNotNull(stream);
   }
}
