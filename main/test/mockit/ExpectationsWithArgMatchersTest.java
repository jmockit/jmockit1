/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.security.cert.*;
import java.util.*;

import static java.util.Arrays.*;

import org.hamcrest.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.*;

import static mockit.ExpectationsWithArgMatchersTest.Delegates.*;
import static org.hamcrest.CoreMatchers.*;

public final class ExpectationsWithArgMatchersTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("unused")
   static class Collaborator
   {
      private void setValue(int value) {}
      void setValue(double value) {}
      void setValue(float value) {}
      void setValue(String value) {}
      void setValues(char c, boolean b) {}
      void setValues(String[] values) {}
      void setTextualValues(Collection<String> values) {}
      private void doSomething(Integer i) {}
      boolean doSomething(String s) { return false; }

      List<?> complexOperation(Object input1, Object... otherInputs)
      {
         return input1 == null ? Collections.emptyList() : asList(otherInputs);
      }

      final void simpleOperation(int a, String b, Date c) {}

      void setValue(Certificate cert) {}
      void setValue(Exception ex) {}

      String useObject(Object arg) { return ""; }
   }

   @Mocked Collaborator mock;

   @Test
   public void replayWithUnexpectedMethodArgument()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(" expected \"test\", got \"other\"");

      new StrictExpectations() {{ mock.simpleOperation(2, "test", null); }};

      mock.simpleOperation(2, "other", null);
   }

   @Test
   public void replayWithMultipleUnexpectedMethodArguments()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(" expected \"a\", got \"b\"");

      new StrictExpectations() {{ mock.setValues('a', true); }};

      mock.setValues('b', false);
   }

   @Test
   public void replayWithUnexpectedSecondMethodArgument()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(" expected true, got false");

      new StrictExpectations() {{ mock.setValues('a', true); }};

      mock.setValues('a', false);
   }

   @Test
   public void replayWithUnexpectedNullArgument()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("expected \"test\", got null");

      new StrictExpectations() {{ mock.simpleOperation(2, "test", null); }};

      mock.simpleOperation(2, null, null);
   }

   @Test
   public void replayWithUnexpectedMethodArgumentUsingMatcher()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("expected -1, got 1");

      new StrictExpectations() {{ mock.setValue(withEqual(-1)); }};

      mock.setValue(1);
   }

   @Test
   public void expectInvocationWithDifferentThanExpectedProxyArgument(@Mocked final Runnable mock2)
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("got null");

      new StrictExpectations() {{ mock.complexOperation(mock2); }};

      mock.complexOperation(null);
   }

   @Test
   public void expectInvocationWithAnyArgumentUsingField()
   {
      new StrictExpectations() {{ mock.setValue(anyInt); }};

      mock.setValue(3);
   }

   @Test
   public void expectInvocationToPrivateInstanceMethodUsingAnyFieldMatcher()
   {
      new StrictExpectations() {{ mock.doSomething(anyInt); }};

      mock.doSomething(3);
   }

   @Test
   public void expectInvocationWithAnyArgumentUsingMethod()
   {
      new StrictExpectations() {{ mock.setValue(withAny(1)); }};

      mock.setValue(3);
   }

   @Test
   public void expectInvocationWithEqualArgument()
   {
      new StrictExpectations() {{ mock.setValue(withEqual(3)); }};

      mock.setValue(3);
   }

   @Test
   public void expectInvocationWithEqualArrayArgument()
   {
      new StrictExpectations() {{ mock.setValues(withEqual(new String[] {"A", "bb", "cee"})); }};

      mock.setValues(new String[] {"A", "bb", "cee"});
   }

   @Test
   public void expectInvocationWithEqualDoubleArgument()
   {
      new StrictExpectations() {{ mock.setValue(withEqual(3.0, 0.01)); times = 3; }};

      mock.setValue(3.0);
      mock.setValue(3.01);
      mock.setValue(2.99);
   }

   @Test
   public void expectInvocationWithEqualFloatArgument()
   {
      new StrictExpectations() {{ mock.setValue(withEqual(3.0F, 0.01)); times = 3; }};

      mock.setValue(3.0F);
      mock.setValue(3.01F);
      mock.setValue(2.99F);
   }

   @Test
   public void expectInvocationWithEqualFloatArgumentButWithDifferentReplayValue()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(" within 0.01 of 3.0, got 3.02F");

      new StrictExpectations() {{ mock.setValue(withEqual(3.0F, 0.01)); }};

      mock.setValue(3.02F);
   }

   @Test
   public void expectInvocationWithNotEqualArgument()
   {
      new StrictExpectations() {{ mock.setValue(withNotEqual(3)); }};

      mock.setValue(4);
   }

   @Test
   public void expectInvocationWithInstanceOfClassFromGivenObject()
   {
      new StrictExpectations() {{
         mock.complexOperation("string");
         mock.complexOperation(withInstanceLike("string"));
      }};

      mock.complexOperation("string");
      mock.complexOperation("another string");
   }

   @Test
   public void expectInvocationWithInstanceOfGivenClass()
   {
      new StrictExpectations() {{ mock.complexOperation(withInstanceOf(long.class)); }};

      mock.complexOperation(5L);
   }

   @Test
   public void expectInvocationWithNullArgument()
   {
      new StrictExpectations() {{ mock.complexOperation(withNull()); }};

      mock.complexOperation(null);
   }

   @Test
   public void expectInvocationWithNotNullArgument()
   {
      new StrictExpectations() {{ mock.complexOperation(withNotNull()); }};

      mock.complexOperation(true);
   }

   @Test
   public void expectInvocationWithSameInstance()
   {
      new StrictExpectations() {{ mock.complexOperation(withSameInstance(45L)); }};

      mock.complexOperation(45L);
   }

   @Test
   public void expectInvocationWithSameMockInstanceButReplayWithNull(
      // This class defines an abstract "toString" override, which initially was erroneously
      // mocked, causing a new expectation to be created during replay:
      @Mocked final Certificate cert)
   {
      thrown.expect(MissingInvocation.class);

      new Expectations() {{
         mock.setValue(withSameInstance(cert)); times = 1;
      }};

      mock.setValue((Certificate) null);
   }

   @Test
   public void expectNotStrictInvocationWithMatcherWhichInvokesMockedMethod()
   {
      thrown.expect(MissingInvocation.class);

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
   }

   @Test
   public void expectStrictInvocationWithCustomMatcherButNeverReplay()
   {
      thrown.expect(MissingInvocation.class);

      new StrictExpectations() {{
         mock.doSomething(with(new Delegate<Integer>() {
            @Mock boolean test(Integer i) { return true; }
         }));
      }};
   }

   @Test
   public void expectInvocationWithSubstring()
   {
      new StrictExpectations() {{ mock.complexOperation(withSubstring("sub")); }};

      mock.complexOperation("abcsub\r\n123");
   }

   @Test
   public void expectInvocationWithPrefix()
   {
      new StrictExpectations() {{ mock.complexOperation(withPrefix("abc")); }};

      mock.complexOperation("abc\tsub\"123\"");
   }

   @Test
   public void expectInvocationWithSuffix()
   {
      new StrictExpectations() {{ mock.complexOperation(withSuffix("123")); }};

      mock.complexOperation("abcsub123");
   }

   @Test
   public void expectInvocationWithMatchForRegex()
   {
      new StrictExpectations() {{
         mock.complexOperation(withMatch("[a-z]+[0-9]*"));
         mock.complexOperation(withMatch("(?i)[a-z]+sub[0-9]*"));
      }};

      mock.complexOperation("abcsub123");
      mock.complexOperation("abcSuB123");
   }

   @Test
   public void expectInvocationWithMatchForRegexButWithNonMatchingArgument()
   {
      thrown.expect(UnexpectedInvocation.class);

      new StrictExpectations() {{ mock.complexOperation(withMatch("test")); }};

      mock.complexOperation("otherValue");
   }

   @Test
   public void expectInvocationWithUserProvidedMatcher()
   {
      new StrictExpectations() {{ mock.setValue(withArgThat(is(equalTo(3)))); }};

      mock.setValue(3);
   }

   @Test
   public void expectInvocationWithUserImplementedMatcherUsingHamcrestAPI()
   {
      new StrictExpectations() {{
         mock.complexOperation(withArgThat(new BaseMatcher<Integer>() {
            @Override
            public boolean matches(Object item)
            {
               Integer value = (Integer) item;
               return value >= 10 && value <= 100;
            }

            @Override
            public void describeTo(Description description)
            {
               description.appendText("between 10 and 100");
            }
         }));
      }};

      mock.complexOperation(28);
   }

   @Test
   public void expectInvocationsWithAnonymousDelegateMatchers()
   {
      new StrictExpectations() {{
         mock.setValue(with(new Delegate<Integer>() {
            @Mock boolean matches(int value)
            {
               return value >= 10 && value <= 100;
            }
         }));

         mock.setValue(with(new Delegate<Double>() {
            @Mock void validate(double value)
            {
               assertTrue("value outside of 20-80 range", value >= 20.0 && value <= 80.0);
            }
         }));
      }};

      new StrictExpectations() {{
         mock.setValue(with(new Delegate<String>() {
            @Mock boolean validLength(String value)
            {
               return value.length() >= 10 && value.length() <= 100;
            }
         }));

         mock.setValue(with(new Delegate<Float>() {
            @Mock boolean positive(float value) { return value > 0.0F; }
         }));
      }};

      new StrictExpectations() {{
         mock.setTextualValues(with(new Delegate<Collection<String>>() {
            @Mock boolean validSize(Collection<String> values) { return values.size() >= 1; }
         }));

         mock.setTextualValues(with(new Delegate<Collection<String>>() {
            @Mock
            boolean randomAccess(Iterable<String> values) { return values instanceof RandomAccess; }
         }));
      }};

      mock.setValue(28);
      mock.setValue(20.0);
      mock.setValue("Test 123 abc");
      mock.setValue(1.5F);

      List<String> values1 = asList("a", "B", "c");
      mock.setTextualValues(values1);

      Collection<String> values2 = asList("1", "2", "3");
      mock.setTextualValues(values2);
   }

   static final class Delegates
   {
      static <T> Delegate<Collection<T>> collectionElement(T item) { return new CollectionElementDelegate<T>(item); }
   }

   static final class CollectionElementDelegate<T> implements Delegate<Collection<T>>
   {
      private final T item;
      CollectionElementDelegate(T item) { this.item = item; }
      @SuppressWarnings("unused") boolean hasItem(Collection<T> items) { return items.contains(item); }
   }

   @Test
   public void expectInvocationsWithNamedDelegateMatcher()
   {
      new StrictExpectations() {{
         mock.setTextualValues(with(collectionElement("B")));
      }};

      List<String> values = asList("a", "B", "c");
      mock.setTextualValues(values);
   }

   @Test
   public void expectInvocationsWithHamcrestMatcher()
   {
      new StrictExpectations() {{
         mock.setTextualValues(this.<Collection<String>>withArgThat(hasItem("B")));
      }};

      List<String> values = asList("a", "B", "c");
      mock.setTextualValues(values);
   }

   @Test
   public void expectInvocationWithMatcherContainingAnotherMatcher()
   {
      new StrictExpectations() {{ mock.setValue(withArgThat(equalTo(3))); }};

      mock.setValue(3);
   }

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
   public void replayWithDifferentArgumentOfClassLackingEqualsMethod()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("argument class java.lang.RuntimeException has no \"equals\" method");

      new StrictExpectations() {{
         mock.setValue(new RuntimeException("Recorded"));
      }};

      mock.setValue(new RuntimeException("Replayed"));
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
}
