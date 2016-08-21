/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import javax.annotation.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockedClassWithSuperClassTest
{
   static class SubclassOfJREClass extends Writer
   {
      @Override public void write(@Nonnull char[] cbuf, int off, int len) {}
      @Override public void flush() {}
      @Override public void close() { throw new UnsupportedOperationException(); }
   }

   static class BaseClass
   {
      protected int doSomething() { return 123; }
      static int staticMethod() { return -1; }
   }

   public static class Subclass extends BaseClass { BaseClass getInstance() { return this; } }

   @Test
   public void mockedClassExtendingJREClass(@Mocked SubclassOfJREClass mock) throws Exception
   {
      // Mocked:
      assertSame(mock, mock.append("a"));
      assertSame(mock, mock.append('a'));
      mock.close();

      // Not mocked:
      Writer w = new Writer() {
         @Override public void write(@Nonnull char[] cbuf, int off, int len) {}
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
      new Expectations() {{ mock.doSomething(); result = 45; times = 3; }};

      // Mocked:
      assertEquals(45, mock.doSomething());
      assertEquals(45, new Subclass().doSomething());

      // Mocked and matching the recorded expectation:
      assertEquals(45, new Subclass() {}.doSomething());

      // Not mocked:
      BaseClass b1 = new BaseClass();
      BaseClass b2 = new BaseClass() { @Override protected int doSomething() { return super.doSomething() - 23; } };
      assertEquals(123, b1.doSomething());
      assertEquals(100, b2.doSomething());
   }

   @Test
   public void cascadingSubclassWithMethodReturningCascadedBaseClassInstance(@Mocked Subclass mock)
   {
      // The subclass is already mocked at this point, when the cascaded instance gets created.
      BaseClass cascaded = mock.getInstance();

      assertEquals(0, cascaded.doSomething());
      assertEquals(0, mock.doSomething());
   }

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

   @Test
   public void recordExpectationOnStaticMethodFromBaseClass(@Mocked Subclass unused)
   {
      new Expectations() {{
         BaseClass.staticMethod();
         result = 123;
      }};

      assertEquals(123, BaseClass.staticMethod());
   }

   static class BaseClassWithConstructor { BaseClassWithConstructor(@SuppressWarnings("unused") boolean b) {} }
   static class DerivedClass extends BaseClassWithConstructor
   {
      protected DerivedClass()
      {
         // TRYCATCHBLOCK instruction appears before call to super, which caused a VerifyError.
         super(true);
         try { doSomething(); } catch (RuntimeException ignore) {}
      }

      private void doSomething() {}
   }

   @Test
   public void mockSubclassWithConstructorContainingTryCatch_usingMockUp()
   {
      new MockUp<DerivedClass>() { @Mock void $init() {} };
      new DerivedClass();
   }

   @Test
   public void mockSubclassWithConstructorContainingTryCatch_usingExpectations(@Mocked DerivedClass mock)
   {
      new DerivedClass();
   }

   static class Subclass2 extends BaseClass {}

   @Test
   public void recordSameMethodOnDifferentMockedSubclasses(@Mocked final Subclass mock1, @Mocked final Subclass2 mock2)
   {
      new Expectations() {{
         mock1.doSomething(); result = 1;
         mock2.doSomething(); result = 2;
      }};

      assertEquals(1, mock1.doSomething());
      assertEquals(2, mock2.doSomething());
   }

   @Test
   public void recordMethodOnMockedBaseClassButReplayOnSubclassInstance(@Mocked final BaseClass baseMock)
   {
      new Expectations() {{ baseMock.doSomething(); result = 45; }};

      Subclass derived = new Subclass();
      assertEquals(45, derived.doSomething());
      assertEquals(45, baseMock.doSomething());
   }
}
