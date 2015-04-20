/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;

public final class ExpectationsUsingMockedTest
{
   public interface Dependency { String doSomething(boolean b); }

   static class Collaborator
   {
      private int value;

      Collaborator() {}
      Collaborator(int value) { this.value = value; }

      int getValue() { return value; }

      @SuppressWarnings("UnusedDeclaration")
      final void simpleOperation(int a, String b, Date c) {}
   }

   public abstract static class AbstractBase { protected abstract boolean add(Integer i); }
   @Mocked AbstractBase base;

   @Test
   public void multipleMockParametersOfSameMockedType(
      @Mocked final Dependency dependency1, @Mocked final Dependency dependency2)
   {
      new Expectations() {{
         dependency1.doSomething(true); result = "1";
         dependency2.doSomething(false); result = "2";
      }};

      assertEquals("1", dependency1.doSomething(true));
      assertNull(dependency1.doSomething(false));
      assertEquals("2", dependency2.doSomething(false));
      assertNull(dependency2.doSomething(true));
   }

   @Test
   public void mockFieldForAbstractClass()
   {
      new Expectations() {{
         base.add(1); result = true;
      }};

      assertFalse(base.add(0));
      assertTrue(base.add(1));
      assertFalse(base.add(2));
   }

   static class ClassWithStaticInitializer
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void stubOutStaticInitializersWhenSpecified(
      @Mocked(stubOutClassInitialization = true) ClassWithStaticInitializer unused)
   {
      assertEquals(0, ClassWithStaticInitializer.initialized());
      assertFalse(ClassWithStaticInitializer.initialized);
   }

   static class ClassWithStaticInitializer2
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void doNotStubOutStaticInitializersByDefault(@Mocked ClassWithStaticInitializer2 unused)
   {
      assertEquals(0, ClassWithStaticInitializer2.initialized());
      assertTrue(ClassWithStaticInitializer2.initialized);
   }

   static class AnotherClassWithStaticInitializer
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void mockEverythingWithoutStubbingStaticInitializers(@Mocked AnotherClassWithStaticInitializer unused)
   {
      assertEquals(0, AnotherClassWithStaticInitializer.initialized());
      assertTrue(AnotherClassWithStaticInitializer.initialized);
   }

   static class AnotherClassWithStaticInitializer2
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void avoidStubbingStaticInitializersThroughSpecificAnnotationAttribute(
      @Mocked(stubOutClassInitialization = false) AnotherClassWithStaticInitializer2 unused)
   {
      assertEquals(0, AnotherClassWithStaticInitializer2.initialized());
      assertTrue(AnotherClassWithStaticInitializer2.initialized);
   }

   class InnerClass { int getValue() { return -1; } }

   @Test
   public void mockInnerClass(@Mocked final InnerClass innerMock)
   {
      assertEquals(0, innerMock.getValue());

      new Expectations() {{
         innerMock.getValue(); result = 123; times = 1;
      }};

      assertEquals(123, new InnerClass().getValue());
   }
}
