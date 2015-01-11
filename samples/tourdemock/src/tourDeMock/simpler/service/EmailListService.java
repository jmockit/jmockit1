/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.simpler.service;

import java.util.*;

import tourDeMock.original.service.*;

public final class EmailListService
{
   /**
    * Retrieves the list of email addresses with the specified name.
    *
    * @throws EmailListNotFound if no list exists with the given name
    */
   public List<String> getListByName(String listName) throws EmailListNotFound
   {
      return Collections.emptyList();
   }
}