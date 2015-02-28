/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class MisusingMockupsAPITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class Collaborator
   {
      int doSomething() { return 1; }
      @SuppressWarnings("unused") void methodWithParameters(int i, String s) {}
   }

   @Test
   public void applySameMockClassWhilePreviousApplicationStillActive()
   {
      // Apply then tear-down.
      new SomeMockUp(0).tearDown();
      assertEquals(1, new Collaborator().doSomething());

      // Apply again after tear-down: ok.
      new SomeMockUp(2);
      assertEquals(2, new Collaborator().doSomething());

      // Apply again while still active: not ok, but handled by automatically tearing-down the previous mock-up.
      new SomeMockUp(3);
      assertEquals(3, new Collaborator().doSomething());
   }

   static final class SomeMockUp extends MockUp<Collaborator>
   {
      final int value;
      SomeMockUp(int value) { this.value = value; }
      @Mock(invocations = 1) int doSomething() { return value; }
   }

   public static final class ProceedingMockUp extends MockUp<Collaborator> {
      @Mock public static int doSomething(Invocation inv) { return inv.proceed(); }
   }

   @Test
   public void applySameMockClassTwiceWithCallsToProceedingMockMethod()
   {
      Collaborator col = new Collaborator();

      new ProceedingMockUp();
      col.doSomething();

      new ProceedingMockUp(); // causes StackOverflow if applied
      col.doSomething();
   }

   @Test
   public void applySameMockClassUsingSecondaryConstructorWhilePreviousApplicationStillActive()
   {
      new AnotherMockUp(0).tearDown();
      assertEquals(1, new Collaborator().doSomething());

      new AnotherMockUp(2);
      assertEquals(2, new Collaborator().doSomething());

      new AnotherMockUp(3);
      assertEquals(3, new Collaborator().doSomething());
   }

   static final class AnotherMockUp extends MockUp<Collaborator>
   {
      final int value;
      AnotherMockUp(int value) { super(Collaborator.class); this.value = value; }
      @Mock(invocations = 1) int doSomething() { return value; }
   }

   @Test
   public void mockSameMethodTwiceWithReentrantMocksFromTwoDifferentMockClasses()
   {
      new MockUp<Collaborator>() {
         @Mock
         int doSomething(Invocation inv)
         {
            int i = inv.proceed();
            return i + 1;
         }
      };

      int i = new Collaborator().doSomething();
      assertEquals(2, i);

      new MockUp<Collaborator>() {
         @Mock
         int doSomething(Invocation inv)
         {
            int j = inv.proceed();
            return j + 2;
         }
      };

      // Should return 4, but returns 6. Chaining mock methods is not supported.
      int j = new Collaborator().doSomething();
      assertEquals(6, j);
   }

   @Test
   public void mockUpMethodInClassWhichIsAlreadyMocked(@Mocked Collaborator col)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("already mocked");
      thrown.expectMessage("Collaborator");

      new MockUp<Collaborator>() {
         @Mock int doSomething() { return 2; }
      };
   }

   @Test
   public void attemptToHaveMockMethodWithInvocationParameterNotAtFirstPosition()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Mock method");
      thrown.expectMessage("Invocation parameter");
      thrown.expectMessage("first");

      new MockUp<Collaborator>() {
         @Mock void methodWithParameters(int i, String s, Invocation inv) {}
      };
   }
}