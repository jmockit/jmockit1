/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.simplified;

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * A cleaned-up version, with a more object-oriented API and internal design, but the same functionality.
 */
public final class CurrencyConversion
{
   private static final Pattern LINE_WITH_SYMBOL = Pattern.compile("href=./currency/.+>(...)</a>");

   static final int CACHE_DURATION = 5 * 60 * 1000;
   static List<String> allCurrenciesCache;
   static long lastCacheRead = Long.MAX_VALUE;

   public List<String> currencySymbols()
   {
      if (allCurrenciesCache != null && System.currentTimeMillis() - lastCacheRead < CACHE_DURATION) {
         return allCurrenciesCache;
      }

      InputStream response = readHtmlPageFromWebSite("http://www.xe.com/iso4217.php");
      List<String> result = extractCurrencySymbolsFromHtml(response);

      allCurrenciesCache = result;
      lastCacheRead = System.currentTimeMillis();
      return result;
   }

   private InputStream readHtmlPageFromWebSite(String url)
   {
      try {
         return new URL(url).openStream();
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private List<String> extractCurrencySymbolsFromHtml(InputStream response)
   {
      BufferedReader reader = new BufferedReader(new InputStreamReader(response));
      List<String> symbols = new ArrayList<>();

      try {
         String line;
         boolean foundTable = false;

         while ((line = reader.readLine()) != null) {
            if (foundTable) {
               Matcher matcher = LINE_WITH_SYMBOL.matcher(line);

               if (matcher.find()) {
                  symbols.add(matcher.group(1));
               }
            }

            if (line.contains("currencyTable")) {
               foundTable = true;
            }
         }
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }

      return symbols;
   }

   public BigDecimal convertFromTo(String fromCurrency, String toCurrency)
   {
      validateSymbol("from", fromCurrency);
      validateSymbol("to", toCurrency);

      String url =
         "http://www.gocurrency.com/v2/dorate.php?inV=1&Calculate=Convert&from=" +
         fromCurrency + "&to=" + toCurrency;
      InputStream response = readHtmlPageFromWebSite(url);

      String value = extractCalculatedConversionRateFromHtmlResult(response);

      return new BigDecimal(value);
   }

   private void validateSymbol(String whichOne, String currencySymbol)
   {
      if (!currencySymbols().contains(currencySymbol)) {
         throw new IllegalArgumentException("Invalid " + whichOne + " currency: " + currencySymbol);
      }
   }

   private String extractCalculatedConversionRateFromHtmlResult(InputStream htmlFormattedConversionResult)
   {
      Scanner s = new Scanner(htmlFormattedConversionResult).skip("(?s).*<div id=\"converter_results\">");
      String innermostHtml = s.findWithinHorizon("<b>.+</b>", 0);

      String[] parts = innermostHtml.split("\\s*=\\s*");
      return parts[1].split(" ")[0];
   }
}
