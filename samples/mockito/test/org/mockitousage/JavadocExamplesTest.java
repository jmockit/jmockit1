package org.mockitousage;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.runner.*;
import static org.junit.Assert.*;

import org.mockito.*;
import org.mockito.exceptions.verification.*;
import org.mockito.invocation.*;
import org.mockito.runners.*;
import org.mockito.stubbing.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.AdditionalAnswers.*;
import static org.mockito.Mockito.*;

/**
 * File created from code snippets in the official
 * <a href="http://docs.mockito.googlecode.com/hg/latest/org/mockito/Mockito.html">Mockito documentation</a>,
 * with some minor changes.
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public final class JavadocExamplesTest
{
   @Mock List<String> mockedList;

   @Test // Uses of Mockito API: 3
   public void verifyingBehavior()
   {
      // Mock creation (for interfaces or concrete classes):
      MockedClass mock = mock(MockedClass.class);

      //using mock object
      mock.doSomething("one", true);
      mock.someMethod("test");

      //verification
      verify(mock).doSomething("one", true);
      verify(mock).someMethod("test");
   }

   @Test // Uses of Mockito API: 5
   public void stubbing()
   {
      MockedClass mock = mock(MockedClass.class);

      //stubbing
      when(mock.getItem(0)).thenReturn("first");
      when(mock.getItem(1)).thenThrow(new RuntimeException());

      assertEquals("first", mock.getItem(0));

      try {
         mock.getItem(1);
      }
      catch (RuntimeException ignore) {
         // OK
      }

      assertNull(mock.getItem(999));
   }

   @Test // Uses of Mockito API: 3
   public void stubbingAndVerifying()
   {
      when(mockedList.get(0)).thenReturn("first");

      assertEquals("first", mockedList.get(0));

      // Although it is possible to verify a stubbed invocation, usually it's just redundant.
      // If your code cares what get(0) returns then something else breaks (often before even
      // verify() gets executed).
      // If your code doesn't care what get(0) returns then it should not be stubbed.
      verify(mockedList).get(0);
   }

   @Test // Uses of Mockito API: 8
   public void argumentMatchers()
   {
      //stubbing using built-in anyInt() argument matcher
      when(mockedList.get(anyInt())).thenReturn("element");

      //stubbing using hamcrest:
      when(mockedList.get(intThat(is(equalTo(5))))).thenThrow(new IllegalArgumentException());
      when(mockedList.contains(argThat(hasProperty("bytes")))).thenReturn(true);
      when(mockedList.containsAll(argThat(hasSize(2)))).thenReturn(true);

      assertEquals("element", mockedList.get(999));
      assertTrue(mockedList.contains("abc"));
      assertTrue(mockedList.containsAll(asList("a", "b")));

      //you can also verify using an argument matcher
      verify(mockedList).get(anyInt());
   }

   @Test // Uses of Mockito API: 6
   public void customArgumentMatcherUsingNamedClass()
   {
      class IsListOfTwoElements extends ArgumentMatcher<List<String>> {
         @Override
         public boolean matches(Object list) { return ((List<String>) list).size() == 2; }
      }

      when(mockedList.addAll(argThat(new IsListOfTwoElements()))).thenReturn(true);

      mockedList.addAll(asList("one", "two"));

      verify(mockedList).addAll(argThat(new IsListOfTwoElements()));
   }

   @Test // Uses of Mockito API: 3
   public void customArgumentMatcherUsingAnonymousClass()
   {
      mockedList.addAll(asList("one", "two"));

      verify(mockedList).addAll(argThat(new ArgumentMatcher<List<String>>() {
         @Override
         public boolean matches(Object list) { return ((List<String>) list).size() == 2; }
      }));
   }

   @Test // Uses of Mockito API: 15
   public void verifyingNumberOfInvocations()
   {
      //using mock
      mockedList.add("once");

      mockedList.add("twice");
      mockedList.add("twice");

      mockedList.add("three times");
      mockedList.add("three times");
      mockedList.add("three times");

      //following two verifications work exactly the same - times(1) is used by default
      verify(mockedList).add("once");
      verify(mockedList, times(1)).add("once");

      //exact number of invocations verification
      verify(mockedList, times(2)).add("twice");
      verify(mockedList, times(3)).add("three times");

      //verification using never(). never() is an alias to times(0)
      verify(mockedList, never()).add("never happened");

      //verification using atLeast()/atMost()
      verify(mockedList, atLeastOnce()).add("three times");
      verify(mockedList, atLeast(2)).add("three times");
      verify(mockedList, atMost(5)).add("three times");
   }

   @Test(expected = RuntimeException.class) // Uses of Mockito API: 2
   public void stubbingVoidMethodsWithExceptions()
   {
      // "thenThrow(...)" is not applicable for void methods, so "doThrow" is used;
      // note also that in this situation it's "when(mock)", not "when(mock.someMethod(...))"
      doThrow(new RuntimeException()).when(mockedList).clear();

      //following throws RuntimeException:
      mockedList.clear();
   }

   @Test // Uses of Mockito API: 5
   public void verificationInOrder()
   {
      List<String> firstMock = mock(List.class);
      List<String> secondMock = mock(List.class);

      //using mocks
      firstMock.add("was called first");
      secondMock.add("was called second");

      //create inOrder object passing any mocks that need to be verified in order
      InOrder inOrder = inOrder(firstMock, secondMock);

      //following will make sure that firstMock was called before secondMock
      inOrder.verify(firstMock).add("was called first");
      inOrder.verify(secondMock).add("was called second");
   }

   @Test // Uses of Mockito API: 6
   public void verifyingThatInteractionsNeverHappened()
   {
      List<String> mockTwo = mock(List.class);
      List<String> mockThree = mock(List.class);

      //using mocks - only mockedList is interacted
      mockedList.add("one");

      //ordinary verification
      verify(mockedList).add("one");

      //verify that method was never called on a mock
      verify(mockedList, never()).add("two");

      //verify that other mocks were not interacted
      verifyZeroInteractions(mockTwo, mockThree);
   }

   @Test(expected = NoInteractionsWanted.class) // Uses of Mockito API: 3
   public void verifyingThatInteractionsNeverHappenedWhenTheyDid()
   {
      List<String> mockTwo = mock(List.class);

      mockedList.add("one");
      mockTwo.size();

      verify(mockedList).add("one");

      verifyZeroInteractions(mockTwo);
   }

   @Test // Uses of Mockito API: 3
   public void verifyingAllInteractions()
   {
      mockedList.add("one");
      mockedList.add("two");

      // Verifies first interaction:
      verify(mockedList).add("one");

      // Verifies second (and last) interaction:
      verify(mockedList).add("two");

      // Verify that no other interactions happened to mockedList:
      verifyNoMoreInteractions(mockedList);
   }

   @Test(expected = NoInteractionsWanted.class) // Uses of Mockito API: 3
   public void verifyingAllInteractionsWhenMoreOfThemHappen()
   {
      mockedList.add("one");
      mockedList.add("two");
      mockedList.size();

      verify(mockedList).add("one");
      verify(mockedList).add("two");
      verifyNoMoreInteractions(mockedList);
   }

   @Test // Uses of Mockito API: 4
   public void stubbingConsecutiveCalls()
   {
      Iterator<String> mock = mock(Iterator.class);

      when(mock.next()).thenThrow(new IllegalStateException()).thenReturn("foo");

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

      // Any consecutive call: prints "foo" as well (last stubbing wins).
      assertEquals("foo", mock.next());
   }

   @Test // Uses of Mockito API: 3
   public void stubbingConsecutiveCallsToReturnASequenceOfValues()
   {
      MockedClass mock = mock(MockedClass.class);

      when(mock.someMethod("some arg")).thenReturn("one", "two", "three");

      assertEquals("one", mock.someMethod("some arg"));
      assertEquals("two", mock.someMethod("some arg"));
      assertEquals("three", mock.someMethod("some arg"));
      assertEquals("three", mock.someMethod("some arg"));
   }

   @Test // Uses of Mockito API: 8
   public void stubbingWithCallbacks()
   {
      final MockedClass mock = mock(MockedClass.class);

      when(mock.someMethod(anyString())).thenAnswer(new Answer() {
         @Override
         public Object answer(InvocationOnMock invocation)
         {
            assertSame(mock, invocation.getMock());
            Object[] args = invocation.getArguments();
            return "called with arguments: " + Arrays.toString(args);
         }
      });

      assertEquals("called with arguments: [foo]", mock.someMethod("foo"));
   }

   @Test // Uses of Mockito API: 7
   public void callingRealMethodFromCallback()
   {
      MockedClass mock = mock(MockedClass.class);

      when(mock.someMethod(anyString())).thenAnswer(new Answer() {
         @Override
         public Object answer(InvocationOnMock invocation) throws Throwable
         {
            String actualResult = (String) invocation.callRealMethod();
            return "Res=" + actualResult;
         }
      });

      assertEquals("Res=3", mock.someMethod("3"));
   }

   @Test // Uses of Mockito API: 2
   public void stubbingVoidMethods()
   {
      doThrow(new RuntimeException()).when(mockedList).clear();

      try {
         // Following throws RuntimeException:
         mockedList.clear();
         fail();
      }
      catch (RuntimeException ignore) {}
   }

   @Test // Uses of Mockito API: 9
   public void spyingOnRealObjects()
   {
      MockedClass realObj = new MockedClass();

      // Create an spy for a real object:
      MockedClass spy = spy(realObj);

      // Optionally, you can stub out some methods:
      when(spy.getSomeValue()).thenReturn(100);

      // When using the regular "when(spy.someMethod(...)).thenDoXyz(...)" API, all calls to a spy
      // object will not only perform stubbing, but also execute the real method:
      // when(spy.get(1)).thenReturn("an item"); would throw an IndexOutOfBoundsException.
      // Therefore, a different API may need to be used with a spy, in order to avoid side effects:
      doReturn("an item").when(spy).getItem(1);

      // Using the spy calls real methods, except those stubbed out:
      spy.doSomething("one", true);
      spy.doSomething("two", false);

      assertEquals("one", spy.getItem(0));
      assertEquals("an item", spy.getItem(1));
      assertEquals(100, spy.getSomeValue());

      // Optionally, you can verify:
      verify(spy).doSomething("one", true); // the real "doSomething" method is not called here
      verify(spy).doSomething(eq("two"), anyBoolean());
   }

   @Test // Uses of Mockito API: 5
   public void capturingArgumentForVerification()
   {
      MockedClass mock = mock(MockedClass.class);

      mock.doSomething(new Person("John"));

      ArgumentCaptor<Person> argument = ArgumentCaptor.forClass(Person.class);
      verify(mock).doSomething(argument.capture());
      assertEquals("John", argument.getValue().getName());
   }

   @Test // Uses of Mockito API: 6
   public void capturingArgumentsForVerification()
   {
      MockedClass mock = mock(MockedClass.class);

      mock.doSomething(new Person("John"));
      mock.doSomething(new Person("Jane"));

      ArgumentCaptor<Person> peopleCaptor = ArgumentCaptor.forClass(Person.class);
      verify(mock, times(2)).doSomething(peopleCaptor.capture());

      List<Person> capturedPeople = peopleCaptor.getAllValues();
      assertEquals("John", capturedPeople.get(0).getName());
      assertEquals("Jane", capturedPeople.get(1).getName());
   }

   @Test // Uses of Mockito API: 8
   public void capturingMultipleArgumentsForVerification()
   {
      MockedClass mock = mock(MockedClass.class);

      mock.doSomething("test", true);

      ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<Boolean> captor2 = ArgumentCaptor.forClass(boolean.class);
      verify(mock).doSomething(captor1.capture(), captor2.capture());

      assertEquals("test", captor1.getValue());
      assertTrue(captor2.getValue());
   }

   @Test // Uses of Mockito API: 5
   public void chainingMethodCallsWithDeepStubbing()
   {
      MockedClass mock = mock(MockedClass.class, RETURNS_DEEP_STUBS);

      // note that we're stubbing a chain of methods here: getBar().getName()
      when(mock.getPerson().getName()).thenReturn("deep");

      // note that we're chaining method calls: getBar().getName()
      assertEquals("deep", mock.getPerson().getName());

      // The following verification does work:
      verify(mock.getPerson()).getName();
      // ... but this one does not: verify(mock).getPerson();
   }

   @SuppressWarnings("CastToIncompatibleInterface")
   @Test
   public void creatingAMockWithExtraInterfaces()
   {
      MockedClass mock = mock(MockedClass.class, withSettings().extraInterfaces(Runnable.class));

      when(mock.getItem(1)).thenReturn("test");

      assertEquals("test", mock.getItem(1));
      ((Runnable) mock).run();

      ((Runnable) verify(mock)).run();
   }

   @Test
   public void verificationIgnoringStubs()
   {
      MockedClass mock = mock(MockedClass.class);
      MockedClass mockTwo = mock(MockedClass.class);

      // Stubbings:
      when(mock.getItem(1)).thenReturn("ignored");

      // In tested code:
      mock.doSomething("a", true);
      mockTwo.someMethod("b");
      mock.getItem(1);

      // Verify invocations that were not stubbed:
      verify(mock).doSomething("a", true);
      verify(mockTwo).someMethod("b");

      // Ignores all stubbed methods:
      // This would fail: verifyNoMoreInteractions(mock, mockTwo);
      verifyNoMoreInteractions(ignoreStubs(mock, mockTwo));
   }

   @Test
   public void verificationInOrderIgnoringStubs()
   {
      MockedClass mock = mock(MockedClass.class);
      MockedClass mockTwo = mock(MockedClass.class);

      // Stubbings:
      when(mock.getItem(1)).thenReturn("ignored");

      // In tested code:
      mock.doSomething("a", true);
      mockTwo.someMethod("b");
      mock.getItem(1);

      // Creates InOrder that will ignore stubbed:
      InOrder inOrder = inOrder(ignoreStubs(mock, mockTwo));
      inOrder.verify(mock).doSomething("a", true);
      inOrder.verify(mockTwo).someMethod("b");
      inOrder.verifyNoMoreInteractions();
   }

   @Test // Uses of Mockito API: 5
   public void nonGreedyVerificationInOrder()
   {
      MockedClass mock = mock(MockedClass.class);

      mock.someMethod("some arg");
      mock.someMethod("some arg");
      mock.someMethod("some arg");
      mock.doSomething("testing", true);
      mock.someMethod("some arg");

      InOrder inOrder = inOrder(mock);
      // "times(2)" would require exactly two invocations, while
      // "atLeast(2)" would also count the fourth invocation:
      inOrder.verify(mock, calls(2)).someMethod("some arg");
      inOrder.verify(mock).doSomething("testing", true);
   }

   @Test // Uses of Mockito API: 4
   public void returningElementsFromAList()
   {
      List<String> list = asList("a", "b", "c");

      when(mockedList.get(anyInt())).then(returnsElementsOf(list));

      assertEquals("a", mockedList.get(0));
      assertEquals("b", mockedList.get(1));
      assertEquals("c", mockedList.get(2));
      assertEquals("c", mockedList.get(3));
   }

   @Test // Uses of Mockito API: 5
   public void returningFirstArgument()
   {
      MockedClass mock = mock(MockedClass.class);

      when(mock.someMethod(anyString())).then(returnsFirstArg());

      assertEquals("test", mock.someMethod("test"));
   }

   @Test // Uses of Mockito API: 5
   public void returningLastArgument()
   {
      when(mockedList.set(anyInt(), anyString())).then(returnsLastArg());

      assertEquals("test", mockedList.set(1, "test"));
   }
}
