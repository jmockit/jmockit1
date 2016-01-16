/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFieldDIByTypeAndNameTest
{
   static class TestedClass
   {
      int someValue;
      int anotherValue;
      final int getSomeValue_base() { return someValue; }
   }

   static class TestedSubclass extends TestedClass
   {
      @SuppressWarnings("FieldNameHidesFieldInSuperclass") int someValue;
      int yetAnotherValue;
   }

   @Tested TestedSubclass tested;

   @Test
   public void injectByFieldTypeAndNameWithTestedClassHavingMultipleFieldsOfSameType(@Injectable("12") int anotherValue)
   {
      assertEquals(0, tested.getSomeValue_base());
      assertEquals(0, tested.someValue);
      assertEquals(12, tested.anotherValue);
      assertEquals(0, tested.yetAnotherValue);
   }

   @Test
   public void injectByFieldTypeAndNameWithTestedClassHavingFieldsOfSameTypeButDifferentNames(@Injectable("45") int val)
   {
      assertEquals(0, tested.getSomeValue_base());
      assertEquals(0, tested.someValue);
      assertEquals(0, tested.anotherValue);
      assertEquals(0, tested.yetAnotherValue);
   }

   @Test
   public void injectByFieldTypeAndNameIntoFieldsAtDifferentLevelsOfClassHierarchy(
      @Injectable("1") int someValue, @Injectable("2") int yetAnotherValue, @Injectable("3") int unused)
   {
      assertEquals(0, tested.getSomeValue_base());
      assertEquals(1, tested.someValue);
      assertEquals(0, tested.anotherValue);
      assertEquals(2, tested.yetAnotherValue);
   }
}
