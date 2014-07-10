/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.jetbrains.annotations.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockedClassWithSuperClassTest
{
   static class SubclassOfJREClass extends Writer
   {
      @Override public void write(@NotNull char[] cbuf, int off, int len) {}
      @Override public void flush() {}
      @Override public void close() { throw new UnsupportedOperationException(); }
   }

   static class BaseClass { int doSomething() { return 123; }}
   static class Subclass extends BaseClass { BaseClass getInstance() { return this; } }

   // With Expectations & Verifications API ///////////////////////////////////////////////////////////////////////////

   @Test
   public void mockedClassExtendingJREClass(@Mocked final SubclassOfJREClass mock) throws Exception
   {
      new NonStrictExpectations() {{
         mock.append(anyChar); result = mock;
      }};

      // Mocked:
      assertNull(mock.append("a"));
      assertSame(mock, mock.append('a'));
      mock.close();

      // Not mocked:
      Writer w = new Writer() {
         @Override public void write(@NotNull char[] cbuf, int off, int len) {}
         @Override public void flush() {}
         @Override public void close() {}
      };
      assertSame(w, w.append("Test1"));
      assertSame(w, w.append('b'));

      new SubclassOfJREClass() {}.close();
   }

   @Test
   public void mockedClassExtendingNonJREClass(@Mocked final Subclass mock)
   {
      new NonStrictExpectations() {{ mock.doSomething(); result = 45; }};

      // Mocked:
      assertEquals(45, mock.doSomething());
      assertEquals(45, new Subclass().doSomething());
      assertEquals(45, new Subclass() {}.doSomething());

      // Not mocked:
      BaseClass b1 = new BaseClass();
      BaseClass b2 = new BaseClass() { @Override int doSomething() { return super.doSomething() - 23; } };
      assertEquals(123, b1.doSomething());
      assertEquals(100, b2.doSomething());

      new Verifications() {{ mock.doSomething(); times = 3; }};
   }

   @Test
   public void cascadingSubclassWithMethodReturningCascadedBaseClassInstance(@Cascading final Subclass mock)
   {
      // The subclass is already mocked at this point, when the cascaded instance gets created.
      BaseClass cascaded = mock.getInstance();

      assertEquals(0, cascaded.doSomething());
      assertEquals(0, mock.doSomething());
   }

   /// With Mockups API ///////////////////////////////////////////////////////////////////////////////////////////////

   public static final class MockUpForSubclass extends MockUp<Subclass> {
      @Mock public int doSomething() { return 1; }
   }

   @Test
   public void mockOnlyInstancesOfTheClassSpecifiedToBeMocked()
   {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());

      new MockUpForSubclass();

      assertEquals(1, d.doSomething());
      assertEquals(123, new BaseClass().doSomething());
      assertEquals(1, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(1, new Subclass() {}.doSomething());
   }

   @Test
   public void mockOnlyInstancesOfTheClassSpecifiedToBeMocked_usingMockingBridge()
   {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());

      new MockUp<Subclass>() {
         @Mock int doSomething() { return 2; }
      };

      assertEquals(123, new BaseClass().doSomething());
      assertEquals(2, d.doSomething());
      assertEquals(2, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(2, new Subclass() {}.doSomething());
   }
}
