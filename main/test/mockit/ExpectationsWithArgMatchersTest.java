/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.security.cert.*;
import java.util.*;

import static java.util.Arrays.*;
import static java.util.Collections.singletonList;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

import static mockit.ExpectationsWithArgMatchersTest.Delegates.*;
import static org.hamcrest.CoreMatchers.*;

public final class ExpectationsWithArgMatchersTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedParameters")
   static class Collaborator
   {
      void setValue(int value) {}
      void setValue(double value) {}
      void setValue(float value) {}
      void setValue(String value) {}
      void setValues(char c, boolean b) {}
      void setValues(String[] values) {}
      void setTextualValues(Collection<String> values) {}
      void doSomething(Integer i) {}
      boolean doSomething(String s) { return false; }

      final void simpleOperation(int a, String b, Date c) {}

      void setValue(Certificate cert) {}
      void setValue(Exception ex) {}

      String useObject(Object arg) { return ""; }
   }

   @Mocked Collaborator mock;

   static final class CollectionElementDelegate<T> implements Delegate<Collection<T>>
   {
      private final T item;
      CollectionElementDelegate(T item) { this.item = item; }
      @SuppressWarnings("unused") boolean hasItem(Collection<T> items) { return items.contains(item); }
   }

   static final class Delegates
   {
      static <T> Delegate<Collection<T>> collectionElement(T item) { return new CollectionElementDelegate<T>(item); }
   }

   @Test
   public void expectInvocationsWithNamedDelegateMatcher()
   {
      new Expectations() {{
         mock.setTextualValues(with(collectionElement("B")));
      }};

      List<String> values = asList("a", "B", "c");
      mock.setTextualValues(values);
   }

   @Test
   public void expectInvocationsWithHamcrestMatcher()
   {
      new Expectations() {{
         mock.setTextualValues(this.<Collection<String>>withArgThat(hasItem("B")));
      }};

      List<String> values = asList("a", "B", "c");
      mock.setTextualValues(values);
   }

   @Test
   public void expectInvocationWithMatcherContainingAnotherMatcher()
   {
      new Expectations() {{ mock.setValue(withArgThat(equalTo(3))); }};

      mock.setValue(3);
   }

   @Test
   public void useMockedMethodBeforeRecordingExpectationWithArgumentMatcher()
   {
      assertFalse(mock.doSomething("abc"));

      new Expectations() {{
         mock.doSomething(anyString);
         result = true;
      }};

      assertTrue(mock.doSomething("xyz"));
      assertTrue(mock.doSomething("abc"));
   }

   @Test
   public void recordExpectationsUsingTheAnyFieldsForParameterOfTypeObject()
   {
      new Expectations() {{
         mock.useObject(anyString); result = "String";
         mock.useObject(anyInt); result = "int";
         mock.useObject(anyByte); result = "byte";
         mock.useObject(anyShort); result = "short";
         mock.useObject(anyLong); result = "long";
         mock.useObject(anyBoolean); result = "boolean";
         mock.useObject(anyChar); result = "char";
         mock.useObject(anyFloat); result = "float";
         mock.useObject(anyDouble); result = "double";
         mock.useObject(any); result = "Object";
      }};

      assertInvocationsWithArgumentsOfDifferentTypesToMethodAcceptingAnyObject();
   }

   void assertInvocationsWithArgumentsOfDifferentTypesToMethodAcceptingAnyObject()
   {
      assertEquals("String", mock.useObject("test"));
      assertEquals("String", mock.useObject(null)); // uses the first recorded expectation, since they all match null
      assertEquals("int", mock.useObject(2));
      assertEquals("Object", mock.useObject(new Object()));
      assertEquals("byte", mock.useObject((byte) -3));
      assertEquals("short", mock.useObject((short) 4));
      assertEquals("long", mock.useObject(-5L));
      assertEquals("boolean", mock.useObject(true));
      assertEquals("boolean", mock.useObject(false));
      assertEquals("char", mock.useObject('A'));
      assertEquals("float", mock.useObject(-1.5F));
      assertEquals("double", mock.useObject(23.456));
   }

   @Test
   public void recordExpectationsUsingTheWithAnyMethodForParameterOfTypeObject()
   {
      new Expectations() {{
         mock.useObject(withAny("a")); result = "String";
         mock.useObject(withAny(2)); result = "int";
         mock.useObject(withAny((byte) 3)); result = "byte";
         mock.useObject(withAny((short) 4)); result = "short";
         mock.useObject(withAny(5L)); result = "long";
         mock.useObject(withAny(true)); result = "boolean";
         mock.useObject(withAny('\0')); result = "char";
         mock.useObject(withAny(0.41F)); result = "float";
         mock.useObject(withAny(0.41)); result = "double";
         mock.useObject(withAny(new Object())); result = "Object";
      }};

      assertInvocationsWithArgumentsOfDifferentTypesToMethodAcceptingAnyObject();
   }

   @Test
   public void declareFieldInExpectationBlockWithNameHavingSamePrefixAsArgumentMatchingField()
   {
      new Expectations() {
         final Integer anyValue = 1;

         {
            mock.setValue(anyValue);
         }
      };

      mock.setValue(1);
   }

   @Test
   public void declareMethodInExpectationBlockWithNameHavingSamePrefixAsArgumentMatchingMethod()
   {
      final List<Integer> values = new ArrayList<Integer>();

      new Expectations() {
         {
            mock.setValues(withEqual('c'), anyBoolean);
            mock.setValue(withCapture(values));
         }

         char withEqual(char c) { return c; }
      };

      mock.setValues('c', true);
      final Collaborator col = new Collaborator();
      col.setValue(1);

      assertEquals(singletonList(1), values);

      new Verifications() {{
         int i;
         mock.setValue(i = withCapture());
         assertEquals(1, i);

         List<Collaborator> collaborators = withCapture(new Collaborator());
         assertSame(col, collaborators.get(0));
      }};
   }

   // "Missing invocations" ///////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void expectInvocationWithSameMockInstanceButReplayWithNull(
      // This class defines an abstract "toString" override, which initially was erroneously
      // mocked, causing a new expectation to be created during replay:
      @Mocked final Certificate cert)
   {
      new Expectations() {{
         mock.setValue(withSameInstance(cert)); times = 1;
      }};

      mock.setValue((Certificate) null);

      thrown.expect(MissingInvocation.class);
   }

   @Test
   public void expectInvocationWithMatcherWhichInvokesMockedMethod()
   {
      new Expectations() {{
         mock.setValue(with(new Delegate<Integer>() {
            @Mock boolean validateAsPositive(int value)
            {
               // Invoking mocked method caused ConcurrentModificationException (bug fixed):
               mock.simpleOperation(1, "b", null);
               return value > 0;
            }
         }));
      }};

      mock.setValue(-3);

      thrown.expect(MissingInvocation.class);
   }

   // Verifications ///////////////////////////////////////////////////////////////////////////////////////////////////

   class ReusableMatcher implements Delegate<Integer> {
      @Mock final boolean isPositive(int i) { return i > 0; }
   }

   @Test
   public void extendingAReusableArgumentMatcher()
   {
      mock.setValue(5);
      mock.setValue(123);

      new Verifications() {{
         mock.setValue(with(new ReusableMatcher() {}));
         times = 2;
      }};
   }
}
