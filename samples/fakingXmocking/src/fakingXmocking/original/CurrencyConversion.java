package fakingXmocking.original;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;

/**
 * From <a href="http://martinfowler.com/articles/modernMockingTools.html">this</a> article, but reformatted and
 * refactored to use Java 7 and fix typos & warnings.
 */
public final class CurrencyConversion
{
   static final int CACHE_DURATION = 5 * 60 * 1000;
   static Map<String, String> allCurrenciesCache;
   static long lastCacheRead = Long.MAX_VALUE;

   private CurrencyConversion() {}

   public static Map<String, String> currencySymbols()
   {
      if (allCurrenciesCache != null && System.currentTimeMillis() - lastCacheRead < CACHE_DURATION) {
         return allCurrenciesCache;
      }

      Map<String, String> symbolToName = new ConcurrentHashMap<>();

      try {
         HttpClient httpClient = HttpClientBuilder.create().build();
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
         HttpClient httpclient = HttpClientBuilder.create().build();
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
}
