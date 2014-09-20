/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.original;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;

/**
 * A version of the "CurrencyConversion" class, minimally modified so that it can be tested by conventional
 * mocking tools such as Mockito.
 * <p/>
 * Specifically, a single seam was added, with only two lines of original code modified and two lines added.
 * The seam takes the form of a package-private field which holds a factory object for HttpClient objects.
 * By replacing the factory implementation, a mock HttpClient can be returned instead of a real one.
 */
public final class CurrencyConversion_testable
{
   static final int CACHE_DURATION = 5 * 60 * 1000;
   static Map<String, String> allCurrenciesCache;
   static long lastCacheRead = Long.MAX_VALUE;

   private CurrencyConversion_testable() {}

   public static Map<String, String> currencySymbols()
   {
      if (allCurrenciesCache != null && System.currentTimeMillis() - lastCacheRead < CACHE_DURATION) {
         return allCurrenciesCache;
      }

      Map<String, String> symbolToName = new ConcurrentHashMap<>();

      try {
         HttpClient httpClient = httpClientFactory.get();
         HttpGet httpGet = new HttpGet("http://www.xe.com/iso4217.php");
         HttpResponse response = httpClient.execute(httpGet);
         HttpEntity entity = response.getEntity();

         if (entity != null) {
            InputStream inStream = entity.getContent();
            readSymbolsFromHTMLPage(symbolToName, inStream);
         }
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }

      allCurrenciesCache = symbolToName;
      lastCacheRead = System.currentTimeMillis();

      return symbolToName;
   }

   private static void readSymbolsFromHTMLPage(Map<String, String> symbolToName, InputStream inStream)
      throws IOException
   {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(inStream))) {
         boolean foundTable = false;
         String l;

         while ((l = br.readLine()) != null) {
            if (foundTable) {
               Pattern symbol =
                  Pattern.compile("href=\"/currency/[^>]+>(...)</a></td><td class=\"[^\"]+\">([A-Za-z ]+)");
               Matcher m = symbol.matcher(l);

               while (m.find()) {
                  symbolToName.put(m.group(1), m.group(2));
               }
            }

            if (l.contains("currencyTable")) {
               foundTable = true;
            }
         }
      }
   }

   public static BigDecimal convertFromTo(String fromCurrency, String toCurrency)
   {
      Map<String, String> symbolToName = currencySymbols();

      if (!symbolToName.containsKey(fromCurrency)) {
         throw new IllegalArgumentException(String.format("Invalid from currency: %s", fromCurrency));
      }

      if (!symbolToName.containsKey(toCurrency)) {
         throw new IllegalArgumentException(String.format("Invalid to currency: %s", toCurrency));
      }

      String url =
         String.format("http://www.gocurrency.com/v2/dorate.php?inV=1&from=%s&to=%s&Calculate=Convert",
            fromCurrency, toCurrency);

      try {
         HttpClient httpclient = httpClientFactory.get();
         HttpGet httpget = new HttpGet(url);
         HttpResponse response = httpclient.execute(httpget);
         HttpEntity entity = response.getEntity();
         StringBuilder result = new StringBuilder();

         if (entity != null) {
            InputStream inStream = entity.getContent();
            readAllLines(result, inStream);
         }

         String theWholeThing = result.toString();
         int start = theWholeThing.lastIndexOf("<div id=\"converter_results\"><ul><li>");
         String substring = result.substring(start);
         int startOfInterestingStuff = substring.indexOf("<b>") + 3;
         int endOfInterestingStuff = substring.indexOf("</b>", startOfInterestingStuff);
         String interestingStuff = substring.substring(startOfInterestingStuff, endOfInterestingStuff);
         String[] parts = interestingStuff.split("=");
         String value = parts[1].trim().split(" ")[0];
         BigDecimal bottom = new BigDecimal(value);
         return bottom;
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static void readAllLines(StringBuilder result, InputStream inStream) throws IOException
   {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(inStream))) {
         String l;

         while ((l = br.readLine()) != null) {
            result.append(l);
         }
      }
   }

   // A "field seam", allowing an internal dependency to be replaced with a mock.
   static Supplier<HttpClient> httpClientFactory = () -> HttpClientBuilder.create().build();
}
