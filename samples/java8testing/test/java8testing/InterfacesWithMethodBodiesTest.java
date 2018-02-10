package java8testing;

import java.util.*;
import java.util.function.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import mockit.*;

final class InterfacesWithMethodBodiesTest
{
   @FunctionalInterface
   public interface InterfaceWithDefaultMethods {
      int regularMethod();
      default int defaultMethod() { return -1; }
   }

   public static final class ClassWhichOverridesDefaultMethodFromInterface implements InterfaceWithDefaultMethods {
      @Override public int regularMethod() { return 4; }
      @Override public int defaultMethod() { return 5; }
   }

   @Test
   void mockInterfaceWithDefaultMethods(@Mocked InterfaceWithDefaultMethods mock) {
      new Expectations() {{
         mock.defaultMethod(); result = 2;
         mock.regularMethod(); result = 1;
      }};

      assertEquals(1, mock.regularMethod());
      assertEquals(2, mock.defaultMethod());
   }

   @Test
   void mockClassWithOverriddenDefaultMethod(@Mocked ClassWhichOverridesDefaultMethodFromInterface mock) {
      new Expectations() {{
         mock.defaultMethod(); result = 2;
         mock.regularMethod(); result = 1;
      }};

      assertEquals(1, mock.regularMethod());
      assertEquals(2, mock.defaultMethod());
   }

   public static class ClassWhichInheritsDefaultMethodFromInterface implements InterfaceWithDefaultMethods {
      @Override public int regularMethod() { return 3; }
   }

   @Test
   void mockClassWithInheritedDefaultMethod(@Mocked ClassWhichInheritsDefaultMethodFromInterface mock) {
      new Expectations() {{ mock.defaultMethod(); result = 123; }};

      assertEquals(123, mock.defaultMethod());
   }

   public interface SubInterfaceWithDefaultMethods extends InterfaceWithDefaultMethods {
      default String anotherDefaultMethod(int i) { return String.valueOf(i); }
      @SuppressWarnings("unused") void anotherRegularMethod(boolean b, String... names);
   }

   static final class ClassInheritingFromInterfaceHierarchy implements SubInterfaceWithDefaultMethods {
      @Override public int regularMethod() { return 4; }
      @Override public void anotherRegularMethod(boolean b, String... names) {}
   }

   @Test
   void mockClassInheritingFromInterfaceHierarchy(@Injectable ClassInheritingFromInterfaceHierarchy mock) {
      new Expectations() {{
         mock.defaultMethod(); result = 123;
         mock.regularMethod(); result = 22;
         mock.anotherDefaultMethod(anyInt); result = "one";
      }};

      assertEquals(123, mock.defaultMethod());
      assertEquals(22, mock.regularMethod());
      assertEquals("one", mock.anotherDefaultMethod(1));
   }

   public interface AnotherInterfaceWithDefaultMethods {
      default int defaultMethod1() { return 1; }
      default int defaultMethod2() { return 2; }
   }

   static final class ClassInheritingMultipleDefaultMethods implements SubInterfaceWithDefaultMethods, AnotherInterfaceWithDefaultMethods {
      @Override public int regularMethod() { return 5; }
      @Override public void anotherRegularMethod(boolean b, String... names) {}
   }

   @Test
   void partiallyMockClassInheritingDefaultMethodsFromMultipleInterfaces() {
      ClassInheritingMultipleDefaultMethods obj = new ClassInheritingMultipleDefaultMethods();

      new Expectations(ClassInheritingMultipleDefaultMethods.class) {{
         obj.defaultMethod(); result = 123;
         obj.defaultMethod2(); result = 22;
         obj.anotherDefaultMethod(1); result = "one";
      }};

      assertEquals(123, obj.defaultMethod());
      assertEquals(5, obj.regularMethod());
      assertEquals(1, obj.defaultMethod1());
      assertEquals(22, obj.defaultMethod2());
      assertEquals("one", obj.anotherDefaultMethod(1));
      obj.anotherRegularMethod(true);

      new Verifications() {{ obj.anotherRegularMethod(anyBoolean, (String[]) any); }};
   }

   public interface InterfaceWithStaticMethod { static InterfaceWithStaticMethod newInstance() { return null; } }

   @Test
   void mockStaticMethodInInterface(@Mocked InterfaceWithStaticMethod mock) {
      InterfaceWithStaticMethod actual = InterfaceWithStaticMethod.newInstance();
      assertSame(mock, actual);
   }

   @Test
   void mockFunctionalInterfaceFromJRE(@Mocked Consumer<String> mockConsumer) {
      StringBuilder concatenated = new StringBuilder();

      new Expectations() {{
         mockConsumer.accept(anyString);
         result = new Delegate() {
            @Mock void delegate(String s) { concatenated.append(s).append(' '); }
         };
      }};

      List<String> list = Arrays.asList("mocking", "a", "lambda");
      list.forEach(mockConsumer);

      assertEquals("mocking a lambda ", concatenated.toString());
   }

   interface NonPublicBase {
      default int baseDefault() { return -1; }
      default String getDefault() { return "default"; }
      static void doStatic() { throw new RuntimeException("1"); }
   }

   interface NonPublicDerived extends NonPublicBase {
      @Override default String getDefault() { return "default derived"; }
      static void doAnotherStatic() { throw new RuntimeException("2"); }
   }

   @Test
   void mockNonPublicInterfaceHierarchyWithDefaultAndStaticMethods(@Mocked NonPublicBase base, @Mocked NonPublicDerived derived) {
      new Expectations() {{
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
}
