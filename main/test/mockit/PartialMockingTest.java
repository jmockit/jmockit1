package mockit;

import java.lang.reflect.*;
import java.util.*;
import javax.xml.bind.annotation.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

@SuppressWarnings("deprecation")
public final class PartialMockingTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Deprecated
   static class Collaborator {
      @Deprecated protected int value;

      Collaborator() { value = -1; }
      @Deprecated Collaborator(@Deprecated int value) { this.value = value; }

      final int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      @SuppressWarnings("unused")
      final boolean simpleOperation(int a, @XmlElement(name = "test") String b, Date c) { return true; }

      static void doSomething(@SuppressWarnings("unused") boolean b, @SuppressWarnings("unused") String s) {
         throw new IllegalStateException();
      }

      @Ignore("test")
      boolean methodWhichCallsAnotherInTheSameClass() {
         return simpleOperation(1, "internal", null);
      }
      
      String overridableMethod() { return "base"; }
   }

   @Test
   public void attemptToPartiallyMockAClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid Class");
      thrown.expectMessage("partial mocking");
      thrown.expectMessage("Collaborator");

      new Expectations(Collaborator.class) {};
   }

   @Test
   public void dynamicMockFullyVerified_verifyAllRecordedExpectationsButNotAllOfTheReplayedOnes() {
      final Collaborator collaborator = new Collaborator(0);

      new Expectations(collaborator) {{ collaborator.setValue(1); }};

      collaborator.setValue(1);
      collaborator.setValue(2);

      // Verifies all the *mocked* invocations that would be left unverified; ignores those not mocked:
      new FullVerifications() {
         // No need to verify "setValue(1)" since it was recorded and implicitly verified.
         // No need to verify "setValue(2)" since it was not recorded.
      };
   }

   @Test
   public void dynamicMockFullyVerifiedInOrder_verifyAllRecordedExpectationsButNotAllOfTheReplayedOnes() {
      final Collaborator collaborator = new Collaborator(0);

      new Expectations(collaborator) {{
         collaborator.setValue(2);
         collaborator.setValue(3);
      }};

      collaborator.setValue(1);
      collaborator.setValue(2);
      collaborator.setValue(3);

      // Verifies all the *mocked* (recorded) invocations, ignoring those not mocked:
      new VerificationsInOrder() {{
         // No need to verify "setValue(1)" since it was not recorded.
         collaborator.setValue(2);
         collaborator.setValue(3);
      }};
      new FullVerifications() {};
   }

   @Test
   public void expectTwoInvocationsOnDynamicMockButReplayOnce() {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{ collaborator.getValue(); times = 2; }};

      assertEquals(0, collaborator.getValue());
      thrown.expect(MissingInvocation.class);
   }

   @Test
   public void expectOneInvocationOnDynamicMockButReplayTwice() {
      final Collaborator collaborator = new Collaborator(1);

      new Expectations(collaborator) {{ collaborator.getValue(); times = 1; }};

      // Mocked:
      assertEquals(0, collaborator.getValue());

      // Still mocked because it's not strict:
      thrown.expect(UnexpectedInvocation.class);
      assertEquals(0, collaborator.getValue());
   }

   @Test
   public void dynamicallyMockAnInstance() {
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

      new Verifications() {{ collaborator.getValue(); times = 1; }};
   }

   @Test
   public void mockMethodInSameClass() {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{ collaborator.simpleOperation(1, anyString, null); result = false; }};

      assertFalse(collaborator.methodWhichCallsAnotherInTheSameClass());
      assertTrue(collaborator.simpleOperation(2, "", null));
      assertFalse(collaborator.simpleOperation(1, "", null));
   }

   static final class SubCollaborator extends Collaborator {
      @SuppressWarnings("unused") SubCollaborator() { this(1); }
      SubCollaborator(int value) { super(value); }

      @Override
      String overridableMethod() { return super.overridableMethod() + " overridden"; }

      String format() { return String.valueOf(value); }
      static void causeFailure() { throw new RuntimeException(); }
   }

   @Test
   public void dynamicallyMockASubCollaboratorInstance() {
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

   interface Dependency {
      boolean doSomething();
      List<?> doSomethingElse(int n);
   }

   @Test
   public void dynamicallyMockAnAnonymousClassInstanceThroughTheImplementedInterface() {
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
      @Mocked AnotherInterface publicInterfaceMock, @Injectable NonPublicInterface nonPublicInterfaceMock
   ) {
      assertInvalidTypeForDynamicPartialMocking(new String[1]);
      assertInvalidTypeForDynamicPartialMocking(123);
      assertInvalidTypeForDynamicPartialMocking(true);
      assertInvalidTypeForDynamicPartialMocking(2.5);
      assertInvalidTypeForDynamicPartialMocking(publicInterfaceMock);
      assertInvalidTypeForDynamicPartialMocking(nonPublicInterfaceMock);
   }

   void assertInvalidTypeForDynamicPartialMocking(Object object) {
      try {
         new Expectations(object) {};
         fail();
      }
      catch (IllegalArgumentException e) {
         assertTrue(e.getMessage().contains("partial mocking"));
      }
   }

   @Test
   public void dynamicPartialMockingWithExactArgumentMatching() {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{ collaborator.simpleOperation(1, "s", null); result = false; }};

      assertFalse(collaborator.simpleOperation(1, "s", null));
      assertTrue(collaborator.simpleOperation(2, "s", null));
      assertTrue(collaborator.simpleOperation(1, "S", null));
      assertTrue(collaborator.simpleOperation(1, "s", new Date()));
      assertTrue(collaborator.simpleOperation(1, null, new Date()));
      assertFalse(collaborator.simpleOperation(1, "s", null));

      new FullVerifications() {{ collaborator.simpleOperation(anyInt, null, null); }};
   }

   @Test
   public void dynamicPartialMockingWithFlexibleArgumentMatching() {
      final Collaborator mock = new Collaborator();

      new Expectations(mock) {{ mock.simpleOperation(anyInt, withPrefix("s"), null); result = false; }};

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
   public void dynamicPartialMockingWithInstanceSpecificMatching() {
      final Collaborator collaborator1 = new Collaborator();
      final Collaborator collaborator2 = new Collaborator(4);

      new Expectations(collaborator1, collaborator2) {{ collaborator1.getValue(); result = 3; }};

      assertEquals(3, collaborator1.getValue());
      assertEquals(4, collaborator2.getValue());

      new VerificationsInOrder() {{
         collaborator1.getValue(); times = 1;
         collaborator2.getValue(); times = 1;
      }};
   }

   @Test
   public void dynamicPartialMockingWithInstanceSpecificMatchingOnTwoInstancesOfSameClass() {
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
   public void methodWithNoRecordedExpectationCalledTwiceDuringReplay() {
      final Collaborator collaborator = new Collaborator(123);

      new Expectations(collaborator) {};

      assertEquals(123, collaborator.getValue());
      assertEquals(123, collaborator.getValue());

      new FullVerifications() {{ collaborator.getValue(); times = 2; }};
   }

   static final class ClassWithNative {
      int doSomething() { return nativeMethod(); }
      private native int nativeMethod();
   }

   @Test
   public void attemptToPartiallyMockNativeMethod() {
      thrown.expect(UnsatisfiedLinkError.class);

      final ClassWithNative mock = new ClassWithNative();

      new Expectations(mock) {{
         // The native method is ignored when using dynamic mocking, so this actually tries to execute the real method,
         // failing since there is no native implementation.
         mock.nativeMethod();
      }};
   }

   @Test
   public void mockAnnotatedConstructor(@Mocked Collaborator mock) throws Exception {
      Constructor<?> mockedConstructor = Collaborator.class.getDeclaredConstructor(int.class);

      assertTrue(mockedConstructor.isAnnotationPresent(Deprecated.class));
      assertTrue(mockedConstructor.getParameterAnnotations()[0][0] instanceof Deprecated);
   }

   static final class TestedClass {
      TestedClass() { this(true); }
      TestedClass(boolean value) { initialize(value); }
      private void initialize(@SuppressWarnings("unused") boolean flag) {}
   }

   static class BaseClass { @SuppressWarnings("unused") BaseClass(Object o) { assert o != null; } BaseClass() {} }
   static class SubClass extends BaseClass {}
   static class SubSubClass extends SubClass {}

   @Test
   public void mockClassIndirectlyExtendingBaseWhoseFirstConstructorHasMoreParametersThanTheSecondOne(@Mocked SubSubClass mock) {
      new SubClass();
   }
}