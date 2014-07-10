package org.mockitousage.examples;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.runners.*;

/**
 * This test class was originally written by Richard Paul.
 * See his <a href="http://www.rapaul.com/2008/11/19/mocking-in-java-with-mockito">blog</a.
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemControllerTest
{
   @InjectMocks ItemController itemController;
   @Mock ItemService itemService;
   final Map<String, Object> modelMap = new HashMap<>();

   @Test
   public void testViewItem() throws Exception
   {
      Item item = new Item(1, "Item 1");
      when(itemService.getItem(item.getId())).thenReturn(item);

      String view = itemController.viewItem(item.getId(), modelMap);

      assertEquals(item, modelMap.get("item"));
      assertEquals("viewItem", view);
   }

   @Test
   public void testViewItemWithItemNotFoundException() throws Exception
   {
      ItemNotFoundException exception = new ItemNotFoundException(5);
      when(itemService.getItem(5)).thenThrow(exception);

      String view = itemController.viewItem(5, modelMap);

      assertEquals("redirect:/errorView", view);
      assertSame(exception, modelMap.get("exception"));
   }

   @Test
   public void testDeleteItem()
   {
      String view = itemController.deleteItem(5);

      verify(itemService).deleteItem(5);
      assertEquals("redirect:/itemList", view);
   }
}
