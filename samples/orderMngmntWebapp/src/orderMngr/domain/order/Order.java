/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.util.*;
import javax.persistence.*;

/**
 * A business order to buy products.
 */
@Entity
public class Order
{
   @Id
   private int number;

   private String customerId;

   @OneToMany
   private List<OrderItem> items = new LinkedList<OrderItem>();

   public Order() {}

   public Order(int number, String customerId)
   {
      this.number = number;
      this.customerId = customerId;
   }

   public int getNumber() { return number; }
   public String getCustomerId() { return customerId; }
   public List<OrderItem> getItems() { return items; }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Order order = (Order) o;

      return number == order.getNumber();
   }

   @Override
   public int hashCode()
   {
      return number;
   }
}
