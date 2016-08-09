/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runners.*;

import mockit.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class SubclassTest
{
   private static boolean superClassConstructorCalled;
   private static boolean subClassConstructorCalled;

   public static class SuperClass
   {
      final String name;

      public SuperClass(int x, String name)
      {
         this.name = name + x;
         superClassConstructorCalled = true;
      }
   }

   public static class SubClass extends SuperClass
   {
      public SubClass(String name)
      {
         super(name.length(), name);
         subClassConstructorCalled = true;
      }
   }

   @Before
   public void setUp()
   {
      superClassConstructorCalled = false;
      subClassConstructorCalled = false;
   }

   @Test
   public void captureSubclassThroughClassfileTransformer(@Capturing SuperClass captured)
   {
      new SubClass("capture");

      assertFalse(superClassConstructorCalled);
      assertFalse(subClassConstructorCalled);
   }

   @Test
   public void captureSubclassThroughRedefinitionOfPreviouslyLoadedClasses(@Capturing SuperClass captured)
   {
      new SubClass("capture");

      assertFalse(superClassConstructorCalled);
      assertFalse(subClassConstructorCalled);
   }

   @Test
   public void mockSubclassUsingExpectationsWithFirstSuperConstructor(@Mocked SubClass mock)
   {
      new Expectations() {{
         new SubClass("test");
      }};

      new SubClass("test");

      assertFalse(superClassConstructorCalled);
      assertFalse(subClassConstructorCalled);
   }

   @Test
   public void partiallyMockSubclassByMockingASingleConstructor()
   {
      new Expectations(SubClass.class) {{
         new SubClass("test");
      }};

      new SubClass("test");

      assertTrue(superClassConstructorCalled);
      assertFalse(subClassConstructorCalled);
   }
}
