package tourDeMock.original;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import tourDeMock.original.service.*;

public final class EmailListServlet extends HttpServlet
{
   // In a real application, it's unlikely that this interface would have multiple implementations.
   // A simpler (and just as effective) design, therefore, would be to make EmailListService a
   // concrete class which would get instantiated whenever needed (there is no real advantage in
   // reusing a single global instance). Such a change would eliminate the init() method and the KEY
   // constant, and would allow the entire before() method to be removed from the test class (at
   // least in the JMockit version). There would be no mocking of ServletConfig or ServletContext.
   private EmailListService emailListService;

   @Override
   public void init() throws ServletException
   {
      emailListService = (EmailListService) getServletContext().getAttribute(EmailListService.KEY);

      if (emailListService == null) {
         throw new ServletException("No ListService available!");
      }
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String listName = request.getParameter("listName");
      List<String> emails;

      try {
         emails = emailListService.getListByName(listName);
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
