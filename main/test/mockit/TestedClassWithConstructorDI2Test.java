/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.net.*;
import javax.servlet.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithConstructorDI2Test
{
   public static final class TestedClass implements Servlet
   {
      static int counter;

      private ServletConfig config;
      private final Dependency dependency1;
      private final Dependency dependency2;
      private final Dependency dependency3;

      public TestedClass(Dependency dependency1, Runnable r, Dependency dependency2, Dependency dependency3)
      {
         this.dependency1 = dependency1;
         this.dependency2 = dependency2;
         this.dependency3 = dependency3;
         r.run();

         int i = dependency1.doSomething();
         assert i == 123;

         try {
            InetAddress localHost = InetAddress.getLocalHost();
            assert localHost.getHostName() == null;
         }
         catch (UnknownHostException e) {
            throw new IllegalStateException("InetAddress should be mocked", e);
         }
      }

      public int doSomeOperation()
      {
         return dependency1.doSomething() + dependency2.doSomething();
      }

      @Override public ServletConfig getServletConfig() { return config; }
      @Override public void service(ServletRequest req, ServletResponse res) {}
      @Override public String getServletInfo() { return null; }

      @Override
      public void init(ServletConfig cfg)
      {
         config = cfg;
         counter++;

         int i = dependency1.doSomething();
         assert i == 123;

         checkInetAddressMocking();
      }

      private void checkInetAddressMocking()
      {
         try {
            InetAddress inetAddress = InetAddress.getByName("testHost");
            assert inetAddress.getHostName() == null;
         }
         catch (UnknownHostException ignore) {
            counter = -1;
         }
      }

      @Override
      public void destroy()
      {
         counter++;
         checkInetAddressMocking();
      }
   }

   static class Dependency { int doSomething() { return -1; } }

   @Tested TestedClass tested;
   @Injectable Runnable task;
   @Injectable Dependency dependency1;
   @Injectable Dependency dependency2;
   @Injectable ServletConfig config;
   @Mocked InetAddress testHost;

   @Before
   public void resetCounter()
   {
      TestedClass.counter = 0;
      new Expectations() {{ dependency1.doSomething(); result = 123; }};
   }

   @Test
   public void exerciseTestedObjectWithDependenciesOfSameTypeInjectedThroughConstructor(
      @Injectable Dependency dependency3)
   {
      assertTestedObjectWasInitialized();
      assertSame(dependency3, tested.dependency3);

      new Expectations() {{
         dependency1.doSomething(); result = 23;
         dependency2.doSomething(); result = 5;
      }};

      assertEquals(28, tested.doSomeOperation());
   }

   @Test
   public void exerciseTestedObjectWithExtraInjectableParameter(
      @Injectable Dependency dependency3, @Injectable Dependency mock4)
   {
      assertTestedObjectWasInitialized();
      assertSame(dependency1, tested.dependency1);
      assertSame(dependency2, tested.dependency2);
      assertSame(dependency3, tested.dependency3);
   }

   void assertTestedObjectWasInitialized()
   {
      assertSame(config, tested.getServletConfig());
      assertEquals(1, TestedClass.counter);
   }

   @After
   public void verifyTestedObjectAfterEveryTest()
   {
      assertEquals(2, TestedClass.counter);
   }
}
