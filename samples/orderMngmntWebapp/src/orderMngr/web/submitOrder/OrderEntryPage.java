/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.web.submitOrder;

import java.math.*;
import java.util.*;

import orderMngr.domain.order.*;

public final class OrderEntryPage
{
   // Data accumulated before submitting the order:
   private String customerId;
   private List<OrderItem> orderItems;

   // Parameters for adding an order item:
   private String productId;
   private String productDescription;
   private int quantity;
   private BigDecimal unitPrice;

   // Item index for removing an item:
   private int itemToRemove;

   // Resulting data when the order is submitted:
   private int orderNo;

   public void load()
   {
      orderItems = new ArrayList<>(5);
      // use some web MVC framework service to retrieve item data, either from request parameters
      // or from the HTTPSession
   }

   public void setCustomerId(String customerId) { this.customerId = customerId; }

   public List<OrderItem> getOrderItems() { return orderItems; }

   public void setProductId(String productId) { this.productId = productId; }

   public void setProductDescription(String productDescription) { this.productDescription = productDescription; }

   public void setQuantity(int quantity) { this.quantity = quantity; }

   public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

   public void setItemToRemove(int itemToRemove) { this.itemToRemove = itemToRemove; }

   public int getOrderNo() { return orderNo; }

   public void addItem()
   {
      OrderItem item = new OrderItem(productId, productDescription, quantity, unitPrice);
      orderItems.add(item);
   }

   public void removeItem()
   {
      orderItems.remove(itemToRemove);
   }

   public void submitOrder() throws Exception
   {
      Order order = new OrderFactory().createOrder(customerId, orderItems);
      orderNo = order.getNumber();
   }
}
