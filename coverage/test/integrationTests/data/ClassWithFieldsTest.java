/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.data;

import org.junit.*;

import integrationTests.*;

public final class ClassWithFieldsTest extends CoverageTest
{
   ClassWithFields tested;

   @Test
   public void setGetStatic1()
   {
      ClassWithFields.setStatic1(1);
      ClassWithFields.setStatic1(2);
      assert ClassWithFields.getStatic1() == 2;

      assertStaticFieldCovered("static1");
   }

   @Test
   public void setStatic2()
   {
      ClassWithFields.setStatic2("test");

      assertStaticFieldUncovered("static2");
   }

   @Test
   public void setGetSetStatic3()
   {
      ClassWithFields.setStatic3(1);
      assert ClassWithFields.getStatic3() == 1;
      ClassWithFields.setStatic3(2);

      assertStaticFieldUncovered("static3");
   }

   @Test
   public void setGetInstance1()
   {
      tested.setInstance1(true);
      assert tested.isInstance1();

      assertInstanceFieldCovered("instance1");
   }

   @Test
   public void setInstance2()
   {
      tested.setInstance2(false);

      assertInstanceFieldUncovered("instance2", tested);
   }

   @Test
   public void setGetSetInstance3()
   {
      tested.setInstance3(2.5);
      assert tested.getInstance3() >= 2.5;
      tested.setInstance3(-0.9);

      assertInstanceFieldUncovered("instance3", tested);
   }

   @AfterClass
   public static void verifyDataCoverage()
   {
      verifyDataCoverage(6, 2, 33);
   }
}
