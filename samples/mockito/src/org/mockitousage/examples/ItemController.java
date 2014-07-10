package org.mockitousage.examples;

import java.util.Map;

public class ItemController
{
   private final ItemService itemService;

   public ItemController(ItemService itemService)
   {
      this.itemService = itemService;
   }

   public String viewItem(long id, Map<String, Object> modelMap)
   {
      try {
         Item item = itemService.getItem(id);
         modelMap.put("item", item);
         return "viewItem";
      }
      catch (ItemNotFoundException e) {
         modelMap.put("exception", e);
         return "redirect:/errorView";
      }
   }

   public String deleteItem(long id)
   {
      itemService.deleteItem(id);
      return "redirect:/itemList";
   }
}
