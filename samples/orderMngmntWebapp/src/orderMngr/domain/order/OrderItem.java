/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import javax.persistence.*;

@Entity
public class OrderItem
{
   @Id @GeneratedValue
   private int id;

   @ManyToOne
   private Order order;

   private String productId;

   private String productDescription;

   private int quantity;

   private BigDecimal unitPrice;

   public OrderItem() {}

   public OrderItem(String productId, String productDescription, int quantity, BigDecimal unitPrice)
   {
      this(null, productId, productDescription, quantity, unitPrice);
   }

   public OrderItem(
      Order order, String productId, String productDescription, int quantity, BigDecimal unitPrice)
   {
      this.order = order;
      this.productId = productId;
      this.productDescription = productDescription;
      this.quantity = quantity;
      this.unitPrice = unitPrice;
   }

   public int getId()
   {
      return id;
   }

   public Order getOrder()
   {
      return order;
   }

   public String getProductId()
   {
      return productId;
   }

   public String getProductDescription()
   {
      return productDescription;
   }

   public int getQuantity()
   {
      return quantity;
   }

   public BigDecimal getUnitPrice()
   {
      return unitPrice;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OrderItem orderItem = (OrderItem) o;

      return productId.equals(orderItem.productId);
   }

   @Override
   public int hashCode()
   {
      return productId.hashCode();
   }
}
