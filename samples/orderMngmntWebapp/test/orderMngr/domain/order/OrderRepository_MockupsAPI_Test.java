/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

import java.math.*;
import java.sql.*;
import java.util.*;

import org.junit.*;

import mockit.*;

import orderMngr.service.*;
import static org.junit.Assert.*;

/**
 * State-based unit tests for the OrderRepository class, which depends on the {@linkplain Database} class.
 * The tests use mocks to simulate the interaction between {@code OrderRepository} and {@code Database}.
 */
public final class OrderRepository_MockupsAPI_Test
{
   static PreparedStatement proxyStmt;
   static ResultSet proxyRS;
   Order order;
   OrderItem orderItem;
   @Tested OrderRepository repository;

   @Test
   public void createOrder()
   {
      order = new Order(561, "customer");
      orderItem = new OrderItem(order, "Prod", "Some product", 3, new BigDecimal("5.20"));
      order.getItems().add(orderItem);

      new MockUp<Database>() {
         boolean orderInserted;

         @Mock(invocations = 2)
         void executeInsertUpdateOrDelete(String sql, Object... args)
         {
            if (orderInserted) {
               assertTrue(sql.trim().toLowerCase().startsWith("insert into order_item "));
               assertEquals(5, args.length);
               assertEquals(order.getNumber(), args[0]);
               assertEquals(orderItem.getProductId(), args[1]);
               assertEquals(orderItem.getProductDescription(), args[2]);
               assertEquals(orderItem.getQuantity(), args[3]);
               assertEquals(orderItem.getUnitPrice(), args[4]);
            }
            else {
               assertTrue(sql.trim().toLowerCase().startsWith("insert into order "));
               assertEquals(order.getNumber(), args[0]);
               assertEquals(order.getCustomerId(), args[1]);
               orderInserted = true;
            }
         }
      };

      repository.create(order);
   }

   @Test
   public void updateOrder()
   {
      order = new Order(1, "test");

      new MockUp<Database>() {
         @Mock(invocations = 1)
         void executeInsertUpdateOrDelete(String sql, Object... args)
         {
            assertTrue(sql.trim().toLowerCase().startsWith("update order "));
            String customerId = (String) args[0];
            assertEquals("test", customerId);
            Integer orderNo = (Integer) args[1];
            assertEquals(1, orderNo.intValue());
         }
      };

      repository.update(order);
   }

   @Test
   public void removeOrder()
   {
      order = new Order(35, "remove");

      new MockUp<Database>() {
         @Mock(minInvocations = 1, maxInvocations = 1) // equivalent to "invocations = 1"
         void executeInsertUpdateOrDelete(String sql, Object... args)
         {
            assertTrue(sql.trim().toLowerCase().startsWith("delete from order "));
            assertEquals(order.getNumber(), args[0]);
         }
      };

      repository.remove(order);
   }

   @Test
   public void findOrderByNumber()
   {
      order = new Order(1, "test");
      orderItem = new OrderItem(order, "343443", "Some product", 3, new BigDecimal(5));
      order.getItems().add(orderItem);

      final Connection connection = new MockUp<Connection>() {
         @Mock
         PreparedStatement prepareStatement(String sql)
         {
            assertNotNull(sql);
            return proxyStmt;
         }
      }.getMockInstance();

      new MockUp<Database>() {
         @Mock
         Connection connection() { return connection; }
      };

      proxyStmt = new MockUp<PreparedStatement>() {
         @Mock
         int executeUpdate() { return 1; }

         @Mock
         ResultSet executeQuery() { return proxyRS; }
      }.getMockInstance();

      proxyRS = new MockUp<ResultSet>() {
         int callNo;

         @Mock
         boolean next()
         {
            callNo++;
            assertTrue("attempted to read more DB rows than expected", callNo <= 3);
            return callNo < 3;
         }

         @Mock
         String getString(int columnIndex)
         {
            if (callNo == 1) {
               return order.getCustomerId();
            }

            return columnIndex == 1 ? orderItem.getProductId() : orderItem.getProductDescription();
         }

         @Mock
         int getInt(int i)
         {
            assertEquals(3, i);
            return orderItem.getQuantity();
         }

         @Mock
         BigDecimal getBigDecimal(int i)
         {
            assertEquals(4, i);
            return orderItem.getUnitPrice();
         }

         @Mock
         Statement getStatement() { return proxyStmt; }
      }.getMockInstance();

      Order found = repository.findByNumber(order.getNumber());

      assertEquals(order, found);
   }

   @Test
   public void findOrderByCustomer()
   {
      order = new Order(890, "Cust");

      new MockDatabaseForFindByCustomer();
      new MockUp<OrderRepository>() { @Mock void loadOrderItems(Order o) {} };

      List<Order> found = repository.findByCustomer(order.getCustomerId());

      assertTrue("Order not found by customer id", found.contains(order));
   }

   public final class MockDatabaseForFindByCustomer extends MockUp<Database>
   {
      private ResultSet mockRS;

      @Mock(invocations = 1)
      public ResultSet executeQuery(String sql, Object... args)
      {
         assertTrue(
            "Invalid Order query: " + sql, sql.matches("select.+from\\s+order.*where.+customer_id\\s*=\\s*\\?"));
         assertEquals(1, args.length);
         assertEquals("Cust", args[0]);

         mockRS = new MockUp<ResultSet>() {
            private int rowIndex;

            @Mock(invocations = 2)
            boolean next()
            {
               rowIndex++;
               return rowIndex == 1;
            }

            @Mock(invocations = 1)
            int getInt(int i)
            {
               assertEquals(1, i);
               return order.getNumber();
            }
         }.getMockInstance();

         return mockRS;
      }

      @Mock(invocations = 1)
      public void closeStatement(ResultSet result)
      {
         assertSame(mockRS, result);
      }
   }
}
