/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.internal.*;

public final class DynamicOnInstanceMockingTest
{
   static class Collaborator
   {
      protected int value;

      Collaborator() { value = -1; }
      Collaborator(int value) { this.value = value; }

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }
   }

   static class AnotherDependency
   {
      private String name;

      public String getName() { return name; }
      public void setName(String name) { this.name = name; }
   }

   @Test
   public void mockingOneInstanceAndMatchingInvocationsOnlyOnThatInstance()
   {
      Collaborator collaborator1 = new Collaborator();
      Collaborator collaborator2 = new Collaborator();
      final Collaborator collaborator3 = new Collaborator();

      new Expectations(collaborator3) {{
         collaborator3.getValue(); result = 3;
      }};

      assertEquals(-1, collaborator1.getValue());
      assertEquals(-1, collaborator2.getValue());
      assertEquals(3, collaborator3.getValue());
      assertEquals(2, new Collaborator(2).getValue());
   }

   @Test
   public void mockingTwoInstancesAndMatchingInvocationsOnEachOne()
   {
      final Collaborator collaborator1 = new Collaborator();
      Collaborator collaborator2 = new Collaborator();

      new Expectations(collaborator1, collaborator2) {{
         collaborator1.getValue(); result = 1;
      }};

      collaborator2.setValue(2);
      assertEquals(2, collaborator2.getValue());
      assertEquals(1, collaborator1.getValue());
      assertEquals(3, new Collaborator(3).getValue());
   }

   @Test
   public void mockingAClassAndMatchingInvocationsOnAnyInstance()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(Collaborator.class) {{
         collaborator.getValue(); result = 1;
      }};

      collaborator.setValue(2);
      assertEquals(1, collaborator.getValue());
      assertEquals(1, new Collaborator(2).getValue());
   }

   @Test
   public void mockingOneInstanceButRecordingOnAnother()
   {
      Collaborator collaborator1 = new Collaborator();
      final Collaborator collaborator2 = new Collaborator();
      Collaborator collaborator3 = new Collaborator();

      new Expectations(collaborator1) {{
         // A misuse of the API:
         collaborator2.getValue(); result = -2;
      }};

      collaborator1.setValue(1);
      collaborator2.setValue(2);
      collaborator3.setValue(3);
      assertEquals(1, collaborator1.getValue());
      assertEquals(-2, collaborator2.getValue());
      assertEquals(3, collaborator3.getValue());
   }

   @Test
   public void mockingOneInstanceAndOneClass()
   {
      Collaborator collaborator1 = new Collaborator();
      final Collaborator collaborator2 = new Collaborator();
      Collaborator collaborator3 = new Collaborator();
      final AnotherDependency dependency = new AnotherDependency();

      new Expectations(collaborator2, AnotherDependency.class) {{
         collaborator2.getValue(); result = -2;
         dependency.getName(); result = "name1";
      }};

      collaborator1.setValue(1);
      collaborator2.setValue(2);
      collaborator3.setValue(3);
      assertEquals(-2, collaborator2.getValue());
      assertEquals(1, collaborator1.getValue());
      assertEquals(3, collaborator3.getValue());

      dependency.setName("modified");
      assertEquals("name1", dependency.getName());

      AnotherDependency dep2 = new AnotherDependency();
      dep2.setName("another");
      assertEquals("name1", dep2.getName());
   }

   public static class Foo
   {
      Foo buildValue(@SuppressWarnings("unused") String s) { return this; }
      boolean doIt() { return false; }
      boolean doItAgain() { return false; }
      AnotherDependency getBar() { return null; }
   }

   public static class SubFoo extends Foo {}

   @Test
   public void recordDuplicateInvocationOnTwoDynamicMocksOfDifferentTypesButSharedBaseClass()
   {
      final Foo f1 = new Foo();
      final SubFoo f2 = new SubFoo();

      new Expectations(f1, f2) {{
         f1.doIt(); result = true;
         f2.doIt(); result = false;
      }};

      assertTrue(f1.doIt());
      assertFalse(f2.doIt());
      assertFalse(new Foo().doIt());
      assertFalse(new SubFoo().doIt());
   }

   @Test
   public void passBothClassLiteralAndInstanceInExpectationsConstructor()
   {
      final Foo foo1 = new Foo();
      Foo foo2 = new Foo();

      // Instance-specific mocking takes precedence over any-instance mocking, when both are
      // (erroneously) used for the same class.
      new Expectations(Foo.class, foo1) {{
         foo1.doIt(); result = true;
      }};

      assertTrue(foo1.doIt());
      assertFalse(foo2.doItAgain());
      assertFalse(foo2.doIt());
      assertFalse(foo1.doItAgain());
      Foo foo3 = new Foo();
      assertFalse(foo3.doIt());
      assertFalse(foo3.doItAgain());
   }

   @Test
   public void verifyMethodInvocationCountOnMockedAndNonMockedInstances()
   {
      final Foo foo1 = new Foo();
      final Foo foo2 = new Foo();

      new Expectations(foo1, foo2) {{
         foo1.doIt(); result = true;
      }};

      assertTrue(foo1.doIt());
      assertFalse(foo2.doItAgain());
      assertFalse(foo2.doIt());
      final Foo foo3 = new Foo();
      assertFalse(foo1.doItAgain());
      assertFalse(foo3.doItAgain());
      assertFalse(foo3.doIt());
      assertFalse(foo3.doItAgain());

      new Verifications() {{
         assertFalse(foo2.doIt()); times = 1;
         assertFalse(foo1.doItAgain()); times = 1;
         assertFalse(foo3.doItAgain()); times = 2;
      }};
   }

   @Test
   public void createCascadedMockFromPartiallyMockedInstance()
   {
      final Foo foo = new Foo();

      new Expectations(foo) {{
         foo.getBar().getName(); result = "cascade";
      }};

      assertEquals("cascade", foo.getBar().getName());
   }

   @Test
   public void useAvailableMockedInstanceAsCascadeFromPartiallyMockedInstance(@Mocked AnotherDependency bar)
   {
      final Foo foo = new Foo();

      new Expectations(foo) {{
         foo.getBar().getName(); result = "cascade";
      }};

      AnotherDependency cascadedBar = foo.getBar();
      assertSame(bar, cascadedBar);
      assertEquals("cascade", cascadedBar.getName());
   }

   static final class Bar extends AnotherDependency {}

   @Test
   public void useAvailableMockedSubclassInstanceAsCascadeFromPartiallyMockedInstance(@Mocked Bar bar)
   {
      final Foo foo = new Foo();

      new Expectations(foo) {{
         foo.getBar().getName(); result = "cascade";
      }};

      AnotherDependency cascadedBar = foo.getBar();
      assertSame(bar, cascadedBar);
      assertEquals("cascade", cascadedBar.getName());
   }

   @Test
   public void useItselfAsCascadeFromPartiallyMockedInstance()
   {
      final Foo foo = new Foo();

      new Expectations(foo) {{
         foo.buildValue(anyString).doIt(); result = true;
      }};

      Foo cascadedFoo = foo.buildValue("test");
      assertSame(foo, cascadedFoo);
      assertTrue(cascadedFoo.doIt());
   }

   @Test
   public void verifySingleInvocationToMockedInstanceWithAdditionalInvocationToSameMethodOnAnotherInstance()
   {
      final Collaborator mocked = new Collaborator();

      new Expectations(mocked) {};

      Collaborator notMocked = new Collaborator();
      assertEquals(-1, notMocked.getValue());
      assertEquals(-1, mocked.getValue());

      new Verifications() {{
         mocked.getValue();
         times = 1;
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void verifyOrderedInvocationsToDynamicallyMockedInstanceWithAnotherInstanceInvolvedButMissingAnInvocation()
   {
      final Collaborator mock = new Collaborator();

      new Expectations(mock) {};

      mock.setValue(1);
      new Collaborator().setValue(2);

      new VerificationsInOrder() {{
         mock.setValue(1); times = 1;
         mock.setValue(2); times = 1; // must be missing
      }};
   }

   @Test
   public void verifyOrderedInvocationsToDynamicallyMockedInstanceWithAnotherInstanceInvolved()
   {
      final Collaborator mock = new Collaborator();

      new Expectations(mock) {{ mock.setValue(anyInt); }};

      mock.setValue(1);
      new Collaborator().setValue(2);

      new VerificationsInOrder() {{
         mock.setValue(1); times = 1;
         mock.setValue(2); times = 0;
      }};
   }

   static class Baz {}
   static class Noop { void noop() {} }

   @Test(expected = MissingInvocation.class)
   public void partiallyMockClassWithInjectableMockWhileHavingTwoMockParametersOfAnotherType(
      @Injectable final Noop mock, @Mocked Baz a, @Mocked Baz b)
   {
      Noop foo = new Noop();
      new Expectations(Noop.class) {{ mock.noop(); }};

      foo.noop();
   }
}