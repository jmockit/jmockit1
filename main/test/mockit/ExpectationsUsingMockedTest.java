/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import javax.swing.*;

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

      void provideSomeService() {}

      int getValue() { return value; }

      @SuppressWarnings("UnusedDeclaration")
      final void simpleOperation(int a, String b, Date c) {}
   }

   public abstract static class AbstractBase { protected abstract boolean add(Integer i); }
   @Mocked AbstractBase base;

   static final class DependencyImpl implements Dependency
   {
      @Override
      public String doSomething(boolean b) { return ""; }
   }

   @Mocked("do.*") DependencyImpl mockDependency;

   @Test
   public void mockParameterWithMockingFilters(
      @Mocked({"(int)", "doInternal()", "[gs]etValue", "complexOperation(Object)"}) final Collaborator mock)
   {
      new Expectations() {{ mock.getValue(); }};

      // Calls the real method, not a mock.
      Collaborator collaborator = new Collaborator();
      collaborator.provideSomeService();

      // Calls the mock method.
      collaborator.getValue();
   }

   @Test(expected = IllegalArgumentException.class)
   public void mockParameterWithInvalidMockingFilter(@Mocked("setValue(int") Collaborator mock)
   {
      fail();
   }

   @Test
   public void multipleMockParametersOfSameMockedType(
      @Mocked final Dependency dependency1, @Mocked final Dependency dependency2)
   {
      new NonStrictExpectations() {{
         dependency1.doSomething(true); result = "1";
         dependency2.doSomething(false); result = "2";
      }};

      assertEquals("1", dependency1.doSomething(true));
      assertNull(dependency1.doSomething(false));
   }

   @Test
   public void mockFieldForAbstractClass()
   {
      new NonStrictExpectations() {{
         base.add(1); result = true;
      }};

      assertFalse(base.add(0));
      assertTrue(base.add(1));
      assertFalse(base.add(2));
   }

   @Test
   public void partialMockingOfConcreteClassThatExcludesConstructors()
   {
      new Expectations() {{
         mockDependency.doSomething(anyBoolean); minTimes = 2;
      }};

      mockDependency.doSomething(true);
      mockDependency.doSomething(false);
      mockDependency.doSomething(true);
   }

   @Test
   public void mockNothingAndStubNoStaticInitializers(@Mocked("") JComponent container)
   {
      assertEquals("Test", new JLabel("Test").getText());
   }

   static class ClassWithStaticInitializer
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void onlyStubOutStaticInitializers(
      @Mocked(value = "", stubOutClassInitialization = true) final ClassWithStaticInitializer unused)
   {
      assertEquals(-1, ClassWithStaticInitializer.initialized());
   }

   static class ClassWithStaticInitializer2
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void stubOutStaticInitializersWhenSpecified(
      @Mocked(stubOutClassInitialization = true) ClassWithStaticInitializer2 unused)
   {
      assertEquals(0, ClassWithStaticInitializer2.initialized());
      assertFalse(ClassWithStaticInitializer2.initialized);
   }

   static class ClassWithStaticInitializer3
   {
      static boolean initialized = true;
      static int initialized() { return initialized ? 1 : -1; }
   }

   @Test
   public void doNotStubOutStaticInitializersByDefault(@Mocked ClassWithStaticInitializer3 unused)
   {
      assertEquals(0, ClassWithStaticInitializer3.initialized());
      assertTrue(ClassWithStaticInitializer3.initialized);
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

      new NonStrictExpectations() {{
         innerMock.getValue(); result = 123; times = 1;
      }};

      assertEquals(123, new InnerClass().getValue());
   }

   static final class ClassWithNative
   {
      int doSomething() { return nativeMethod(); }
      private native int nativeMethod();
   }

   @Test
   public void partiallyMockNativeMethod(@Mocked("nativeMethod") final ClassWithNative mock)
   {
      new Expectations() {{
         mock.nativeMethod(); result = 123;
      }};

      assertEquals(123, mock.doSomething());
   }
}
