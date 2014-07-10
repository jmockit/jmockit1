/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.original;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import static java.util.Arrays.*;

import org.junit.*;

import mockit.*;

import tourDeMock.original.service.*;

public final class EmailListServlet_JMockitTest
{
   EmailListServlet servlet;

   @Mocked HttpServletRequest request;
   @Cascading HttpServletResponse response;
   @Mocked EmailListService service;

   @Cascading ServletConfig servletConfig;

   @Before
   public void before() throws Exception
   {
      new NonStrictExpectations() {{
         servletConfig.getServletContext().getAttribute(EmailListService.KEY); result = service;
      }};

      servlet = new EmailListServlet();
      servlet.init(servletConfig);
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      new NonStrictExpectations() {{
         service.getListByName(null); result = new ServletException();
      }};

      servlet.doGet(request, response);
   }

   @Test
   public void doGetWithList(@Mocked final PrintWriter writer) throws Exception
   {
      new NonStrictExpectations() {{
         service.getListByName(anyString);
         result = asList("larry@stooge.com", "moe@stooge.com", "curley@stooge.com");
      }};

      servlet.doGet(request, response);

      new VerificationsInOrder() {{
         writer.println("larry@stooge.com");
         writer.println("moe@stooge.com");
         writer.println("curley@stooge.com");
         response.flushBuffer();
      }};
   }
}