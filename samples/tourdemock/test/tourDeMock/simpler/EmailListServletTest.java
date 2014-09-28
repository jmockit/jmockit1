/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.simpler;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import static java.util.Arrays.*;

import tourDeMock.original.service.*;
import tourDeMock.simpler.service.EmailListService;

import org.junit.*;

import mockit.*;

public final class EmailListServletTest
{
   @Tested EmailListServlet servlet;
   @Mocked HttpServletRequest request;
   @Mocked EmailListService emailListService;

   @Test(expected = ServletException.class)
   public void doGetWithoutList() throws Exception
   {
      new Expectations() {{ emailListService.getListByName(null); result = new EmailListNotFound(); }};

      servlet.doGet(request, null);
   }

   @Test
   public void doGetWithList(@Mocked final HttpServletResponse response, @Injectable final PrintWriter writer)
      throws Exception
   {
      new Expectations() {{
         emailListService.getListByName(anyString);
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