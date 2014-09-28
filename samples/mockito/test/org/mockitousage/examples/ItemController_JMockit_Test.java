/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.mockitousage.examples;

import java.util.*;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * These tests are equivalent to the ones in {@link ItemControllerTest}.
 */
public final class ItemController_JMockit_Test
{
   @Tested ItemController itemController;
   @Injectable ItemService itemService;
   final Map<String, Object> modelMap = new HashMap<>();

   @Test
   public void testViewItem() throws Exception
   {
      final Item item = new Item(1, "Item 1");
      new Expectations() {{ itemService.getItem(item.getId()); result = item; }};

      String view = itemController.viewItem(item.getId(), modelMap);

      assertEquals(item, modelMap.get("item"));
      assertEquals("viewItem", view);
   }

   @Test
   public void testViewItemWithItemNotFoundException() throws Exception
   {
      final ItemNotFoundException exception = new ItemNotFoundException(5);
      new Expectations() {{ itemService.getItem(5); result = exception; }};

      String view = itemController.viewItem(5, modelMap);

      assertEquals("redirect:/errorView", view);
      assertSame(exception, modelMap.get("exception"));
   }

   @Test
   public void testDeleteItem() throws Exception
   {
      String view = itemController.deleteItem(5);

      new Verifications() {{ itemService.deleteItem(5); }};
      assertEquals("redirect:/itemList", view);
   }
}
