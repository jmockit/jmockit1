/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.mockitousage;

import java.util.*;

import static java.util.Arrays.asList;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;
import mockit.internal.*;

import static org.hamcrest.Matchers.*;

/**
 * Note how the number of <em>uses</em> of each mocking API (considering methods/constructors called, fields accessed,
 * and annotations applied) is usually smaller in a JMockit test when compared to the equivalent Mockito test, and never
 * larger.
 * <p/>
 * Depending on code formatting style, though, JMockit tests may take more lines of code than the equivalent Mockito
 * tests, because of the embedded "code blocks" created to record and verify expectations.
 * This syntactical difference tends to make JMockit tests taller (more lines) but narrower (shorter lines), when
 * compared to similar tests written with APIs which rely on method chaining.
 * (Other API innovations such as the "<code>any</code>" argument matching <em>fields</em> also contribute to less test
 * code, by avoiding lots of pointless parentheses.)
 * <p/>
 * Finally, the use of separate code blocks in JMockit tests provides a couple of nice readability gains:
 * 1) it clearly demarcates the special calls made on mock objects to record/verify expectations, separating them from
 * "real" calls in the test method (no need, therefore, to add a comment before a bunch of such calls);
 * and 2) it allows said blocks to be automatically <em>collapsed</em> by a Java IDE, causing the test method to appear
 * significantly smaller at first, while allowing the user to see the code inside a block by simply hovering the mouse
 * cursor over it.
 */
@SuppressWarnings("unused")
public final class JavadocExamples_JMockit_Test
{
   @Mocked List<String> mockedList;

   @Test // Uses of JMockit API: 2
   public void verifyBehavior(@Mocked final MockedClass mock)
   {
      // Mock is used (replay phase):
      mock.doSomething("one", true);
      mock.someMethod("test");

      // Invocations to mock are verified (verify phase):
      new Verifications() {{
         mock.doSomething("one", true);
         mock.someMethod("test");
      }};
   }

   @Test // Uses of JMockit API: 4
   public void stubInvocations(@Mocked final MockedClass mock)
   {
      new Expectations() {{
         mock.getItem(0); result = "first";
         mock.getItem(1); result = new RuntimeException();
      }};

      assertEquals("first", mock.getItem(0));

      try {
         mock.getItem(1);
      }
      catch (RuntimeException ignore) {
         // OK
      }

      assertNull(mock.getItem(999));
   }

   @Test // Uses of JMockit API: 3
   public void stubAndVerifyInvocation()
   {
      // A recorded expectation is expected to occur at least once, by default.
      new Expectations() {{ mockedList.get(0); result = "first"; }};

      assertEquals("first", mockedList.get(0));

      // Note that verifying a stubbed invocation isn't "just redundant" if the test cares that the
      // invocation occurs at least once. If this is the case, then it's not safe to expect the test
      // to break without an explicit verification, because the method under test may never call the
      // stubbed one, and that would be a bug that the test should detect.
   }

   @Test // Uses of JMockit API: 3
   public void stubAndVerifyInvocationWithoutRepeatingItInExpectationAndVerificationBlocks()
   {
      new Expectations() {{
         // Notice that this can't be done in Mockito, which requires the repetition of
         // "mockedList.get(0);" in the verification phase.
         mockedList.get(0); result = "first"; times = 1;
      }};

      assertEquals("first", mockedList.get(0));
   }

   @Test // Uses of JMockit API: 8
   public void useArgumentMatchers()
   {
      new Expectations() {{
         // Using built-in matchers:
         mockedList.get(anyInt); result = "element";

         // Using Hamcrest matchers:
         mockedList.get(withArgThat(is(equalTo(5)))); result = new IllegalArgumentException(); minTimes = 0;
         mockedList.contains(withArgThat(hasProperty("bytes"))); result = true;
         mockedList.containsAll(withArgThat(hasSize(2))); result = true;
      }};

      assertEquals("element", mockedList.get(999));
      assertTrue(mockedList.contains("abc"));
      assertTrue(mockedList.containsAll(asList("a", "b")));

      new Verifications() {{ mockedList.get(anyInt); }};
   }

   @Test // Uses of JMockit API: 5
   public void customArgumentMatcherUsingNamedClass()
   {
      class IsListOfTwoElements implements Delegate<List<String>> {
         boolean matches(List<?> list) { return list.size() == 2; }
      }

      new Expectations() {{
         mockedList.addAll(with(new IsListOfTwoElements())); result = true;
         times = 1;
      }};

      mockedList.addAll(asList("one", "two"));

      // No need to re-verify the invocation of "addAll" here.
   }

   @Test // Uses of JMockit API: 3
   public void customArgumentMatcherUsingAnonymousClass()
   {
      mockedList.addAll(asList("one", "two"));

      new Verifications() {{
         mockedList.addAll(with(new Delegate<List<String>>() {
            boolean matches(List<?> list) { return list.size() == 2; }
         }));
      }};
   }

   @Test // Uses of JMockit API: 8
   public void verifyNumberOfInvocations()
   {
      // Using mock:
      mockedList.add("once");

      mockedList.add("twice");
      mockedList.add("twice");

      mockedList.add("three times");
      mockedList.add("three times");
      mockedList.add("three times");

      new Verifications() {{
         // Following two verifications work exactly the same:
         mockedList.add("once"); // minTimes == 1 is the default
         mockedList.add("once"); times = 1;

         // Verifies exact number of invocations:
         mockedList.add("twice"); times = 2;
         mockedList.add("three times"); times = 3;

         // Verifies no invocations occurred:
         mockedList.add("never happened"); times = 0;

         // Verifies min/max number of invocations:
         mockedList.add("three times"); minTimes = 1;
         mockedList.add("three times"); minTimes = 2;
         mockedList.add("three times"); maxTimes = 5;
      }};
   }

   @Test(expected = RuntimeException.class) // Uses of JMockit API: 2
   public void stubVoidMethodsWithExceptions()
   {
      new Expectations() {{
         // void/non-void methods are handled the same way, with a consistent API:
         mockedList.clear(); result = new RuntimeException();
      }};

      mockedList.clear();
   }

   @Test // Uses of JMockit API: 3
   public void verifyInOrder(@Mocked final List<String> firstMock, @Mocked final List<String> secondMock)
   {
      // Using mocks:
      firstMock.add("was called first");
      secondMock.add("was called second");

      new VerificationsInOrder() {{
         // Verifies that firstMock was called before secondMock:
         firstMock.add("was called first");
         secondMock.add("was called second");
      }};
   }

   @Test // Uses of JMockit API: 4
   public void verifyThatInvocationsNeverHappened(@Mocked List<String> mockTwo, @Mocked List<String> mockThree)
   {
      // Using mocks - only mockedList is invoked:
      mockedList.add("one");

      // Verify that the two other mocks were never invoked.
      new FullVerifications() {{
         // Ordinary verification:
         mockedList.add("one");

         // Verify that method was never called on a mock:
         mockedList.add("two"); times = 0;
      }};
   }

   @Test(expected = UnexpectedInvocation.class) // Uses of JMockit API: 2
   public void verifyThatInvocationsNeverHappenedWhenTheyDid(@Mocked List<String> mockTwo)
   {
      mockedList.add("one");
      mockTwo.size();

      new FullVerifications() {{ mockedList.add("one"); }};
   }

   @Test // Uses of JMockit API: 1
   public void verifyAllInvocations()
   {
      mockedList.add("one");
      mockedList.add("two");

      // Verify all invocations to mockedList.
      new FullVerifications() {{
         // Verifies first invocation:
         mockedList.add("one");

         // Verifies second (and last) invocation:
         mockedList.add("two");
      }};
   }

   @Test(expected = UnexpectedInvocation.class) // Uses of JMockit API: 1
   public void verifyAllInvocationsWhenMoreOfThemHappen()
   {
      mockedList.add("one");
      mockedList.add("two");
      mockedList.size();

      // Verify all invocations to mockedList.
      new FullVerifications() {{
         mockedList.add("one");
         mockedList.add("two");
      }};
   }

   @Test // Uses of JMockit API: 1
   public void verifyAllInvocationsInOrder()
   {
      mockedList.add("one");
      mockedList.size();
      mockedList.add("two");

      new FullVerificationsInOrder() {{
         mockedList.add("one");
         mockedList.size();
         mockedList.add("two");
      }};
   }

   @Test(expected = UnexpectedInvocation.class) // Uses of JMockit API: 1
   public void verifyAllInvocationsInOrderWhenMoreOfThemHappen()
   {
      mockedList.add("one");
      mockedList.add("two");
      mockedList.size();

      new FullVerificationsInOrder() {{
         mockedList.add("one");
         mockedList.add("two");
      }};
   }

   @Test(expected = MissingInvocation.class) // Uses of JMockit API: 1
   public void verifyAllInvocationsInOrderWithOutOfOrderVerifications()
   {
      mockedList.add("one");
      mockedList.add("two");

      new FullVerificationsInOrder() {{
         mockedList.add("two");
         mockedList.add("one");
      }};
   }

   @Test // Uses of JMockit API: 4
   public void stubbingConsecutiveCalls(@Mocked final Iterator<String> mock)
   {
      new Expectations() {{
         mock.next(); result = new IllegalStateException(); result = "foo";
      }};

      // First call: throws exception.
      try {
         mock.next();
         fail();
      }
      catch (IllegalStateException ignore) {
         // OK
      }

      // Second call: prints "foo".
      assertEquals("foo", mock.next());

      // Any consecutive call: prints "foo" as well.
      assertEquals("foo", mock.next());
   }

   @Test // Uses of JMockit API: 3
   public void stubbingConsecutiveCallsToReturnASequenceOfValues(@Mocked final MockedClass mock)
   {
      new Expectations() {{ mock.someMethod("some arg"); returns("one", "two", "three"); }};

      assertEquals("one", mock.someMethod("some arg"));
      assertEquals("two", mock.someMethod("some arg"));
      assertEquals("three", mock.someMethod("some arg"));
      assertEquals("three", mock.someMethod("some arg"));
   }

   @Test // Uses of JMockit API: 5
   public void stubbingWithCallbacksUsingDelegate(@Mocked final MockedClass mock)
   {
      new Expectations() {{
         mock.someMethod(anyString);
         result = new Delegate() {
            String delegate(String s) { return "called with arguments: " + s; }
         };
      }};

      assertEquals("called with arguments: foo", mock.someMethod("foo"));
   }

   @Test // Uses of JMockit API: 4
   public void stubbingWithCallbacksUsingMockUp()
   {
      final MockedClass mock = new MockedClass();

      new MockUp<MockedClass>() {
         @Mock
         String someMethod(Invocation inv, String s)
         {
            assertSame(mock, inv.getInvokedInstance());
            return "called with arguments: " + s;
         }
      };

      assertEquals("called with arguments: foo", mock.someMethod("foo"));
   }

   @Test // Uses of JMockit API: 7
   public void callingRealMethodFromDelegate(@Injectable final MockedClass mock)
   {
      new Expectations() {{
         mock.someMethod(anyString);
         result = new Delegate() {
            String delegate(Invocation invocation, String s)
            {
               String actualResult = invocation.proceed();
               return "Res=" + actualResult;
            }
         };
      }};

      assertEquals("Res=3", mock.someMethod("3"));
   }

   @Test // Uses of JMockit API: 2
   public void stubbingVoidMethods()
   {
      // The API is consistent, so this is the same as for non-void methods:
      new Expectations() {{ mockedList.clear(); result = new RuntimeException(); }};

      try {
         // Following throws RuntimeException:
         mockedList.clear();
         fail();
      }
      catch (RuntimeException ignore) {}
   }

   // Equivalent to "spyingOnRealObjects", but real implementations execute only on replay.
   @Test // Uses of JMockit API: 7
   public void dynamicPartialMocking()
   {
      final MockedClass mock = new MockedClass();

      // Mocks a real object:
      new Expectations(mock) {};

      // Optionally, you can record some invocations:
      new Expectations() {{ mock.getSomeValue(); result = 100; }};

      // When recording invocations on any mocked instance, partially mocked or not, real implementations
      // are never executed, so this call would never throw an exception:
      new Expectations() {{ mock.getItem(1); result = "an item"; }};

      // Using the mock calls real methods, except for calls which match recorded expectations:
      mock.doSomething("one", true);
      mock.doSomething("two", false);

      assertEquals("one", mock.getItem(0));
      assertEquals("an item", mock.getItem(1));
      assertEquals(100, mock.getSomeValue());

      // Optionally, you can verify invocations to the dynamically mocked types/objects:
      new Verifications() {{ // when verifying invocations, real implementations are never executed
         mock.doSomething("one", true);
         mock.doSomething("two", anyBoolean);
      }};
   }

   @Test // Uses of JMockit API: 3
   public void capturingArgumentForVerification(@Mocked final MockedClass mock)
   {
      mock.doSomething(new Person("John"));

      new Verifications() {{
         Person argument;
         mock.doSomething(argument = withCapture());
         assertEquals("John", argument.getName());
      }};
   }

   @Test // Uses of JMockit API: 3
   public void capturingArgumentsForVerification(@Mocked final MockedClass mock)
   {
      mock.doSomething(new Person("John"));
      mock.doSomething(new Person("Jane"));

      new Verifications() {{
         List<Person> capturedPeople = new ArrayList<>();

         mock.doSomething(withCapture(capturedPeople)); times = 2;

         assertEquals("John", capturedPeople.get(0).getName());
         assertEquals("Jane", capturedPeople.get(1).getName());
      }};
   }

   @Test // Uses of JMockit API: 4
   public void capturingMultipleArgumentsForVerification(@Mocked final MockedClass mock)
   {
      mock.doSomething("test", true);

      new Verifications() {{
         String captor1;
         boolean captor2;
         mock.doSomething(captor1 = withCapture(), captor2 = withCapture());

         assertEquals("test", captor1);
         assertTrue(captor2);
      }};
   }

   @Test // Uses of JMockit API: 4
   public void chainingMethodCallsWithCascading(@Mocked final MockedClass mock)
   {
      new Expectations() {{ mock.getPerson().getName(); result = "deep"; }};

      assertEquals("deep", mock.getPerson().getName());

      new Verifications() {{
         // Not likely to be useful often, but such verifications do work:
         mock.getPerson();
         mock.getPerson().getName();
      }};
   }

   @Test
   public void verificationIgnoringStubs(@Mocked final MockedClass mock, @Mocked final MockedClass mockTwo)
   {
      // Stubbings, with an invocation count constraint for later (automatic) verification:
      new Expectations() {{ mock.getItem(1); result = "ignored"; times = 1; }};

      // In tested code:
      mock.doSomething("a", true);
      mockTwo.someMethod("b");
      mock.getItem(1);

      // Verify all invocations, except those verified implicitly through recorded invocation
      // count constraints.
      // There is no support for ignoring stubbings that were not verified in any way.
      // That said, note the API does not require any code duplication.
      new FullVerifications() {{
         mock.doSomething("a", true);
         mockTwo.someMethod("b");
      }};
   }

   @Test
   public void verificationInOrderIgnoringStubs(@Mocked final MockedClass mock, @Mocked final MockedClass mockTwo)
   {
      // Stubbings, with an invocation count constraint for later (automatic) verification:
      new Expectations() {{ mock.getItem(1); result = "ignored"; times = 1; }};

      // In tested code:
      mock.doSomething("a", true);
      mockTwo.someMethod("b");
      mock.getItem(1);

      // Verify all invocations, except those verified implicitly through recorded invocation
      // count constraints.
      // There is no support for ignoring stubbings that were not verified in any way.
      // That said, note the API does not require any code duplication.
      new FullVerificationsInOrder() {{
         mock.doSomething("a", true);
         mockTwo.someMethod("b");
      }};
   }

   @Test // Uses of JMockit API: 2
   public void nonGreedyVerificationInOrder(@Mocked final MockedClass mock)
   {
      mock.someMethod("some arg");
      mock.someMethod("some arg");
      mock.someMethod("some arg");
      mock.doSomething("testing", true);
      mock.someMethod("some arg");

      new VerificationsInOrder() {{
         mock.someMethod("some arg"); minTimes = 2;
         mock.doSomething("testing", true);
      }};
   }

   @Test // Uses of JMockit API: 3
   public void returningElementsFromAList()
   {
      final List<String> list = asList("a", "b", "c");

      new Expectations() {{ mockedList.get(anyInt); result = list; }};

      assertEquals("a", mockedList.get(0));
      assertEquals("b", mockedList.get(1));
      assertEquals("c", mockedList.get(2));
      assertEquals("c", mockedList.get(3));
   }

   @Test // Uses of JMockit API: 5
   public void returningFirstArgument(@Mocked final MockedClass mock)
   {
      new Expectations() {{
         mock.someMethod(anyString);
         result = new Delegate() { String firstArg(String s) { return s; } };
      }};

      assertEquals("test", mock.someMethod("test"));
   }

   @Test // Uses of Mockito API: 5
   public void returningLastArgument()
   {
      new Expectations() {{
         mockedList.set(anyInt, anyString);
         result = new Delegate() { String lastArg(int i, String s) { return s; } };
      }};

      assertEquals("test", mockedList.set(1, "test"));
   }
}
