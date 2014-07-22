/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;
import mockit.internal.startup.*;

public final class Java8SupportTest
{
   @FunctionalInterface
   public interface InterfaceWithDefaultMethods
   {
      int regularMethod();
      default int defaultMethod() { return -1; }
   }

   public static final class ClassWhichOverridesDefaultMethodFromInterface implements InterfaceWithDefaultMethods
   {
      @Override public int regularMethod() { return 4; }
      @Override public int defaultMethod() { return 5; }
   }

   @Test
   public void mockInterfaceWithDefaultMethods(@Mocked InterfaceWithDefaultMethods mock)
   {
      new NonStrictExpectations() {{
         mock.defaultMethod(); result = 2;
         mock.regularMethod(); result = 1;
      }};

      assertEquals(1, mock.regularMethod());
      assertEquals(2, mock.defaultMethod());
   }

   @Test
   public void mockClassWithOverriddenDefaultMethod(@Mocked ClassWhichOverridesDefaultMethodFromInterface mock)
   {
      new NonStrictExpectations() {{
         mock.defaultMethod(); result = 2;
         mock.regularMethod(); result = 1;
      }};

      assertEquals(1, mock.regularMethod());
      assertEquals(2, mock.defaultMethod());
   }

   public static class ClassWhichInheritsDefaultMethodFromInterface implements InterfaceWithDefaultMethods
   {
      @Override public int regularMethod() { return 3; }
   }

   @Test
   public void mockClassWithInheritedDefaultMethod(@Mocked ClassWhichInheritsDefaultMethodFromInterface mock)
   {
      new NonStrictExpectations() {{
         mock.defaultMethod();
         result = 123;
      }};

      assertEquals(123, mock.defaultMethod());
   }

   public interface SubInterfaceWithDefaultMethods extends InterfaceWithDefaultMethods
   {
      default String anotherDefaultMethod(int i) { return String.valueOf(i); }
      @SuppressWarnings("unused") void anotherRegularMethod(boolean b, String... names);
   }

   static final class ClassInheritingFromInterfaceHierarchy implements SubInterfaceWithDefaultMethods
   {
      @Override public int regularMethod() { return 4; }
      @Override public void anotherRegularMethod(boolean b, String... names) {}
   }

   @Test
   public void mockClassInheritingFromInterfaceHierarchy(@Injectable ClassInheritingFromInterfaceHierarchy mock)
   {
      new Expectations() {{
         mock.defaultMethod(); result = 123;
         mock.regularMethod(); result = 22;
         mock.anotherDefaultMethod(anyInt); result = "one";
      }};

      assertEquals(123, mock.defaultMethod());
      assertEquals(22, mock.regularMethod());
      assertEquals("one", mock.anotherDefaultMethod(1));
   }

   public interface AnotherInterfaceWithDefaultMethods
   {
      default int defaultMethod1() { return 1; }
      default int defaultMethod2() throws IOException { return 2; }
   }

   static final class ClassInheritingMultipleDefaultMethods
      implements SubInterfaceWithDefaultMethods, AnotherInterfaceWithDefaultMethods
   {
      @Override public int regularMethod() { return 5; }
      @Override public void anotherRegularMethod(boolean b, String... names) {}
   }

   @Test
   public void partiallyMockClassInheritingDefaultMethodsFromMultipleInterfaces() throws Exception
   {
      ClassInheritingMultipleDefaultMethods obj = new ClassInheritingMultipleDefaultMethods();

      new NonStrictExpectations(ClassInheritingMultipleDefaultMethods.class) {{
         obj.defaultMethod(); result = 123;
         obj.defaultMethod2(); result = 22;
         obj.anotherDefaultMethod(1); result = "one";
      }};

      assertEquals(123, obj.defaultMethod());
      assertEquals(5, obj.regularMethod());
      assertEquals(1, obj.defaultMethod1());
      assertEquals(22, obj.defaultMethod2());
      obj.anotherRegularMethod(true);

      new Verifications() {{
         obj.anotherRegularMethod(anyBoolean, (String[]) any);
      }};
   }

   public interface InterfaceWithStaticMethod { static InterfaceWithStaticMethod newInstance() { return null; } }

   @Test
   public void mockStaticMethodInInterface(@Mocked InterfaceWithStaticMethod mock)
   {
      new NonStrictExpectations() {{
         InterfaceWithStaticMethod.newInstance();
         result = mock;
      }};

      InterfaceWithStaticMethod actual = InterfaceWithStaticMethod.newInstance();
      assertSame(mock, actual);
   }

   @Test
   public void mockFunctionalInterfaceFromJRE(@Mocked Consumer<String> mockConsumer)
   {
      StringBuilder concatenated = new StringBuilder();

      new NonStrictExpectations() {{
         mockConsumer.accept(anyString);
         result = new Delegate() {
            @Mock void delegate(String s) { concatenated.append(s).append(' '); }
         };
      }};

      List<String> list = Arrays.asList("mocking", "a", "lambda");
      list.forEach(mockConsumer);

      assertEquals("mocking a lambda ", concatenated.toString());
   }

   interface NonPublicBase
   {
      default int baseDefault() { return -1; }
      default String getDefault() { return "default"; }
      static void doStatic() { throw new RuntimeException("1"); }
   }

   interface NonPublicDerived extends NonPublicBase
   {
      @Override default String getDefault() { return "default derived"; }
      static void doAnotherStatic() { throw new RuntimeException("2"); }
   }

   @Test
   public void mockNonPublicInterfaceHierarchyWithDefaultAndStaticMethods(
      @Mocked NonPublicBase base, @Mocked NonPublicDerived derived)
   {
      new NonStrictExpectations() {{
         base.baseDefault(); result = 1;
         derived.baseDefault(); result = 2;
      }};

      assertEquals(1, base.baseDefault());
      assertEquals(2, derived.baseDefault());
      assertNull(base.getDefault());
      NonPublicBase.doStatic();
      NonPublicDerived.doAnotherStatic();

      new VerificationsInOrder() {{
         NonPublicBase.doStatic();
         NonPublicDerived.doAnotherStatic();
      }};
   }

   @Test
   public void dynamicallyMockLambdaObject()
   {
      Supplier<String> s = () -> "test";
      new NonStrictExpectations(s) {};

      assertEquals("test", s.get());
      new Verifications() {{ s.get(); }};
   }

   static class SomeClass
   {
      static String doSomethingStatic() { return "test1"; }
      String doSomething() { return "test2"; }
   }

   @Test
   public void dynamicallyMockReferenceToStaticMethod()
   {
      Supplier<String> s = SomeClass::doSomethingStatic;

      new Expectations(s) {};

      assertEquals("test1", s.get());
      new VerificationsInOrder() {{ s.get(); }};
   }

   @Ignore(
      "Fails on loadClass('...$$Lambda$1...' with ClassNotFoundException due to lambda not getting registered" +
      "with the system class loader; a JVM bug, apparently.")
   @Test
   public void dynamicallyMockReferenceToInstanceMethod() throws Exception
   {
      Supplier<String> s = new SomeClass()::doSomething;

      new NonStrictExpectations(s) {};

      assertEquals("test2", s.get());
//      new VerificationsInOrder() {{ s.get(); }};
   }

   @Ignore("Similar to previous test - apparent JVM issue")
   @Test
   public void dynamicallyMockLambdaObjectWithCapturedVariable()
   {
      SomeClass toBeCaptured = new SomeClass();
      //noinspection Convert2MethodRef
      Supplier<String> s = () -> toBeCaptured.doSomething();

      Startup.retransformClass(s.getClass());
//      new NonStrictExpectations(s) {};

      assertEquals("test2", s.get());
//      new Verifications() {{ s.get(); }};
   }

   @Test
   public void mockClock()
   {
      // Create a test clock with a fixed instant.
      LocalDateTime testDateTime = LocalDateTime.parse("2014-05-10T10:15:30");
      ZoneId zoneId = ZoneId.systemDefault();
      Instant testInstant = testDateTime.toInstant(zoneId.getRules().getOffset(testDateTime));
      Clock testClock = Clock.fixed(testInstant, zoneId);

      // In production code, obtain local date & time from the test clock.
      LocalDateTime now = LocalDateTime.now(testClock);

      assertEquals(testDateTime, now);
   }

   @Test
   public void mockLocalDateTime()
   {
      LocalDateTime testDateTime = LocalDateTime.parse("2014-05-10T09:35:12");

      new NonStrictExpectations(LocalDateTime.class) {{
         LocalDateTime.now(); result = testDateTime;
      }};

      LocalDateTime now = LocalDateTime.now();

      assertSame(testDateTime, now);
   }

   @Test
   public void mockInstant()
   {
      Instant testInstant = Instant.parse("2014-05-10T09:35:12Z");

      new NonStrictExpectations(Instant.class) {{
         Instant.now(); result = testInstant;
      }};

      Instant now = Instant.now();

      assertSame(testInstant, now);
   }
}
