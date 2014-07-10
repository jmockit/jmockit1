/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.util.ArrayList;
import java.util.*;

import org.junit.*;

import mockit.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

public final class OrderFactoryTest
{
   @Tested OrderFactory factory;
   @Mocked OrderRepository orderRepository;

   @Test
   public void createOrder() throws Exception
   {
      // Test data:
      List<OrderItem> expectedItems = asList(
         new OrderItem("393439493", "Core Java 5 6ed", 2, new BigDecimal("45.00")),
         new OrderItem("04940458", "JUnit Recipes", 1, new BigDecimal("49.95")));

      // Exercises code under test:
      String customerId = "123";
      final Order created = factory.createOrder(customerId, expectedItems);

      // Conventional JUnit state-based verifications:
      assertNotNull(created);
      assertEquals(customerId, created.getCustomerId());
      assertEquals(expectedItems, created.getItems());

      // Verify that expected invocations (excluding the ones inside a previous Expectations block)
      // actually occurred:
      new Verifications() {{
         orderRepository.create(created);
      }};
   }
}
