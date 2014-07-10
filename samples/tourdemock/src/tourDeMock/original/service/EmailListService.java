package tourDeMock.original.service;

import java.util.*;

public interface EmailListService
{
   String KEY = "service.EmailListService";

   /**
    * Retrieves the list of email addresses with the specified name. If no list exists with that
    * name an exception is thrown.
    */
   List<String> getListByName(String listName) throws EmailListNotFound;
}
