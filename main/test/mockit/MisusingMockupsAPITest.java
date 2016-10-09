/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.applet.*;
import java.io.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.MockUpTest.SomeInterface;

public final class MisusingMockupsAPITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void applySameMockClassWhilePreviousApplicationStillActive()
   {
      new SomeMockUp(2);
      assertEquals(2, new Applet().getComponentCount());

      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Duplicate application");
      thrown.expectMessage("same mock-up");

      new SomeMockUp(3);
   }

   static final class SomeMockUp extends MockUp<Applet>
   {
      final int value;
      SomeMockUp(int value) { this.value = value; }
      @Mock int getComponentCount() { return value; }
   }

   @Test
   public void applySameMockClassUsingSecondaryConstructorWhilePreviousApplicationStillActive()
   {
      new AnotherMockUp(2);
      assertEquals(2, new Applet().getComponentCount());

      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Duplicate application");
      thrown.expectMessage("same mock-up");

      new AnotherMockUp(3);
   }

   static final class AnotherMockUp extends MockUp<Applet>
   {
      final int value;
      AnotherMockUp(int value) { super(Applet.class); this.value = value; }
      @Mock int getComponentCount() { return value; }
   }

   @Test
   public void mockSameMethodTwiceWithReentrantMocksFromTwoDifferentMockClasses()
   {
      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int i = inv.proceed();
            return i + 1;
         }
      };

      int i = new Applet().getComponentCount();
      assertEquals(1, i);

      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int j = inv.proceed();
            return j + 2;
         }
      };

      // Should return 3, but returns 5. Chaining mock methods is not supported.
      int j = new Applet().getComponentCount();
      assertEquals(5, j);
   }

   @Mocked Process mockProcess;

   @Test
   public void mockUpMethodInClassWhichIsAlreadyMocked()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("already mocked");
      thrown.expectMessage("Process");

      new MockUp<Process>() {};
   }

   @Test
   public void attemptToHaveMockMethodWithInvocationParameterNotAtFirstPosition()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Mock method");
      thrown.expectMessage("Invocation parameter");
      thrown.expectMessage("first");

      new MockUp<Applet>() {
         @Mock void resize(int width, int height, Invocation inv) {}
      };
   }

   @Test
   public <X> void attemptToApplyMockUpFromUnboundedTypeParameter()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Unbounded base type");
      thrown.expectMessage("\"X\"");

      new MockUp<X>() {};
   }

   @Test
   public <BI extends SomeInterface> void attemptToGetMockInstanceFromMockUpForAllClassesImplementingBaseInterface()
   {
      MockUp<BI> mockUp = new MockUp<BI>() {};

      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("No single instance");

      mockUp.getMockInstance();
   }

   public interface AnInterface { void doSomething(); }

   @Test
   public void attemptToProceedIntoEmptyMethodOfPublicInterface()
   {
      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("Cannot proceed");
      thrown.expectMessage("interface method");

      AnInterface mock = new MockUp<AnInterface>() {
         @Mock
         void doSomething(Invocation invocation) { invocation.proceed(); }
      }.getMockInstance();

      mock.doSomething();
   }

   final PrintStream originalOutput = System.err;
   final OutputStream output = new ByteArrayOutputStream();
   @Before public void redirectStandardErrorOutput() { System.setErr(new PrintStream(output)); }
   @After  public void restoreStandardErrorOutput()  { System.setErr(originalOutput); }

   public static class Dependency
   {
      private Dependency() {}
      @SuppressWarnings("unused") int getCount() { return 1; }
   }

   @Test
   public void attemptToFakeClassFromTestedCodebase()
   {
      new MockUp<Dependency>() {};

      String warningMessage = output.toString();
      assertTrue(warningMessage.contains("Invalid mock-up for internal class"));
      assertTrue(warningMessage.contains("Dependency"));
   }

   @Test
   public void attemptToFakeInternalMethodInClassFromTestedCodebase()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid mock");
      thrown.expectMessage("getCount()");
      thrown.expectMessage("package-private method");

      new MockUp<Dependency>() {
         @Mock int getCount() { return 0; }
      };
   }

   @Test
   public void attemptToFakeInternalConstructorInClassFromTestedCodebase()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid mock");
      thrown.expectMessage("$init()");
      thrown.expectMessage("private constructor");

      new MockUp<Dependency>() {
         @Mock void $init() {}
      };
   }

   @Test
   public void getMockInstanceFromStatelessClassMockupCreatedWithoutATargetInstance()
   {
      MockUp<Applet> mockUp = new MockUp<Applet>() {};

      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Invalid");
      thrown.expectMessage("uninitialized instance");
      thrown.expectMessage("Applet");

      mockUp.getMockInstance();
   }

   @Test
   public void attemptToApplyMockupFromTestHavingMockParameters(@Mocked Runnable mock)
   {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Invalid application of mock-up from test with mock parameters");

      new SomeMockUp(1);
   }
}