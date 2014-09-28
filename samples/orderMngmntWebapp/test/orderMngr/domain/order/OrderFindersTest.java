/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.sql.*;
import java.util.*;

import orderMngr.service.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static mockit.Deencapsulation.*;

public final class OrderFindersTest
{
   @Mocked final Database db = null;
   @Mocked ResultSet rs;
   @Tested @Mocked OrderRepository repository;
   Order order;

   @Test
   public void findOrderByNumber(@Mocked final ResultSet rsItems) throws Exception
   {
      // Set up state.
      order = new Order(1, "test");
      final OrderItem orderItem = new OrderItem(order, "343443", "Some product", 3, new BigDecimal(5));
      order.getItems().add(orderItem);

      // Expectations for the first database query, which loads order data:
      new Expectations() {{
         Database.executeQuery(withSubstring("where number="), order.getNumber());
         result = rs;

         rs.next(); result = true;
         rs.getString(1); result = order.getCustomerId();
      }};

      // Expectations for the second query, which loads the order items:
      new Expectations() {{
         Database.executeQuery(withMatch("select .+ from order_item where .+"), order.getNumber());
         result = rsItems;

         rsItems.next(); result = new boolean[] {true, false};

         rsItems.getString(1); result = orderItem.getProductId();
         rsItems.getString(2); result = orderItem.getProductDescription();
         rsItems.getInt(3); result = orderItem.getQuantity();
         rsItems.getBigDecimal(4); result = orderItem.getUnitPrice();
      }};

      // Exercise code under test:
      Order found = repository.findByNumber(order.getNumber());

      // Verify results:
      assertEquals(order, found);
      assertEquals(1, found.getItems().size());
      assertEquals(orderItem, found.getItems().get(0));
   }

   @Test
   public void findOrderByCustomer() throws Exception
   {
      final String customerId = "Cust";
      order = new Order(890, customerId);

      new Expectations() {{
         Database.executeQuery(withMatch("select.+from\\s+order.*where.+customer_id\\s*=\\s*\\?"), customerId);
         result = rs;

         rs.next(); result = new boolean[] {true, false};
         rs.getInt(1); result = order.getNumber();
      }};

      // Causes an already tested private method to do nothing:
      new Expectations() {{
         invoke(repository, "loadOrderItems", order);
      }};

      List<Order> found = repository.findByCustomer(customerId);

      assertTrue("Order not found by customer id", found.contains(order));

      new Verifications() {{ Database.closeStatement(rs); }};
   }
}
