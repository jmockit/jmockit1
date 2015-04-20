/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runners.*;

import mockit.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class SubclassTest
{
   private static boolean superClassConstructorCalled;
   private static boolean subClassConstructorCalled;
   private static boolean mockConstructorCalled;

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
      mockConstructorCalled = false;
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
   public void mockSubclassUsingMockUpClass()
   {
      new MockUp<SubClass>() {
         @Mock
         void $init(String name)
         {
            assertNotNull(name);
            mockConstructorCalled = true;
         }
      };

      new SubClass("test");

      assertTrue(superClassConstructorCalled);
      assertFalse(subClassConstructorCalled);
      assertTrue(mockConstructorCalled);
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
