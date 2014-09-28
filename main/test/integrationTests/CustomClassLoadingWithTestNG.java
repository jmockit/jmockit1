/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import java.lang.reflect.*;
import java.net.*;

import org.testng.*;

import org.testng.annotations.*;

import static org.testng.Assert.assertEquals;

import mockit.*;

public final class CustomClassLoadingWithTestNG
{
   static final class IsolatedClassLoader extends URLClassLoader
   {
      IsolatedClassLoader() throws MalformedURLException
      {
         super(
            new URL[] {
               new URL("file:lib/testng-6.8.jar"),
               new URL("file:///github/jmockit1.org/jmockit.jar"),
               new URL("file:main/test-classes/")
            },
            IsolatedClassLoader.class.getClassLoader().getParent());
      }

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
      {
         Class<?> c = findLoadedClass(name);

         if (c == null) {
            try { c = findClass(name); } catch (ClassNotFoundException ignore) { c = getParent().loadClass(name); }
         }

         if (resolve) {
            resolveClass(c);
         }

         return c;
      }
   }

   public static void main(String[] args) throws Exception
   {
      ClassLoader cl = new IsolatedClassLoader();
      Class<?> thisClass = cl.loadClass(CustomClassLoadingWithTestNG.class.getName());
      Method runTestsMethod = thisClass.getDeclaredMethod("runTests");
      runTestsMethod.invoke(null);
   }

   @SuppressWarnings("unused")
   public static void runTests()
   {
      TestNG testNG = new TestNG();
      testNG.setServiceLoaderClassLoader((URLClassLoader) testNG.getClass().getClassLoader());

      Class<?>[] testClasses = {TestNGTests.class};
      testNG.setTestClasses(testClasses);

      testNG.run();
   }

   public static final class TestNGTests
   {
      @BeforeClass
      public void beforeAllTests()
      {
         assertEquals(new SomeClass().doSomething("123"), 123);
      }

      @AfterClass
      public void afterAllTests()
      {
         assertEquals(new SomeClass().doSomething("45"), 45);
      }

      @Test
      public void doNothing()
      {
         System.out.println("Nothing done");
      }

      @Test
      public void useMockUpsAPI()
      {
         assertEquals(new SomeClass().doSomething("23"), 23);

         new MockUp<SomeClass>() {
            @Mock
            int doSomething(String s) { return s.length(); }
         };

         assertEquals(new SomeClass().doSomething("Abc"), 3);
         assertEquals(new SomeClass().doSomething("Testing 123"), 11);
      }

      @Test
      public void useExpectationsAPI()
      {
         final SomeClass sc = new SomeClass();

         new Expectations(sc) {{
            sc.doSomething(anyString);
            result = 123;
         }};

         assertEquals(sc.doSomething("testing"), 123);
      }

      @Test
      public void useVerificationsAPIWithMockParameter(@Mocked final SomeClass mock)
      {
         new Expectations() {{
            mock.doSomething(anyString);
            result = 45;
         }};

         assertEquals(mock.doSomething("testing"), 45);
      }
   }

   static class SomeClass
   {
      int doSomething(String s) { return s == null ? -1 : Integer.parseInt(s); }
   }
}