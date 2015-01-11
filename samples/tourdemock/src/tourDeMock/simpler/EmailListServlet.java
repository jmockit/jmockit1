/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.simpler;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import tourDeMock.original.service.*;
import tourDeMock.simpler.service.EmailListService;

public final class EmailListServlet extends HttpServlet
{
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String listName = request.getParameter("listName");
      List<String> emails;

      try {
         emails = new EmailListService().getListByName(listName);
      }
      catch (EmailListNotFound e) {
         throw new ServletException("No e-mail list with the given name was found", e);
      }

      writeListOfEmailsToClient(response, emails);
   }

   private static void writeListOfEmailsToClient(HttpServletResponse response, List<String> emails) throws IOException
   {
      PrintWriter writer = response.getWriter();

      for (String email : emails) {
         writer.println(email);
      }

      response.flushBuffer();
   }
}