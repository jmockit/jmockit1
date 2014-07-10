package org.mockitousage.examples;

public interface ItemService
{
   Item getItem(long id) throws ItemNotFoundException;

   void deleteItem(long id);
}
