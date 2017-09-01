/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.xml.bind.annotation.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.*;

@SuppressWarnings("deprecation")
public final class DynamicPartialMockingTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("unused")
   @Deprecated
   static class Collaborator
   {
      @Deprecated protected int value;

      Collaborator() { value = -1; }
      @Deprecated Collaborator(@Deprecated int value) { this.value = value; }

      final int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      final boolean simpleOperation(int a, @XmlElement(name = "test") String b, Date c) { return true; }
      static void doSomething(boolean b, String s) { throw new IllegalStateException(); }

      @Ignore("test")
      boolean methodWhichCallsAnotherInTheSameClass()
      {
         return simpleOperation(1, "internal", null);
      }
      
      String overridableMethod() { return "base"; }
      @SuppressWarnings("DeprecatedIsStillUsed")
      @Deprecated native void nativeMethod();

      void readFile(File f) {}
      private void initialize() {}
   }

   interface Dependency
   {
      boolean doSomething();
      List<?> doSomethingElse(int n);
   }

   @Test
   public void dynamicallyMockJREClass() throws Exception
   {
      new Expectations(ByteArrayOutputStream.class) {{
         new ByteArrayOutputStream().size(); result = 123;
      }};

      // Mocked:
      ByteArrayOutputStream collaborator = new ByteArrayOutputStream();
      assertNull(Deencapsulation.getField(collaborator, "buf"));
      assertEquals(123, collaborator.size());

      // Not mocked:
      ByteArrayOutputStream buf = new ByteArrayOutputStream(200);
      buf.write(65);
      assertEquals("A", buf.toString("UTF-8"));
   }

   @Test
   public void dynamicallyMockAClass()
   {
      new Expectations(Collaborator.class) {{ new Collaborator().getValue(); result = 123; }};

      // Mocked:
      final Collaborator col1 = new Collaborator();
      assertEquals(123, col1.getValue());

      // Not mocked:
      final Collaborator col2 = new Collaborator(200);
      col2.setValue(45);
      assertEquals(45, col2.value);
      assertEquals(45, col2.getValue());

      new Verifications() {{
         col1.getValue(); times = 1;
         col2.getValue(); times = 1;

         Collaborator col2Equivalent = new Collaborator(200); times = 1;
         col2Equivalent.getValue(); times = 1;
      }};
   }

   @Test
   public void mockOnlyTheFutureObjectsThatMatchASpecificConstructorInvocation()
   {
      // Not mocked:
      Collaborator f0 = new Collaborator(12);
      assertEquals(12, f0.getValue());

      // Applies partial mocking to all instances.
      new Expectations(Collaborator.class) {{
         Collaborator anyFutureInstanceWithValue1 = new Collaborator(1);
         anyFutureInstanceWithValue1.overridableMethod(); result = "mock"; times = 2;
      }};

      // Mocked:
      Collaborator f1 = new Collaborator(1);
      assertEquals("mock", f1.overridableMethod());

      // Not mocked:
      Collaborator f2 = new Collaborator(2);
      assertEquals("base", f2.overridableMethod());

      // Also mocked:
      Collaborator f3 = new Collaborator(1);
      assertEquals("mock", f3.overridableMethod());

      // Invocations to non-mocked instances can also be verified (excluding those existing before mocking was applied).
      new Verifications() {{
         Collaborator anyOtherInstance = new Collaborator(withNotEqual(1));
         anyOtherInstance.overridableMethod(); times = 1;
      }};
   }

   @Test
   public void verifyFutureMockedAndNonMockedObjectsInOrder()
   {
      new Expectations(Collaborator.class) {{
         Collaborator anyFutureInstanceWithValue1 = new Collaborator(1);
         anyFutureInstanceWithValue1.getValue(); result = 15;
      }};

      Collaborator f1 = new Collaborator(1);
      assertEquals(15, f1.getValue());

      Collaborator f2 = new Collaborator(2);
      assertEquals(2, f2.getValue());
      assertEquals("base", f2.overridableMethod());

      assertEquals("base", f1.overridableMethod());

      Collaborator f3 = new Collaborator(1);
      assertEquals(15, f3.getValue());

      new FullVerificationsInOrder() {{
         Collaborator anyInstanceWithValue1 = new Collaborator(1);
         anyInstanceWithValue1.getValue();

         Collaborator anyOtherInstance = new Collaborator(withNotEqual(1));
         anyOtherInstance.getValue();
         anyOtherInstance.overridableMethod();

         anyInstanceWithValue1.overridableMethod();

         new Collaborator(1);
         anyInstanceWithValue1.getValue();
      }};
   }

   @Test
   public void dynamicMockFullyVerified_verifyAllRecordedExpectationsButNotAllOfTheReplayedOnes()
   {
      final Collaborator collaborator = new Collaborator(0);

      new Expectations(collaborator) {{
         collaborator.setValue(1);
      }};

      collaborator.setValue(1);
      collaborator.setValue(2);

      // Verifies all the *mocked* invocations that would be left unverified; ignores those not mocked:
      new FullVerifications() {
         // No need to verify "setValue(1)" since it was recorded and implicitly verified.
         // No need to verify "setValue(2)" since it was not recorded.
      };
   }

   @Test
   public void dynamicMockFullyVerifiedInOrder_verifyAllRecordedExpectationsButNotAllOfTheReplayedOnes()
   {
      final Collaborator collaborator = new Collaborator(0);

      new Expectations(collaborator) {{
         collaborator.setValue(2);
         collaborator.setValue(3);
      }};

      collaborator.setValue(1);
      collaborator.setValue(2);
      collaborator.setValue(3);

      // Verifies all the *mocked* (recorded) invocations, ignoring those not mocked:
      new FullVerificationsInOrder() {{
         // No need to verify "setValue(1)" since it was not recorded.
         collaborator.setValue(2);
         collaborator.setValue(3);
      }};
   }

   @Test
   public void dynamicallyMockedClassFullyVerified_verifyRecordedExpectationButNotReplayedOne()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(Collaborator.class) {{
         collaborator.simpleOperation(1, "internal", null);
         result = false;
      }};

      assertFalse(collaborator.methodWhichCallsAnotherInTheSameClass());

      new FullVerifications() {{
         collaborator.simpleOperation(anyInt, anyString, null);
      }};
   }

   @Test
   public void expectTwoInvocationsOnDynamicMockButReplayOnce()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.getValue(); times = 2;
      }};

      assertEquals(0, collaborator.getValue());
      thrown.expect(MissingInvocation.class);
   }

   @Test
   public void expectOneInvocationOnDynamicMockButReplayTwice()
   {
      final Collaborator collaborator = new Collaborator(1);

      new Expectations(collaborator) {{
         collaborator.getValue(); times = 1;
      }};

      // Mocked:
      assertEquals(0, collaborator.getValue());

      // Still mocked because it's not strict:
      thrown.expect(UnexpectedInvocation.class);
      assertEquals(0, collaborator.getValue());
   }

   @Test
   public void dynamicallyMockAnInstance()
   {
      final Collaborator collaborator = new Collaborator(2);

      new Expectations(collaborator) {{
         collaborator.simpleOperation(1, "", null); result = false;
         Collaborator.doSomething(anyBoolean, "test");
      }};

      // Mocked:
      assertFalse(collaborator.simpleOperation(1, "", null));
      Collaborator.doSomething(true, "test");

      // Not mocked:
      assertEquals(2, collaborator.getValue());
      assertEquals(45, new Collaborator(45).value);
      assertEquals(-1, new Collaborator().value);

      try {
         Collaborator.doSomething(false, null);
         fail();
      }
      catch (IllegalStateException ignore) {}

      new Verifications() {{
         collaborator.getValue(); times = 1;
      }};
   }

   @Test
   public void mockMethodInSameClass()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.simpleOperation(1, anyString, null); result = false;
      }};

      assertFalse(collaborator.methodWhichCallsAnotherInTheSameClass());
      assertTrue(collaborator.simpleOperation(2, "", null));
      assertFalse(collaborator.simpleOperation(1, "", null));
   }

   static final class SubCollaborator extends Collaborator
   {
      SubCollaborator() { this(1); }
      SubCollaborator(int value) { super(value); }

      @Override
      String overridableMethod() { return super.overridableMethod() + " overridden"; }

      String format() { return String.valueOf(value); }
      static void causeFailure() { throw new RuntimeException(); }
   }

   @Test
   public void dynamicallyMockASubCollaboratorInstance()
   {
      final SubCollaborator collaborator = new SubCollaborator();

      new Expectations(collaborator) {{
         collaborator.getValue(); result = 5;
         collaborator.format(); result = "test";
         SubCollaborator.causeFailure();
      }};

      // Mocked:
      assertEquals(5, collaborator.getValue());
      SubCollaborator.causeFailure();
      assertEquals("test", collaborator.format());

      // Not mocked:
      assertTrue(collaborator.simpleOperation(0, null, null)); // not recorded
      assertEquals("1", new SubCollaborator().format()); // was recorded but on a different instance

      try {
         Collaborator.doSomething(true, null); // not recorded
         fail();
      }
      catch (IllegalStateException ignore) {}
   }

   @Test
   public void dynamicallyMockClassHierarchyForSpecifiedSubclass()
   {
      final SubCollaborator collaborator = new SubCollaborator();

      new Expectations(SubCollaborator.class) {{
         collaborator.getValue(); result = 123;
         collaborator.format(); result = "test";
      }};

      // Mocked:
      assertEquals("test", collaborator.format());
      assertEquals(123, collaborator.getValue());

      // Not mocked:
      assertTrue(collaborator.simpleOperation(0, null, null));

      // Mocked sub-constructor/not mocked base constructor:
      assertEquals(-1, new SubCollaborator().value);

      new VerificationsInOrder() {{
         collaborator.format();
         new SubCollaborator();
      }};
   }

   @Test
   public void mockTheBaseMethodWhileExercisingTheOverride()
   {
      final Collaborator collaborator = new Collaborator();
      
      new Expectations(Collaborator.class) {{
         collaborator.overridableMethod(); result = ""; result = "mocked";
      }};

      assertEquals("", collaborator.overridableMethod());
      assertEquals("mocked overridden", new SubCollaborator().overridableMethod());
   }

   @Test
   public void dynamicallyMockAnAnonymousClassInstanceThroughTheImplementedInterface()
   {
      final Collaborator collaborator = new Collaborator();

      final Dependency dependency = new Dependency() {
         @Override public boolean doSomething() { return false; }
         @Override public List<?> doSomethingElse(int n) { return null; }
      };
      
      new Expectations(collaborator, dependency) {{
         collaborator.getValue(); result = 5;
         dependency.doSomething(); result = true;
      }};

      // Mocked:
      assertEquals(5, collaborator.getValue());
      assertTrue(dependency.doSomething());

      // Not mocked:
      assertTrue(collaborator.simpleOperation(0, null, null));
      assertNull(dependency.doSomethingElse(3));

      new FullVerifications() {{
         dependency.doSomethingElse(anyInt);
         collaborator.simpleOperation(0, null, null);
      }};
   }

   public interface AnotherInterface {}
   interface NonPublicInterface {}

   @Test
   public void attemptToUseDynamicMockingForInvalidTypes(
      @Mocked AnotherInterface publicInterfaceMock, @Injectable NonPublicInterface nonPublicInterfaceMock)
   {
      assertInvalidTypeForDynamicPartialMocking(Dependency.class);
      assertInvalidTypeForDynamicPartialMocking(Test.class);
      assertInvalidTypeForDynamicPartialMocking(int[].class);
      assertInvalidTypeForDynamicPartialMocking(new String[1]);
      assertInvalidTypeForDynamicPartialMocking(char.class);
      assertInvalidTypeForDynamicPartialMocking(123);
      assertInvalidTypeForDynamicPartialMocking(Boolean.class);
      assertInvalidTypeForDynamicPartialMocking(true);
      assertInvalidTypeForDynamicPartialMocking(2.5);
      assertInvalidTypeForDynamicPartialMocking(publicInterfaceMock);
      assertInvalidTypeForDynamicPartialMocking(nonPublicInterfaceMock);
   }

   void assertInvalidTypeForDynamicPartialMocking(Object classOrObject)
   {
      try {
         new Expectations(classOrObject) {};
         fail();
      }
      catch (IllegalArgumentException e) {
         assertTrue(e.getMessage().contains("partial mocking"));
      }
   }

   @Test
   public void dynamicPartialMockingWithExactArgumentMatching()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.simpleOperation(1, "s", null); result = false;
      }};

      assertFalse(collaborator.simpleOperation(1, "s", null));
      assertTrue(collaborator.simpleOperation(2, "s", null));
      assertTrue(collaborator.simpleOperation(1, "S", null));
      assertTrue(collaborator.simpleOperation(1, "s", new Date()));
      assertTrue(collaborator.simpleOperation(1, null, new Date()));
      assertFalse(collaborator.simpleOperation(1, "s", null));

      new FullVerifications() {{
         collaborator.simpleOperation(anyInt, null, null);
      }};
   }

   @Test
   public void dynamicPartialMockingWithFlexibleArgumentMatching()
   {
      final Collaborator mock = new Collaborator();

      new Expectations(mock) {{
         mock.simpleOperation(anyInt, withPrefix("s"), null); result = false;
      }};

      assertFalse(mock.simpleOperation(1, "sSs", null));
      assertTrue(mock.simpleOperation(2, " s", null));
      assertTrue(mock.simpleOperation(1, "S", null));
      assertFalse(mock.simpleOperation(-1, "s", new Date()));
      assertTrue(mock.simpleOperation(1, null, null));
      assertFalse(mock.simpleOperation(0, "string", null));

      Collaborator collaborator = new Collaborator();
      assertTrue(collaborator.simpleOperation(1, "sSs", null));
      assertTrue(collaborator.simpleOperation(-1, null, new Date()));
   }

   @Test
   public void dynamicPartialMockingWithInstanceSpecificMatching()
   {
      final Collaborator collaborator1 = new Collaborator();
      final Collaborator collaborator2 = new Collaborator(4);

      new Expectations(collaborator1, collaborator2) {{
         collaborator1.getValue(); result = 3;
      }};

      assertEquals(3, collaborator1.getValue());
      assertEquals(4, collaborator2.getValue());

      new FullVerificationsInOrder() {{
         collaborator1.getValue(); times = 1;
         collaborator2.getValue(); times = 1;
      }};
   }

   @Test
   public void dynamicPartialMockingWithInstanceSpecificMatchingOnTwoInstancesOfSameClass()
   {
      final Collaborator mock1 = new Collaborator();
      final Collaborator mock2 = new Collaborator();

      new Expectations(mock1, mock2) {{
         mock1.getValue(); result = 1;
         mock2.getValue(); result = 2;
      }};

      assertEquals(2, mock2.getValue());
      assertEquals(1, mock1.getValue());
   }

   @Test
   public void methodWithNoRecordedExpectationCalledTwiceDuringReplay()
   {
      final Collaborator collaborator = new Collaborator(123);

      new Expectations(collaborator) {};

      assertEquals(123, collaborator.getValue());
      assertEquals(123, collaborator.getValue());

      new FullVerifications() {{
         collaborator.getValue(); times = 2;
      }};
   }

   static class ClassWithStaticInitializer
   {
      static boolean initialized = true;
      static int doSomething() { return initialized ? 1 : -1; }
   }

   @Test
   public void doNotStubOutStaticInitializersWhenDynamicallyMockingAClass()
   {
      new Expectations(ClassWithStaticInitializer.class) {{
         ClassWithStaticInitializer.doSomething(); result = 2;
      }};

      assertEquals(2, ClassWithStaticInitializer.doSomething());
      assertTrue(ClassWithStaticInitializer.initialized);
   }

   static final class ClassWithNative
   {
      int doSomething() { return nativeMethod(); }
      private native int nativeMethod();
   }

   @Test
   public void attemptToPartiallyMockNativeMethod()
   {
      thrown.expect(UnsatisfiedLinkError.class);

      final ClassWithNative mock = new ClassWithNative();

      new Expectations(mock) {{
         // The native method is ignored when using dynamic mocking, so this actually tries to execute the real method,
         // failing since there is no native implementation.
         mock.nativeMethod();
      }};
   }

   @Test
   public void mockedClassWithAnnotatedElements() throws Exception
   {
      new Expectations(Collaborator.class) {};

      Collaborator mock = new Collaborator(123);
      Class<?> mockedClass = mock.getClass();

      assertTrue(mockedClass.isAnnotationPresent(Deprecated.class));
      assertTrue(mockedClass.getDeclaredField("value").isAnnotationPresent(Deprecated.class));

      Method mockedMethod1 = mockedClass.getDeclaredMethod("simpleOperation", int.class, String.class, Date.class);
      Annotation xmlElement = mockedMethod1.getParameterAnnotations()[1][0];
      assertTrue(xmlElement instanceof XmlElement);
      assertEquals("test", ((XmlElement) xmlElement).name());

      Method mockedMethod2 = mockedClass.getDeclaredMethod("methodWhichCallsAnotherInTheSameClass");
      Ignore ignore = mockedMethod2.getAnnotation(Ignore.class);
      assertNotNull(ignore);
      assertEquals("test", ignore.value());

      assertTrue(mockedClass.getDeclaredMethod("nativeMethod").isAnnotationPresent(Deprecated.class));
   }

   @Test
   public void mockAnnotatedConstructor(@Mocked Collaborator mock) throws Exception
   {
      Constructor<?> mockedConstructor = Collaborator.class.getDeclaredConstructor(int.class);

      assertTrue(mockedConstructor.isAnnotationPresent(Deprecated.class));
      assertTrue(mockedConstructor.getParameterAnnotations()[0][0] instanceof Deprecated);
   }

   @Test
   public void regularMockedMethodCallingOverriddenEqualsInDynamicallyMockedClass(@Mocked final Collaborator mock)
   {
      @SuppressWarnings("TooBroadScope") final File f = new File("test");

      new Expectations(File.class) {};

      mock.readFile(new File("test"));

      new Verifications() {{
         mock.readFile(f);
      }};
   }

   static final class TestedClass
   {
      private boolean value;

      TestedClass() { this(true); }
      TestedClass(boolean value) { initialize(value); }

      private void initialize(boolean flag) { value = flag; }
   }

   @Test
   public void mockClassWithConstructorWhichCallsPrivateMethod()
   {
      new Expectations(TestedClass.class) {};

      assertTrue(new TestedClass(true).value);
      assertFalse(new TestedClass(false).value);

      new Verifications() {{
         TestedClass t = new TestedClass(anyBoolean); times = 2;
         t.initialize(anyBoolean); times = 2;
      }};
   }

   static class Action implements Runnable { @Override public void run() {} }

   @Test
   public void partiallyMockAClassImplementingAMockedInterface(@Mocked Runnable mock)
   {
      new Expectations(Action.class) {};
   }

   static class Base { List<String> list() { return null; } }
   static class Sub extends Base {}

   @Test
   public void callIterableReturningInstanceMethodOnPartiallyMockedSubclassWithNoRecordedExpectations()
   {
      new Expectations(Sub.class) {};

      Object result = new Base().list();

      assertNull(result);
   }
}
