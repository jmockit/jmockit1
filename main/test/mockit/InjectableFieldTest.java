/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.net.*;
import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;

public final class InjectableFieldTest
{
   static class Base
   {
      protected int getValue() { return 1; }
   }

   static class Foo extends Base
   {
      void doSomething(String s) { throw new RuntimeException(s); }
      int getAnotherValue() { return 2; }
      Boolean getBooleanValue() { return true; }
      final List<Integer> getList() { return null; }
      static String doSomethingElse() { return ""; }
   }

   @Injectable Foo foo;

   @Before
   public void recordCommonExpectations()
   {
      new Expectations() {{
         foo.getValue(); result = 12;
         foo.getAnotherValue(); result = 123;
      }};

      assertEquals(123, foo.getAnotherValue());
      assertEquals(12, foo.getValue());
      assertEquals(1, new Base().getValue());
      assertEquals(2, new Foo().getAnotherValue());
   }

   @Test
   public void cascadeOneLevel()
   {
      try {
         new Foo().doSomething("");
         fail();
      }
      catch (RuntimeException ignore) {}

      new Expectations() {{ foo.doSomething("test"); times = 1; }};

      assertEquals(123, foo.getAnotherValue());
      assertFalse(foo.getBooleanValue());
      assertTrue(foo.getList().isEmpty());

      foo.doSomething("test");
   }

   @Test
   public void overrideExpectationRecordedInBeforeMethod()
   {
      new Expectations() {{ foo.getAnotherValue(); result = 45; }};

      assertEquals(45, foo.getAnotherValue());
      foo.doSomething("sdf");
   }

   @Test
   public void partiallyMockClassWithoutAffectingInjectableInstances()
   {
      assertEquals("", Foo.doSomethingElse());

      new Expectations(Foo.class) {{
         Foo.doSomethingElse(); result = "test";
      }};

      assertEquals("test", Foo.doSomethingElse());
      assertEquals(12, foo.getValue());
      foo.doSomething("");
   }

   @Test
   public void partiallyMockInstanceWithoutAffectingInjectableInstances()
   {
      final Foo localFoo = new Foo();

      new Expectations(localFoo) {{
         localFoo.getAnotherValue(); result = 3;
         Foo.doSomethingElse(); result = "test";
      }};

      assertEquals(3, localFoo.getAnotherValue());
      assertEquals(123, foo.getAnotherValue());
      assertEquals(2, new Foo().getAnotherValue());
      assertEquals("test", Foo.doSomethingElse());
      foo.doSomething("");
   }

   @Test
   public void partiallyMockJREClassWhileHavingInjectableInstancesOfSameClassAsWell(
      @Injectable final InetAddress localHost, @Injectable final InetAddress remoteHost)
      throws Exception
   {
      new Expectations(InetAddress.class) {{
         InetAddress.getLocalHost(); result = localHost;
         InetAddress.getByName(anyString); result = remoteHost;

         localHost.getCanonicalHostName(); result = "localhost";
      }};

      assertSame(localHost, InetAddress.getLocalHost());
      assertSame(remoteHost, InetAddress.getByName("remote"));
      assertEquals("localhost", localHost.getCanonicalHostName());
      assertNull(remoteHost.getCanonicalHostName());
      foo.doSomething(null);

      new Verifications() {{
         remoteHost.getCanonicalHostName();
      }};
   }

   @Injectable int primitiveInt = 123;
   @Injectable Integer wrapperInt = 45;
   @Injectable String string = "Abc";

   @Test
   public void useNonMockableInjectablesWithValuesProvidedThroughFieldAssignment()
   {
      assertEquals(123, primitiveInt);
      assertEquals(45, wrapperInt.intValue());
      assertEquals("Abc", string);
   }

   @Injectable int defaultInt;
   @Injectable Integer nullInteger;
   @Injectable String nullString;
   @Injectable String emptyString = "";

   @Test
   public void useNullAndEmptyInjectablesOfNonMockableTypes()
   {
      assertEquals(0, defaultInt);
      assertNull(nullInteger);
      assertNull(nullString);
      assertTrue(emptyString.isEmpty());
   }
}
