/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.springdi;

import java.io.*;
import java.util.*;
import javax.inject.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.springframework.stereotype.*;
import tourDeMock.original.service.*;
import tourDeMock.springdi.service.EmailListService;

@Controller
public final class EmailListServlet extends HttpServlet
{
   @Inject private EmailListService emailListService;

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String listName = request.getParameter("listName");

      try {
         List<String> emails = emailListService.getListByName(listName);
         writeListOfEmailsToClient(response.getWriter(), emails);
      }
      catch (EmailListNotFound e) {
         throw new ServletException("No e-mail list with the given name was found", e);
      }

      response.flushBuffer();
   }

   private void writeListOfEmailsToClient(PrintWriter writer, List<String> emails)
   {
      for (String email : emails) {
         writer.println(email);
      }
   }
}
