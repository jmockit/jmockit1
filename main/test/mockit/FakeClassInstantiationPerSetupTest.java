/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.net.*;
import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class FakeClassInstantiationPerSetupTest
{
   public static final class RealClass1
   {
      public static void doSomething() { throw new RuntimeException(); }
      public int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   public static final class RealClass2
   {
      public static void doSomething() { throw new RuntimeException(); }
      public int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   public static final class RealClass3
   {
      public static void doSomething() { throw new RuntimeException(); }
      public int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   public static final class RealClass4
   {
      public static void doSomething() { throw new RuntimeException(); }
      public int performComputation(int a, boolean b) { return b ? a : -a; }
   }

   static final class FakeClass1 extends MockUp<RealClass1>
   {
      static Object singleInstanceCreated;

      FakeClass1()
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

   static final class FakeClass2 extends MockUp<RealClass2>
   {
      static Object singleInstanceCreated;

      FakeClass2()
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

   static final class FakeClass3 extends MockUp<RealClass3>
   {
      static Object singleInstanceCreated;

      FakeClass3()
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

   static final class FakeClass4 extends MockUp<RealClass4>
   {
      static Object singleInstanceCreated;

      FakeClass4()
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
   public static void setUpClassLevelFakes()
   {
      new FakeClass1();
   }

   @BeforeClass
   public static void setUpAdditionalClassLevelFakes()
   {
      new FakeClass2();
   }

   @Before
   public void setUpMethodLevelFakes()
   {
      FakeClass3.singleInstanceCreated = null;
      new FakeClass3();
   }

   @Test
   public void fakeInstancePerSetupInClassAndFixtureScopes()
   {
      assertFakeClass1();
      assertFakeClass2();
      assertFakeClass3();
      assertEquals(1, new RealClass4().performComputation(1, true));
   }

   void assertFakeClass1()
   {
      RealClass1.doSomething();
      assertEquals(2, new RealClass1().performComputation(1, true));
   }

   void assertFakeClass2()
   {
      RealClass2.doSomething();
      assertEquals(2, new RealClass2().performComputation(1, true));
   }

   void assertFakeClass3()
   {
      RealClass3.doSomething();
      assertEquals(2, new RealClass3().performComputation(1, true));
   }

   void assertFakeClass4()
   {
      RealClass4.doSomething();
      assertEquals(2, new RealClass4().performComputation(1, true));
   }

   @Test
   public void fakeInstancePerSetupInAllScopes()
   {
      new FakeClass4();

      assertFakeClass1();
      assertFakeClass2();
      assertFakeClass3();
      assertFakeClass4();
   }

   public static final class FakeURL extends MockUp<URL>
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
   public void reentrantFakeForJREClass() throws Exception
   {
      new FakeURL();

      InputStream response = new URL("http://test").openStream();

      assertEquals("response", new Scanner(response).nextLine());
   }
}