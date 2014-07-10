/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.sql.*;
import java.util.*;

import static orderMngr.service.Database.*;

public final class OrderRepository
{
   public void create(Order order)
   {
      String sql = "insert into order (number, customer_id) values (?, ?)";
      int orderNo = order.getNumber();
      executeInsertUpdateOrDelete(sql, orderNo, order.getCustomerId());

      for (OrderItem item : order.getItems()) {
         executeInsertUpdateOrDelete(
            "insert into order_item (order_no, product_id, product_desc, quantity, unit_price) values (?, ?, ?, ?, ?)",
            orderNo, item.getProductId(), item.getProductDescription(), item.getQuantity(),
            item.getUnitPrice());
      }
   }

   public void update(Order order)
   {
      String sql = "update order set customer_id=? where number=?";
      executeInsertUpdateOrDelete(sql, order.getCustomerId(), order.getNumber());
   }

   public void remove(Order order)
   {
      String sql = "delete from order where number=?";
      executeInsertUpdateOrDelete(sql, order.getNumber());
   }

   public Order findByNumber(int orderNumber)
   {
      try {
         return loadOrder(orderNumber);
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }

   private Order loadOrder(int orderNumber) throws SQLException
   {
      ResultSet result = executeQuery("select customer_id from order where number=?", orderNumber);

      try {
         if (result.next()) {
            String customerId = result.getString(1);
            Order order = new Order(orderNumber, customerId);
            loadOrderItems(order);
            return order;
         }

         return null;
      }
      finally {
         closeStatement(result);
      }
   }

   private void loadOrderItems(Order order) throws SQLException
   {
      ResultSet result =
         executeQuery(
            "select product_id, product_desc, quantity, unit_price from order_item where order_number=?",
            order.getNumber());

      try {
         while (result.next()) {
            String productId = result.getString(1);
            String productDescription = result.getString(2);
            int quantity = result.getInt(3);
            BigDecimal unitPrice = result.getBigDecimal(4);

            OrderItem item = new OrderItem(order, productId, productDescription, quantity, unitPrice);
            order.getItems().add(item);
         }
      }
      finally {
         closeStatement(result);
      }
   }

   public List<Order> findByCustomer(String customerId)
   {
      ResultSet result = executeQuery("select number from order where customer_id=?", customerId);

      try {
         List<Order> orders = new ArrayList<>();

         while (result.next()) {
            int orderNumber = result.getInt(1);
            Order order = new Order(orderNumber, customerId);
            loadOrderItems(order);
            orders.add(order);
         }

         return orders;
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(result);
      }
   }
}
