package tourDeMock.original;

import java.util.*;
import javax.servlet.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.mock.web.*;
import tourDeMock.original.service.*;

public final class EmailListServlet_SpringMockTest
{
   static final String SEP = System.getProperty("line.separator");

   EmailListServlet servlet;

   MockHttpServletRequest request;
   MockHttpServletResponse response;

   @Before
   public void before() throws ServletException
   {
      EmailListService emailListService = new MockEmailListService();

      ServletConfig servletConfig = new MockServletConfig();
      servletConfig.getServletContext().setAttribute(EmailListService.KEY, emailListService);

      servlet = new EmailListServlet();
      servlet.init(servletConfig);

      request = new MockHttpServletRequest();
      response = new MockHttpServletResponse();
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      servlet.doGet(request, response);
   }

   @Test
   public void doGetWithList() throws Exception
   {
      request.setParameter("listName", "foo");

      servlet.doGet(request, response);

      assertTrue(response.isCommitted());
      assertEquals(
         "larry@stooge.com" + SEP + "moe@stooge.com" + SEP + "curley@stooge.com" + SEP,
         response.getContentAsString());
   }

   private static final class MockEmailListService implements EmailListService
   {
      @Override
      public List<String> getListByName(String listName) throws EmailListNotFound
      {
         if (listName == null) {
            throw new EmailListNotFound();
         }

         return asList("larry@stooge.com", "moe@stooge.com", "curley@stooge.com");
      }
   }
}
