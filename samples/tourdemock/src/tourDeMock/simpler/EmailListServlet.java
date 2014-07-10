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
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
   {
      String listName = request.getParameter("listName");

      try {
         List<String> emails = new EmailListService().getListByName(listName);
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