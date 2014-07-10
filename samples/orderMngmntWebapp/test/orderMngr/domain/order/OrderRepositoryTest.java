/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;

import org.junit.*;

import mockit.*;

import orderMngr.service.*;

/**
 * Unit tests for the {@link OrderRepository} class, which depends on the {@link Database} class.
 * The tests use expectations to simulate the interaction between {@code OrderRepository} and {@code Database.
 */
public final class OrderRepositoryTest
{
   @Mocked final Database db = null; // only contain static methods, so no instance is needed
   @Tested OrderRepository repository;
   Order order;

   @Test
   public void createOrder()
   {
      order = new Order(561, "customer");
      final OrderItem orderItem = new OrderItem(order, "Prod", "Some product", 3, new BigDecimal("5.20"));
      order.getItems().add(orderItem);

      repository.create(order);

      new VerificationsInOrder() {{
         Database.executeInsertUpdateOrDelete(
            withPrefix("insert into order "), order.getNumber(), order.getCustomerId());

         Database.executeInsertUpdateOrDelete(
            withPrefix("insert into order_item "),
            order.getNumber(), orderItem.getProductId(), orderItem.getProductDescription(),
            orderItem.getQuantity(), orderItem.getUnitPrice());
      }};
   }

   @Test
   public void updateOrder()
   {
      order = new Order(1, "test");

      repository.update(order);

      new Verifications() {{
         Database.executeInsertUpdateOrDelete(withPrefix("update order "), order.getCustomerId(), order.getNumber());
      }};
   }

   @Test
   public void removeOrder()
   {
      order = new Order(35, "remove");

      repository.remove(order);

      new Verifications() {{
         Database.executeInsertUpdateOrDelete(withPrefix("delete from order "), order.getNumber());
      }};
   }
}
