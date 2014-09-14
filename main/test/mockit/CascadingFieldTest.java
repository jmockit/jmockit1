/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class CascadingFieldTest
{
   static class Foo
   {
      Bar getBar() { return null; }

      static Bar globalBar() { return null; }

      void doSomething(String s) { throw new RuntimeException(s); }
      int getIntValue() { return 1; }
      private Boolean getBooleanValue() { return true; }
      String getStringValue() { return "abc"; }
      public final Date getDate() { return null; }
      final List<Integer> getList() { return null; }
   }

   static class Bar
   {
      Bar() { throw new RuntimeException(); }
      int doSomething() { return 1; }
      boolean isDone() { return false; }
      Short getShort() { return 1; }
      List<?> getList() { return null; }
      Baz getBaz() { return null; }
      Runnable getTask() { return null; }
   }

   static final class Baz { void doSomething() {} }

   public interface A { B getB(); }
   public interface B { C getC(); }
   public interface C {}

   @Mocked Foo foo;
   @Mocked A a;

   @Before
   public void recordCommonExpectations()
   {
      new NonStrictExpectations() {{
         foo.getBar().isDone(); result = true;
      }};
   }

   @Test
   public void obtainCascadedInstancesAtAllLevels()
   {
      assertNotNull(foo.getBar());
      assertNotNull(foo.getBar().getList());
      assertNotNull(foo.getBar().getBaz());
      assertNotNull(foo.getBar().getTask());

      B b = a.getB();
      assertNotNull(b);
      assertNotNull(b.getC());
   }

   @Test
   public void obtainCascadedInstancesAtAllLevelsAgain()
   {
      Bar bar = foo.getBar();
      assertNotNull(bar);
      assertNotNull(bar.getList());
      assertNotNull(bar.getBaz());
      assertNotNull(bar.getTask());

      assertNotNull(a.getB());
      assertNotNull(a.getB().getC());
   }

   @Test
   public void cascadeOneLevel()
   {
      assertTrue(foo.getBar().isDone());
      assertEquals(0, foo.getBar().doSomething());
      assertEquals(0, Foo.globalBar().doSomething());
      assertNotSame(foo.getBar(), Foo.globalBar());
      assertEquals(0, foo.getBar().getShort().intValue());

      foo.doSomething("test");
      assertEquals(0, foo.getIntValue());
      assertFalse(foo.getBooleanValue());
      assertNull(foo.getStringValue());
      assertNotNull(foo.getDate());
      assertTrue(foo.getList().isEmpty());

      new Verifications() {{ foo.doSomething(anyString); }};
   }

   @Test
   public void exerciseCascadingMockAgain()
   {
      assertTrue(foo.getBar().isDone());
   }

   @Test
   public void recordUnambiguousStrictExpectationsProducingDifferentCascadedInstances()
   {
      new Expectations() {{
         Bar c1 = Foo.globalBar();
         c1.isDone(); result = true;
         Bar c2 = Foo.globalBar();
         c2.doSomething(); result = 5;
         assertNotSame(c1, c2);
      }};

      Bar b1 = Foo.globalBar();
      assertTrue(b1.isDone());
      Bar b2 = Foo.globalBar();
      assertEquals(5, b2.doSomething());
      assertNotSame(b1, b2);
   }

   @Test
   public void recordUnambiguousNonStrictExpectationsProducingDifferentCascadedInstances(
      @Mocked final Foo foo1, @Mocked final Foo foo2)
   {
      new NonStrictExpectations() {{
         Date c1 = foo1.getDate();
         Date c2 = foo2.getDate();
         assertNotSame(c1, c2);
      }};

      Date d1 = foo1.getDate();
      Date d2 = foo2.getDate();
      assertNotSame(d1, d2);
   }

   @Test
   public void recordAmbiguousNonStrictExpectationsOnInstanceMethodProducingTheSameCascadedInstance()
   {
      new NonStrictExpectations() {{
         Bar c1 = foo.getBar();
         Bar c2 = foo.getBar();
         assertSame(c1, c2);
      }};

      Bar b1 = foo.getBar();
      Bar b2 = foo.getBar();
      assertSame(b1, b2);
   }

   @Test
   public void recordAmbiguousNonStrictExpectationsOnStaticMethodProducingTheSameCascadedInstance()
   {
      new NonStrictExpectations() {{
         Bar c1 = Foo.globalBar();
         Bar c2 = Foo.globalBar();
         assertSame(c1, c2);
      }};

      Bar b1 = Foo.globalBar();
      Bar b2 = Foo.globalBar();
      assertSame(b1, b2);
   }

   static final class AnotherFoo { Bar getBar() { return null; } }
   @Mocked AnotherFoo anotherFoo;

   @Test
   public void cascadingMockField()
   {
      new NonStrictExpectations() {{
         anotherFoo.getBar().doSomething(); result = 123;
      }};

      assertEquals(123, new AnotherFoo().getBar().doSomething());
   }

   @Test
   public void cascadingInstanceAccessedFromDelegateMethod()
   {
      new NonStrictExpectations() {{
         foo.getIntValue();
         result = new Delegate() {
            @Mock int delegate() { return foo.getBar().doSomething(); }
         };
      }};

      assertEquals(0, foo.getIntValue());
   }

   // Tests for cascaded instances obtained from generic methods //////////////////////////////////////////////////////

   static class GenericBaseClass1<T> { T getValue() { return null; } }

   @Test
   public void cascadeGenericMethodFromSpecializedGenericClass(@Mocked GenericBaseClass1<C> mock)
   {
      C value = mock.getValue();
      assertNotNull(value);
   }

   static class ConcreteSubclass1 extends GenericBaseClass1<A> {}

   @Test
   public void cascadeGenericMethodOfConcreteSubclassWhichExtendsGenericClass(@Mocked final ConcreteSubclass1 mock)
   {
      new NonStrictExpectations() {{
         mock.getValue().getB().getC();
         result = new C() {};
      }};

      A value = mock.getValue();
      assertNotNull(value);
      B b = value.getB();
      assertNotNull(b);
      assertNotNull(b.getC());

      new FullVerificationsInOrder() {{ mock.getValue().getB().getC(); }};
   }

   interface Ab extends A {}
   static class GenericBaseClass2<T extends A> { T getValue() { return null; } }
   static class ConcreteSubclass2 extends GenericBaseClass2<Ab> {}

   @Test
   public void cascadeGenericMethodOfSubclassWhichExtendsGenericClassWithUpperBoundUsingInterface(
      @Mocked final ConcreteSubclass2 mock)
   {
      new NonStrictExpectations() {{ mock.getValue().getB().getC(); }};

      Ab value = mock.getValue();
      assertNotNull(value);
      value.getB().getC();

      new Verifications() {{ mock.getValue().getB().getC(); times = 1; }};
   }

   @Test
   public void cascadeGenericMethodOfSubclassWhichExtendsGenericClassWithUpperBoundOnlyInVerificationBlock(
      @Mocked final ConcreteSubclass2 mock)
   {
      new FullVerifications() {{
         Ab value = mock.getValue(); times = 0;
         B b = value.getB(); times = 0;
         b.getC(); times = 0;
      }};
   }

   static final class Action implements A { @Override public B getB() { return null; } }
   static final class ActionHolder extends GenericBaseClass2<Action> {}

   @Test
   public void cascadeGenericMethodOfSubclassWhichExtendsGenericClassWithUpperBoundUsingClass(
      @Mocked final ActionHolder mock)
   {
      new Expectations() {{ mock.getValue().getB().getC(); }};

      mock.getValue().getB().getC();
   }
}
