/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.util.*;

import org.junit.*;

import mockit.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

public final class OrderFactory_MockupsAPI_Test
{
   @Test
   public void createOrder() throws Exception
   {
      String customerId = "123";
      List<OrderItem> items = asList(
         new OrderItem("393439493", "Core Java 5 6ed", 2, new BigDecimal("45.00")),
         new OrderItem("04940458", "JUnit Recipes", 1, new BigDecimal("49.95")));

      MockOrder mockOrder = new MockOrder(customerId);

      new MockUp<OrderRepository>()
      {
         @Mock(invocations = 1)
         void create(Order order)
         {
            assertNotNull(order);
         }
      };

      Order order = new OrderFactory().createOrder(customerId, items);

      assertNotNull(order);
      assertEquals(items, mockOrder.items);
   }

   static final class MockOrder extends MockUp<Order>
   {
      final String expectedCustomerId;
      final Collection<OrderItem> items = new ArrayList<>();

      MockOrder(String customerId) { expectedCustomerId = customerId; }

      @Mock(invocations = 1)
      void $init(int number, String actualCustomerId)
      {
         assertTrue(number > 0);
         assertEquals(expectedCustomerId, actualCustomerId);
      }

      @Mock(invocations = 1)
      Collection<OrderItem> getItems() { return items; }
   }

   // The following tests are here just for completeness, since they have no need for mocking.

   @Test(expected = MissingOrderItems.class)
   public void createOrderWithEmptyItemList() throws Exception
   {
      new OrderFactory().createOrder("45", Collections.<OrderItem>emptyList());
   }

   @Test(expected = InvalidOrderItem.class)
   public void createOrderWithInvalidItemQuantity() throws Exception
   {
      List<OrderItem> items = asList(new OrderItem("393439493", "Core Java 5 6ed", 0, new BigDecimal("45.00")));

      new OrderFactory().createOrder("45", items);
   }

   @Test(expected = InvalidOrderItem.class)
   public void createOrderWithInvalidItemUnitPrice() throws Exception
   {
      List<OrderItem> items = asList(new OrderItem("393439493", "Core Java 5 6ed", 1, new BigDecimal("-5.20")));

      new OrderFactory().createOrder("45", items);
   }

   @Test(expected = DuplicateOrderItem.class)
   public void createOrderWithDuplicateItem() throws Exception
   {
      List<OrderItem> items = asList(
         new OrderItem("39", "Core Java 5 6ed", 1, new BigDecimal("45.00")),
         new OrderItem("39", "Xyz", 1, new BigDecimal("67.50")));

      new OrderFactory().createOrder("45", items);
   }
}
