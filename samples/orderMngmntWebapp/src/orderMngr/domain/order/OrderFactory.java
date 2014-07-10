/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.util.*;

public final class OrderFactory
{
   // Just for simplicity. In a real application, a more robust mechanism would be used for
   // generating sequential order numbers, such as a database sequence or identity column.
   private static int nextOrderNo = 1;

   public Order createOrder(String customerId, List<OrderItem> items)
      throws MissingOrderItems, InvalidOrderItem, DuplicateOrderItem
   {
      if (items.isEmpty()) {
         throw new MissingOrderItems();
      }

      validateOrderItems(items);

      Order order = new Order(nextOrderNo++, customerId);
      order.getItems().addAll(items);

      new OrderRepository().create(order);

      return order;
   }

   private void validateOrderItems(List<OrderItem> items) throws InvalidOrderItem, DuplicateOrderItem
   {
      for (OrderItem item : items) {
         if (item.getQuantity() <= 0 || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderItem("Quantity=" + item.getQuantity() + ", Unit Price=" + item.getUnitPrice());
         }
      }

      if (new HashSet<>(items).size() < items.size()) {
         throw new DuplicateOrderItem();
      }
   }
}
