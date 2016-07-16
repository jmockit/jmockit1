/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class InjectableMockedTest
{
   static final class ClassWithStaticInitializer1
   {
      static boolean classInitializationExecuted = true;
      static int doSomething() { return 1; }
   }

   @Test
   public void mockClassWithStaticInitializerAsInjectable(@Injectable ClassWithStaticInitializer1 mock)
   {
      assertEquals(1, ClassWithStaticInitializer1.doSomething());
      assertTrue(ClassWithStaticInitializer1.classInitializationExecuted);
   }

   static final class ClassWithStaticInitializer2
   {
      static boolean classInitializationExecuted = true;
      static int doSomething() { return 2; }
   }

   @Test
   public void mockClassWithStaticInitializerAsInjectableButSpecifyStubbingOutOfStaticInitializer(
      @Injectable @Mocked(stubOutClassInitialization = true) ClassWithStaticInitializer2 mock)
   {
      assertEquals(2, ClassWithStaticInitializer2.doSomething());
      assertFalse(ClassWithStaticInitializer2.classInitializationExecuted);
   }

   static class Collaborator
   {
      final int value;
      Collaborator() { value = 101; }
      Collaborator(int value) { this.value = value; }
      int doSomething(boolean b) { return b ? 1 : -1; }
   }

   @Test
   public void mockConstructorInInjectableMockedClass(@Injectable Collaborator mock)
   {
      new Expectations(Collaborator.class) {{
         new Collaborator(anyInt);
      }};

      Collaborator collaborator = new Collaborator(123);
      assertEquals(0, collaborator.value);
      assertEquals(0, mock.doSomething(true));
   }

   @Test
   public void mockNextCreatedInstance(@Capturing(maxInstances = 1) @Injectable final Collaborator mock)
   {
      new Expectations() {{
         mock.doSomething(true); result = 2;
      }};

      Collaborator captured = new Collaborator();
      assertEquals(0, captured.value);
      assertEquals(0, captured.doSomething(false));
      assertEquals(2, captured.doSomething(true));

      new Verifications() {{
         mock.doSomething(anyBoolean); times = 2;
      }};

      Collaborator notMocked = new Collaborator();
      assertEquals(101, notMocked.value);
      assertEquals(-1, notMocked.doSomething(false));
      assertEquals(1, notMocked.doSomething(true));
   }

   @Test
   public void mockSeparatelyTheNextTwoCreatedInstances(
      @Injectable @Capturing(maxInstances = 1) final Collaborator mock1,
      @Injectable @Capturing(maxInstances = 1) final Collaborator mock2)
   {
      new Expectations() {{
         mock1.doSomething(true); result = 10;
         mock2.doSomething(false); result = 20;
      }};

      Collaborator captured1 = new Collaborator();
      assertEquals(0, captured1.value);
      assertEquals(0, captured1.doSomething(false));
      assertEquals(10, captured1.doSomething(true));

      Collaborator captured2 = new Collaborator(123);
      assertEquals(0, captured2.value);
      assertEquals(20, captured2.doSomething(false));
      assertEquals(0, captured2.doSomething(true));

      Collaborator notMocked = new Collaborator();
      assertEquals(101, notMocked.value);
      assertEquals(-1, notMocked.doSomething(false));
      assertEquals(1, notMocked.doSomething(true));
   }

   @Test
   public void mockSeparatelyTwoGroupsOfInternallyCreatedInstances(
      @Capturing(maxInstances = 2) @Injectable final Collaborator mock1,
      @Capturing(maxInstances = 3) @Injectable final Collaborator mock2)
   {
      new Expectations() {{
         mock1.doSomething(false); result = -45;
         mock2.doSomething(true); result = 123;
      }};

      // First two instances created and captured in code under test (mock1):
      assertEquals(0, new Collaborator(4) {}.doSomething(true));
      assertEquals(-45, new Collaborator().doSomething(false));

      // Next three instances created and captured in code under test (mock2):
      assertEquals(123, new Collaborator() {}.doSomething(true));
      assertEquals(123, new Collaborator(12).doSomething(true));
      assertEquals(0, new Collaborator(-5).doSomething(false));

      // Further instances not captured:
      assertEquals(1, new Collaborator().doSomething(true));
      assertEquals(-1, new Collaborator(2) {}.doSomething(false));

      new Verifications() {{
         mock1.doSomething(anyBoolean); times = 2;
      }};
   }

   static class AnotherCollaborator
   {
      final int value;
      AnotherCollaborator() { value = 101; }
      AnotherCollaborator(int value) { this.value = value; }
      int doSomething(boolean b) { return b ? 1 : -1; }
   }

   static final class SubclassOfAnotherCollaborator extends AnotherCollaborator
   {
      SubclassOfAnotherCollaborator() {}
      SubclassOfAnotherCollaborator(int value) { throw new IllegalArgumentException("Bad value: " + value); }

      @Override
      int doSomething(boolean b) { return -super.doSomething(b); }
   }

   @Capturing(maxInstances = 2) @Injectable AnotherCollaborator anotherMock1;
   @Capturing(maxInstances = 1) @Injectable AnotherCollaborator anotherMock2;

   @Test
   public void mockSeparatelyTwoGroupsOfInternallyCreatedInstancesUsingMockFields()
   {
      new Expectations() {{
         anotherMock1.doSomething(true); result = -45;
         anotherMock2.doSomething(true); result = 123;
         anotherMock2.doSomething(false); result = 246;
      }};

      // First two instances created and captured in code under test (anotherMock1):
      assertEquals(-45, new AnotherCollaborator(4) {}.doSomething(true));
      assertEquals(0, new SubclassOfAnotherCollaborator().doSomething(false));

      // Next instance created and captured in code under test (anotherMock2):
      AnotherCollaborator instance3 = new AnotherCollaborator();
      assertEquals(123, instance3.doSomething(true));
      assertEquals(246, instance3.doSomething(false));

      // Further instances not captured:
      assertEquals(1, new AnotherCollaborator().doSomething(true));
      assertEquals(-1, new AnotherCollaborator(2) {}.doSomething(false));
      assertEquals(1, new SubclassOfAnotherCollaborator().doSomething(false));

      try {
         new SubclassOfAnotherCollaborator(4560);
         fail();
      }
      catch (IllegalArgumentException ignore) {}

      new FullVerifications() {{
         anotherMock1.doSomething(anyBoolean); times = 2;
      }};
   }
}
