/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.data;

import org.junit.*;

import integrationTests.*;

public final class ClassWithFieldsAccessedByMultipleTestsTest extends CoverageTest
{
   ClassWithFieldsAccessedByMultipleTests tested;

   @Test
   public void onlyAssignStaticField1()
   {
      ClassWithFieldsAccessedByMultipleTests.setStaticField1(false);
   }

   @Test
   public void readAndAssignStaticField1()
   {
      ClassWithFieldsAccessedByMultipleTests.isStaticField1();
      ClassWithFieldsAccessedByMultipleTests.setStaticField1(true);
   }

   @AfterClass
   public static void staticField1ShouldBeUncovered()
   {
      assertStaticFieldUncovered("staticField1");
   }

   @Test
   public void assignAndReadStaticField2()
   {
      ClassWithFieldsAccessedByMultipleTests.setStaticField2(true);
      ClassWithFieldsAccessedByMultipleTests.isStaticField2();
   }

   @Test
   public void assignStaticField2()
   {
      ClassWithFieldsAccessedByMultipleTests.setStaticField2(false);
   }

   @AfterClass
   public static void staticField2ShouldBeCovered()
   {
      assertStaticFieldCovered("staticField2");
   }

   @Test
   public void onlyAssignInstanceField1()
   {
      tested.setInstanceField1(1);
   }

   @Test
   public void readAndAssignInstanceField1()
   {
      tested.getInstanceField1();
      tested.setInstanceField1(2);
   }

   @AfterClass
   public static void instanceField1ShouldBeUncovered()
   {
      assertInstanceFieldUncovered("instanceField1");
   }

   @Test
   public void assignAndReadInstanceField2()
   {
      tested.setInstanceField2(3);
      tested.getInstanceField2();
   }

   @Test
   public void assignInstanceField2()
   {
      tested.setInstanceField2(4);
   }

   @AfterClass
   public static void instanceField2ShouldBeCovered()
   {
      assertInstanceFieldCovered("instanceField2");
   }

   @AfterClass
   public static void verifyDataCoverage()
   {
      verifyDataCoverage(4, 2, 50);
   }
}
