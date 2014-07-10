/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.net.*;
import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class MockClassInstantiationPerSetupTest
{
   static final class RealClass1
   {
      static void doSomething() { throw new RuntimeException(); }
      int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   static final class RealClass2
   {
      static void doSomething() { throw new RuntimeException(); }
      int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   static final class RealClass3
   {
      static void doSomething() { throw new RuntimeException(); }
      int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   static final class RealClass4
   {
      static void doSomething() { throw new RuntimeException(); }
      int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   static final class MockClass1 extends MockUp<RealClass1>
   {
      static Object singleInstanceCreated;

      MockClass1()
      {
         assertNull(singleInstanceCreated);
         singleInstanceCreated = this;
      }

      @Mock void doSomething() { assertSame(singleInstanceCreated, this); }

      @Mock int performComputation(int a, boolean b)
      {
         assertSame(singleInstanceCreated, this);
         assertTrue(a > 0); assertTrue(b); return 2;
      }
   }

   static final class MockClass2 extends MockUp<RealClass2>
   {
      static Object singleInstanceCreated;

      MockClass2()
      {
         assertNull(singleInstanceCreated);
         singleInstanceCreated = this;
      }

      @Mock void doSomething() { assertSame(singleInstanceCreated, this); }

      @Mock int performComputation(int a, boolean b)
      {
         assertSame(singleInstanceCreated, this);
         assertTrue(a > 0); assertTrue(b); return 2;
      }
   }

   static final class MockClass3 extends MockUp<RealClass3>
   {
      static Object singleInstanceCreated;

      MockClass3()
      {
         assertNull(singleInstanceCreated);
         singleInstanceCreated = this;
      }

      @Mock void doSomething() { assertSame(singleInstanceCreated, this); }

      @Mock int performComputation(int a, boolean b)
      {
         assertSame(singleInstanceCreated, this);
         assertTrue(a > 0); assertTrue(b); return 2;
      }
   }

   static final class MockClass4 extends MockUp<RealClass4>
   {
      static Object singleInstanceCreated;

      MockClass4()
      {
         assertNull(singleInstanceCreated);
         singleInstanceCreated = this;
      }

      @Mock void doSomething() { assertSame(singleInstanceCreated, this); }

      @Mock int performComputation(int a, boolean b)
      {
         assertSame(singleInstanceCreated, this);
         assertTrue(a > 0);
         assertTrue(b);
         return 2;
      }
   }

   @BeforeClass
   public static void setUpClassLevelMocks()
   {
      new MockClass1();
   }

   @BeforeClass
   public static void setUpAdditionalClassLevelMocks()
   {
      new MockClass2();
   }

   @Before
   public void setUpMethodLevelMocks()
   {
      MockClass3.singleInstanceCreated = null;
      new MockClass3();
   }

   @Test
   public void mockInstancePerSetupInClassAndFixtureScopes()
   {
      assertMockClass1();
      assertMockClass2();
      assertMockClass3();
      assertEquals(1, new RealClass4().performComputation(1, true));
   }

   private void assertMockClass1()
   {
      RealClass1.doSomething();
      assertEquals(2, new RealClass1().performComputation(1, true));
   }

   private void assertMockClass2()
   {
      RealClass2.doSomething();
      assertEquals(2, new RealClass2().performComputation(1, true));
   }

   private void assertMockClass3()
   {
      RealClass3.doSomething();
      assertEquals(2, new RealClass3().performComputation(1, true));
   }

   private void assertMockClass4()
   {
      RealClass4.doSomething();
      assertEquals(2, new RealClass4().performComputation(1, true));
   }

   @Test
   public void mockInstancePerSetupInAllScopes()
   {
      new MockClass4();

      assertMockClass1();
      assertMockClass2();
      assertMockClass3();
      assertMockClass4();
   }

   public static final class MockURL extends MockUp<URL>
   {
      @Mock
      public InputStream openStream(Invocation inv) throws IOException
      {
         URL it = inv.getInvokedInstance();

         if ("test".equals(it.getHost())) {
            return new ByteArrayInputStream("response".getBytes());
         }

         return it.openStream();
      }
   }

   @Test
   public void reentrantMockForJREClass() throws Exception
   {
      new MockURL();

      InputStream response = new URL("http://test").openStream();

      assertEquals("response", new Scanner(response).nextLine());
   }
}