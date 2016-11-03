/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.inject.*;
import javax.naming.*;
import javax.sql.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithConstructorDI4Test
{
   static class GenericClass<T> { T doSomething() { return null; } }

   @SuppressWarnings("FieldMayBeFinal")
   public static final class TestedClass
   {
      final GenericClass<String> go;
      final List<Integer> values;
      final Callable<Number> action1;
      private Callable<Number> action2;
      private Callable<Number> action3;
      private DataSource database;

      public TestedClass(GenericClass<String> go, List<Integer> values, Callable<Number>... actions)
      {
         this.go = go;
         this.values = values;
         action1 = actions[0];
         if (actions.length > 1) action2 = actions[1];
         if (actions.length > 2) action3 = actions[2];

         try {
            //noinspection JNDIResourceOpenedButNotSafelyClosed
            InitialContext context = new InitialContext();
            database = (DataSource) context.lookup("testDB");
            context.close();
         }
         catch (NamingException e) { throw new RuntimeException(e); }
      }
   }

   @Tested TestedClass tested;
   @Injectable Callable<Number> action1;
   @Injectable final GenericClass<String> mockGO = new GenericClass<String>(); // still mocked
   @Injectable final List<Integer> numbers = asList(1, 2, 3); // not mocked when interface
   @Mocked InitialContext jndiContext;
   @Mocked DataSource testDB;

   @Before
   public void recordCommonExpectations() throws Exception
   {
      new Expectations() {{ mockGO.doSomething(); result = "test"; minTimes = 0; }};
      new Expectations() {{ jndiContext.lookup("testDB"); result = testDB; }};
   }

   @Test
   public void exerciseTestedObjectWithValuesInjectedFromMockFields()
   {
      assertNotNull(tested.go);
      assertEquals(asList(1, 2, 3), tested.values);
      assertSame(action1, tested.action1);
      assertEquals("test", mockGO.doSomething());
      assertNull(new GenericClass<String>().doSomething());
   }

   @Test
   public void exerciseTestedObjectWithValuesInjectedFromMockParameters(
      @Injectable Callable<Number> action2, @Injectable Callable<Number> action3)
   {
      assertNotNull(tested.go);
      assertEquals(asList(1, 2, 3), tested.values);
      assertSame(action1, tested.action1);
      assertSame(action2, tested.action2);
      assertSame(action3, tested.action3);
      assertEquals("test", mockGO.doSomething());
      assertNull(new GenericClass().doSomething());
   }

   @Test
   public void useMockedJREClassesDuringTestedObjectCreation(@Mocked File fileMock)
   {
      assertNotNull(tested.database);
      mockGO.doSomething();
   }

   static class TestedClass3
   {
      final String text;
      final Runnable dependency;
      @Inject GenericClass<Integer> otherDep;

      TestedClass3(String text, Runnable dependency)
      {
         this.text = text;
         this.dependency = dependency;
      }
   }

   @Tested TestedClass3 tested7;
   @Injectable final String text = null;
   @Injectable final Runnable dependency = null;
   @Injectable final GenericClass<Integer> otherDep = null;

   @Test
   public void injectNullsThroughConstructorParametersAndIntoRequiredField()
   {
      assertNull(tested7.text);
      assertNull(tested7.dependency);
      assertNull(tested7.otherDep);
   }
}
