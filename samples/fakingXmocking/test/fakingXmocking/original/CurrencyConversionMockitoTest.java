/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package fakingXmocking.original;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.mockito.*;
import org.mockito.runners.*;

import org.junit.*;
import org.junit.runner.*;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

/**
 * This test class shows that it's trivial to make the original CurrencyConversion "testable"
 * (in the sense of allowing a conventional mocking tool to be used).
 * After this initial refactoring, passing unit tests can be written and then used to prevent the
 * introduction of bugs during more extensive refactoring.
 * <p/>
 * Applying more elaborate refactorings before being able to create a set of tests should not be recommended,
 * due to the increased risk of breaking existing functionality not yet covered by automated tests.
 * For example, extracting "untestable" blocks of code into new helper methods in the tested class may allow
 * said methods to be mocked away, but only at the cost of applying "partial" mocking, a technique which very
 * much goes against the idea that the <em>collaborators</em> of a tested class should be mocked, not parts
 * of the tested class itself.
 * <p/>
 * This experiment shows that, while the use of a mocking tool with limitations in what it can mock
 * (or the use of no mocking tool at all) does force some refactoring of the tested code, there is
 * no guarantee that the developer will refactor it "enough" to reach a desired quality level.
 * Such a guarantee can only be obtained from the judicious use of static analysis tools and peer reviews.
 * <p/>
 * Another observation from these particular tests is that they can give a false sense of security, given
 * that actual calls to the "httpClient" collaborator are not verified.
 * Such verifications could easily be added through {@code verify(httpClient).execute(...)} calls, but they
 * would introduce duplicate code in the test method;
 * as a consequence, developers may feel inclined to leave them out.
 */
@RunWith(MockitoJUnitRunner.class)
public final class CurrencyConversionMockitoTest
{
   @Mock(answer = Answers.RETURNS_DEEP_STUBS) HttpClient httpClient;
   Supplier<HttpClient> originalFactory;

   @Mock
   HttpEntity httpEntity; // not automatically used as a "deep stub"

   @Before
   public void prepareSUT() throws Exception
   {
      originalFactory = CurrencyConversion_testable.httpClientFactory;
      CurrencyConversion_testable.httpClientFactory = () -> httpClient;

      // Needed because Mockito would not use the existing HttpEntity mock, but create a new one:
      when(httpClient.execute(any(HttpGet.class)).getEntity()).thenReturn(httpEntity);
   }

   @After
   public void resetSUT()
   {
      CurrencyConversion_testable.httpClientFactory = originalFactory;
      CurrencyConversion_testable.allCurrenciesCache = null;
   }

   @Test
   public void loadCurrencySymbolsFromWebSite() throws Exception
   {
      String content =
         "<table class='currencyTable'>\r\n" +
         "<td><a href=\"/currency/x\">USD</a></td><td class=\"x\">Dollar</td>\r\n" +
         "<td><a href=\"/currency/x\">EUR</a></td><td class=\"x\">Euro</td>";
      when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));

      Map<String, String> symbols = CurrencyConversion_testable.currencySymbols();

      assertEquals(2, symbols.size());
      assertTrue(symbols.containsKey("USD"));
      assertTrue(symbols.containsKey("EUR"));
   }

   @Test
   public void convertFromOneCurrencyToAnother() throws Exception
   {
      CurrencyConversion_testable.allCurrenciesCache = new HashMap<String, String>() {{ put("X", ""); put("Y", ""); }};

      String content = "<div id=\"converter_results\"><ul><li><b>1 X = 1.3 Y</b>";
      when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));

      BigDecimal rate = CurrencyConversion_testable.convertFromTo("X", "Y");

      assertEquals("1.3", rate.toPlainString());
   }
}
