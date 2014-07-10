/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.springdi.service;

import java.util.*;

import org.springframework.stereotype.*;
import tourDeMock.original.service.*;

@Service
public class EmailListService
{
   /**
    * Retrieves the list of email addresses with the specified name. If no list exists with that
    * name an exception is thrown.
    */
   public List<String> getListByName(String listName) throws EmailListNotFound
   {
      return Collections.emptyList();
   }
}
