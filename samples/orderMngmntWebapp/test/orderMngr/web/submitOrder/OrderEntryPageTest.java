/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.web.submitOrder;

import java.math.*;

import org.junit.*;

import mockit.*;

import orderMngr.domain.order.*;
import static org.junit.Assert.*;

public final class OrderEntryPageTest
{
   @Test
   public void submitOrder(@Mocked final OrderFactory orderFactory) throws Exception
   {
      final OrderEntryPage page = new OrderEntryPage();
      page.load();

      final String customerId = "889000";
      page.setCustomerId(customerId);
      page.getOrderItems().add(new OrderItem("3934", "test item", 2, new BigDecimal(20)));
      assertEquals(0, page.getOrderNo());

      final int orderNo = 464;

      new Expectations() {{
         orderFactory.createOrder(customerId, page.getOrderItems());
         result = new Order(orderNo, customerId);
      }};

      page.submitOrder();

      assertEquals(orderNo, page.getOrderNo());
   }
}
