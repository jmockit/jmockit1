/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithConstructorAndFieldDI2Test
{
   public static final class TestedClass
   {
      private final int i;
      private final String name;
      private final Runnable action1;
      Runnable action2;
      int i2;
      String text;
      String text2;
      String text3;
      List<String> names;

      public TestedClass(int i, String name, Runnable action1)
      {
         this.i = i;
         this.name = name;
         this.action1 = action1;
      }
   }

   static final class TestedClass2 { boolean flag; }

   @Tested final TestedClass tested1 = new TestedClass(123, "test", null);
   @Tested TestedClass tested2;
   @Tested TestedClass2 tested3;

   @Injectable Runnable action;
   @Injectable("-67") int i2; // must match the target field by name
   @Injectable String text = "text";
   @Injectable("8") int intValue2; // won't be used
   @Injectable final int intValue3 = 9; // won't be used
   @Injectable final List<String> names = asList("Abc", "xyz");

   @Before
   public void setUp()
   {
      Runnable action1 = new Runnable() { @Override public void run() {} };
      tested2 = new TestedClass(45, "another", action1);
   }

   @Test
   public void verifyTestedObjectsInjectedFromFieldsInTheTestClass()
   {
      assertFieldsSetByTheConstructor();
      assertFieldsSetThroughFieldInjectionFromInjectableFields();

      // Fields not set either way:
      assertNull(tested1.text2);
      assertNull(tested2.text2);
   }

   void assertFieldsSetByTheConstructor()
   {
      assertEquals(123, tested1.i);
      assertEquals("test", tested1.name);
      assertNull(tested1.action1);

      assertEquals(45, tested2.i);
      assertEquals("another", tested2.name);
      assertNotNull(tested2.action1);
      assertNotSame(action, tested2.action1);
   }

   void assertFieldsSetThroughFieldInjectionFromInjectableFields()
   {
      assertSame(action, tested1.action2);
      assertEquals(-67, tested1.i2);
      assertEquals("text", tested1.text);

      assertSame(action, tested2.action2);
      assertEquals(-67, tested2.i2);
      assertEquals("text", tested2.text);

      assertEquals(asList("Abc", "xyz"), tested1.names);
      assertSame(tested1.names, tested2.names);
   }

   @Test
   public void verifyTestedObjectsInjectedFromInjectableFieldsAndParameters(@Injectable("Test") String text2)
   {
      assertFieldsSetByTheConstructor();

      // Fields set from injectable parameters:
      assertEquals("Test", tested1.text2);
      assertEquals("Test", tested2.text2);

      // Fields not set:
      assertNull(tested1.text3);
      assertNull(tested2.text3);
   }

   @Test
   public void verifyTestedObjectsInjectedFromParametersByName(
      @Injectable("two") String text2, @Injectable("three") String text3, @Injectable("true") boolean flag)
   {
      assertFieldsSetByTheConstructor();

      // Fields set from injectable parameters:
      assertEquals("two", tested1.text2);
      assertEquals("three", tested1.text3);
      assertEquals("two", tested2.text2);
      assertEquals("three", tested2.text3);
      assertTrue(tested3.flag);
   }

   static class ClassWithConstructorHavingReferenceTypeParameterAndDoubleSizedLocalVar
   {
      @SuppressWarnings("unused")
      ClassWithConstructorHavingReferenceTypeParameterAndDoubleSizedLocalVar(String s) { long var = 1; }
   }

   @Tested ClassWithConstructorHavingReferenceTypeParameterAndDoubleSizedLocalVar sut;
}
