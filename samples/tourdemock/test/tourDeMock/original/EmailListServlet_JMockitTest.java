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
   @Tested EmailListServlet servlet;
   @Injectable ServletConfig servletConfig;

   @Mocked HttpServletRequest request;
   @Mocked HttpServletResponse response;
   @Mocked EmailListService service;

   @Before
   public void before()
   {
      new NonStrictExpectations() {{
         servletConfig.getServletContext().getAttribute(EmailListService.KEY); result = service;
      }};
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      new Expectations() {{ service.getListByName(null); result = new ServletException(); }};

      servlet.doGet(request, response);
   }

   @Test
   public void doGetWithList(@Injectable final PrintWriter writer) throws Exception
   {
      new Expectations() {{
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