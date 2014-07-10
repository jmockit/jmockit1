/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.simplified;

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.stream;

import mockit.*;

/**
 * The URL class is used internally by the JRE itself, and we don't want to interfere.
 * Therefore, mock methods in this class proceed back into the original implementation whenever the real, not fake,
 * behavior is desired.
 */
public final class CurrencyConversionURLFake extends MockUp<URL>
{
   private static final BigDecimal DEFAULT_RATE = new BigDecimal("1.2");
   private static final Map<String, BigDecimal> currenciesAndRates = new ConcurrentHashMap<>();

   @Mock
   public InputStream openStream(Invocation inv)
   {
      URL url = inv.getInvokedInstance();
      String host = url.getHost();
      String response;

      switch (host) {
         case "www.xe.com":
            response =
               "<table class='currencyTable'><tr>\r\n" +
               "  <td><a href='/currency/usd'>USD</a></td><td class='x'>Dollar</td>\r\n" +
               "  <td><a href='/currency/eur'>EUR</a></td><td class='x'>Euro</td>\r\n" +
               "  <td><a href='/currency/brl'>BRL</a></td><td class='x'>Real</td>\r\n" +
               "  <td><a href='/currency/cny'>CNY</a></td><td class='x'>Yen</td>\r\n" +
               "</tr></table>";
            break;
         case "www.gocurrency.com":
            String[] params = url.getQuery().split("&");
            response = formatResultContainingCurrencyConversion(params);
            break;
         default:
            return inv.proceed();
      }

      return new ByteArrayInputStream(response.getBytes());
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
