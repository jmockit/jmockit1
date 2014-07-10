/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.original;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.runner.*;

import org.unitils.*;
import org.unitils.mock.*;
import org.unitils.mock.core.*;
import tourDeMock.original.service.*;

@RunWith(UnitilsJUnit4TestClassRunner.class)
public final class EmailListServlet_UnitilsTest
{
   EmailListServlet servlet;

   Mock<HttpServletRequest> request;
   Mock<HttpServletResponse> response;
   Mock<EmailListService> emailListService;

   @Before
   public void before() throws Exception
   {
      MockObject<ServletConfig> servletConfig = new MockObject<>(ServletConfig.class, this);
      servletConfig.returns(emailListService).getServletContext().getAttribute(EmailListService.KEY);

      servlet = new EmailListServlet();
      servlet.init(servletConfig.getMock());
   }

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      emailListService.raises(new EmailListNotFound()).getListByName(null);

      servlet.doGet(request.getMock(), response.getMock());
   }

   @Test
   public void doGetWithList() throws Exception
   {
      Mock<PrintWriter> writer = new MockObject<>(PrintWriter.class, this);

      List<String> emails = asList("larry@stooge.com", "moe@stooge.com", "curley@stooge.com");
      emailListService.returns(emails).getListByName(null);

      response.returns(writer).getWriter();

      servlet.doGet(request.getMock(), response.getMock());

      writer.assertInvokedInSequence().println("larry@stooge.com");
      writer.assertInvokedInSequence().println("moe@stooge.com");
      writer.assertInvokedInSequence().println("curley@stooge.com");
      response.assertInvokedInSequence().flushBuffer();
   }
}