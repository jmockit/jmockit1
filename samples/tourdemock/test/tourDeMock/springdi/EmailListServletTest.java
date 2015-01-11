/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tourDeMock.springdi;

import java.util.*;
import javax.inject.*;
import javax.servlet.*;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.runner.*;
import static org.junit.Assert.*;

import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.*;
import tourDeMock.original.service.*;
import tourDeMock.springdi.service.EmailListService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public final class EmailListServletTest
{
   @Configuration
   static class ApplicationConfiguration
   {
      @Bean EmailListService emailListService() { return new MockEmailListService(); }
      @Bean EmailListServlet emailListServlet() { return new EmailListServlet(); }
   }

   static final String SEP = System.getProperty("line.separator");

   @Inject EmailListServlet servlet;
   MockHttpServletRequest request;
   MockHttpServletResponse response;

   @Before
   public void before()
   {
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

   private static final class MockEmailListService extends EmailListService
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
