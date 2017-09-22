/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class ThirdJUnit4DecoratorTest extends BaseJUnit4DecoratorTest
{
   public static final class RealClass4
   {
      public String getValue() { return "REAL4"; }
   }

   public static final class FakeClass4 extends MockUp<RealClass4>
   {
      @Mock public String getValue() { return "TEST4"; }
   }

   @BeforeClass
   public static void applyFakes()
   {
      new FakeClass4();
   }

   @Test
   public void realClassesFakedInBaseClassMustStillBeFakedHere()
   {
      assertEquals("TEST0", new RealClass0().getValue());
      assertEquals("TEST1", new RealClass1().getValue());
   }

   @Test
   public void realClassesFakedInOtherTestClassesMustNotBeFakedHere()
   {
      assertEquals("REAL2", new JUnit4DecoratorTest.RealClass2().getValue());
      assertEquals("REAL3", new SecondJUnit4DecoratorTest.RealClass3().getValue());
   }

   @Test
   public void useClassScopedFakeDefinedForThisClass()
   {
      assertEquals("TEST4", new RealClass4().getValue());
   }
}