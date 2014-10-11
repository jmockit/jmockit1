package tourDeMock.original;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.runner.*;

import org.mockito.*;
import org.mockito.runners.*;
import tourDeMock.original.service.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class EmailListServlet_MockitoTest
{
   EmailListServlet servlet;

   @Mock HttpServletRequest request;
   @Mock HttpServletResponse response;
   @Mock EmailListService emailListService;

   @Before
   public void before() throws Exception
   {
      ServletConfig servletConfig = mock(ServletConfig.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
      when(servletConfig.getServletContext().getAttribute(EmailListService.KEY)).thenReturn(emailListService);

      servlet = new EmailListServlet();
      servlet.init(servletConfig);
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      when(emailListService.getListByName(null)).thenThrow(new EmailListNotFound());

      servlet.doGet(request, response);
   }

   @Test
   public void doGetWithList() throws Exception
   {
      PrintWriter writer = mock(PrintWriter.class);
      when(response.getWriter()).thenReturn(writer);

      List<String> emails = asList("larry@stooge.com", "moe@stooge.com", "curley@stooge.com");
      when(emailListService.getListByName(anyString())).thenReturn(emails);

      servlet.doGet(request, response);

      InOrder order = inOrder(writer, response);
      order.verify(writer).println("larry@stooge.com");
      order.verify(writer).println("moe@stooge.com");
      order.verify(writer).println("curley@stooge.com");
      order.verify(response).flushBuffer();
   }
}
