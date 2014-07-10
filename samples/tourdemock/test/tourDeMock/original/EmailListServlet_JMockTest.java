package tourDeMock.original;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import static java.util.Arrays.*;

import org.junit.*;

import org.jmock.*;
import org.jmock.auto.*;
import org.jmock.integration.junit4.*;
import org.jmock.lib.legacy.*;
import tourDeMock.original.service.*;

public final class EmailListServlet_JMockTest
{
   @Rule public final JUnitRuleMockery mockery = new JUnitRuleMockery();

   EmailListServlet servlet;

   @Mock HttpServletRequest request;
   @Mock HttpServletResponse response;
   @Mock EmailListService emailListService;

   public EmailListServlet_JMockTest()
   {
      mockery.setImposteriser(ClassImposteriser.INSTANCE);
   }

   @Before
   public void before() throws Exception
   {
      final ServletConfig servletConfig = mockery.mock(ServletConfig.class);
      final ServletContext servletContext = mockery.mock(ServletContext.class);

      mockery.checking(new Expectations() {{
         oneOf(servletConfig).getServletContext(); will(returnValue(servletContext));
         oneOf(servletContext).getAttribute(EmailListService.KEY); will(returnValue(emailListService));
         ignoring(request);
      }});

      servlet = new EmailListServlet();
      servlet.init(servletConfig);
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      mockery.checking(new Expectations() {{
         oneOf(emailListService).getListByName(""); will(throwException(new EmailListNotFound()));
      }});

      servlet.doGet(request, response);
   }

   @Test
   public void doGetWithList() throws Exception
   {
      final PrintWriter writer = mockery.mock(PrintWriter.class);

      mockery.checking(new Expectations() {{
         List<String> list = asList("larry@stooge.com", "moe@stooge.com", "curley@stooge.com");
         oneOf(emailListService).getListByName(with(any(String.class))); will(returnValue(list));

         oneOf(response).getWriter(); will(returnValue(writer));

         Sequence printSequence = mockery.sequence("printSequence");
         oneOf(writer).println("larry@stooge.com"); inSequence(printSequence);
         oneOf(writer).println("moe@stooge.com"); inSequence(printSequence);
         oneOf(writer).println("curley@stooge.com"); inSequence(printSequence);
         oneOf(response).flushBuffer(); inSequence(printSequence);
      }});

      servlet.doGet(request, response);
   }
}
