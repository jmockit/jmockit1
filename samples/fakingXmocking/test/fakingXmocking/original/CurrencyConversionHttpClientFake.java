/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.original;

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.stream;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;

import mockit.*;

/**
 * This mock-up class allows the currency conversion code to be tested with a functional/integration test,
 * without needing actual access to an external web site.
 * The integration test does not refer to this class, so by default it tests the real thing, including the
 * part that hits the external web site.
 * If this class is activated for the test run, however, it will provide a partial alternate implementation
 * for the {@code AbstractHttpClient} base class, thereby avoiding actual access to the network.
 *
 * @see CurrencyConversionIntegrationTest
 */
public final class CurrencyConversionHttpClientFake extends MockUp<AbstractHttpClient>
{
   private static final BigDecimal DEFAULT_RATE = new BigDecimal("1.2");
   private final Map<String, BigDecimal> currenciesAndRates = new ConcurrentHashMap<>();

   @Mock
   public HttpResponse execute(HttpUriRequest req)
   {
      URI uri = req.getURI();
      final String response;

      if ("www.xe.com".equals(uri.getHost())) {
         response =
            "<table class='currencyTable'><tr>\r\n" +
            "  <td><a href=\"/currency/usd\">USD</a></td><td class=\"x\">Dollar</td>\r\n" +
            "  <td><a href=\"/currency/eur\">EUR</a></td><td class=\"x\">Euro</td>\r\n" +
            "  <td><a href=\"/currency/brl\">BRL</a></td><td class=\"x\">Real</td>\r\n" +
            "  <td><a href=\"/currency/cny\">CNY</a></td><td class=\"x\">Yen</td>\r\n" +
            "</tr></table>";
      }
      else {
         String[] params = uri.getQuery().split("&");
         response = formatResultContainingCurrencyConversion(params);
      }

      return new BasicHttpResponse(req.getProtocolVersion(), 200, "OK") {
         @Override
         public HttpEntity getEntity() { return createHttpResponse(response); }
      };
   }

   private String formatResultContainingCurrencyConversion(String[] params)
   {
      String from = getParameter(params, "from");
      String to = getParameter(params, "to");
      BigDecimal rate = findConversionRate(from, to);

      currenciesAndRates.put(from + '>' + to, rate.setScale(2));
      currenciesAndRates.put(to + '>' + from, BigDecimal.ONE.divide(rate, 2, RoundingMode.HALF_UP));

      return "<div id=\"converter_results\"><ul><li><b>1 " + from + " = " + rate + ' ' + to + "</b>";
   }

   private HttpEntity createHttpResponse(String content)
   {
      try {
         return new StringEntity(content);
      }
      catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   private String getParameter(String[] params, String name)
   {
      Optional<String[]> param = stream(params).map(p -> p.split("=")).filter(p -> name.equals(p[0])).findAny();
      return param.isPresent() ? param.get()[1] : null;
   }

   private BigDecimal findConversionRate(String from, String to)
   {
      BigDecimal rate = currenciesAndRates.get(from + '>' + to);

      if (rate != null) {
         return rate;
      }

      // Special cases:
      if (from.equals(to)) {
         rate = BigDecimal.ONE;
      }
      else if ("USD".equals(from) && "CNY".equals(to)) {
         rate = BigDecimal.TEN;
      }
      else if ("CNY".equals(from) && "USD".equals(to)) {
         rate = new BigDecimal("0.1");
      }
      else {
         rate = DEFAULT_RATE;
      }

      return rate;
   }
}
