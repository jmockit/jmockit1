/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public class BaseJUnit4DecoratorTest
{
   public static final class RealClass0
   {
      public String getValue() { return "REAL0"; }
   }

   public static final class FakeClass0 extends MockUp<RealClass0>
   {
      @Mock public String getValue() { return "TEST0"; }
   }

   @BeforeClass
   public static void beforeClass()
   {
      assertEquals("REAL0", new RealClass0().getValue());
      new FakeClass0();
      assertEquals("TEST0", new RealClass0().getValue());
   }

   public static final class RealClass1
   {
      public String getValue() { return "REAL1"; }
   }

   public static final class FakeClass1 extends MockUp<RealClass1>
   {
      @Mock public String getValue() { return "TEST1"; }
   }

   @Before
   public final void beforeBase()
   {
      assertEquals("REAL1", new RealClass1().getValue());
      new FakeClass1();
      assertEquals("TEST1", new RealClass1().getValue());
   }

   @After
   public final void afterBase()
   {
      assertEquals("TEST0", new RealClass0().getValue());
      assertEquals("TEST1", new RealClass1().getValue());
   }

   @AfterClass
   public static void afterClass()
   {
      assertEquals("TEST0", new RealClass0().getValue());
      assertEquals("REAL1", new RealClass1().getValue());
   }
}
