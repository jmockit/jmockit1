/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import javax.annotation.*;
import javax.inject.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestedClassWithAnnotatedDITest
{
   static class TestedClass1
   {
      @Resource(name = "secondAction") Runnable action2;
      @Autowired int someValue;
      @Resource(name = "firstAction") Runnable action1;
      @Inject int anotherValue;
      @Value("textValue") String stringFieldWithValue;
      @Value("123.45") double numericFieldWithValue;
      @Value("#{systemProperties.someProperty}") String systemProperty;
   }

   static class TestedClass2
   {
      final int someValue;
      final Runnable action;
      @Resource Runnable anotherAction;
      String text;
      @Inject String anotherText;
      @Autowired(required = false) Runnable optionalAction;

      @Autowired
      TestedClass2(int someValue, Runnable action, String textValue)
      {
         this.someValue = someValue;
         this.action = action;
         text = textValue;
      }
   }

   @Tested TestedClass1 tested1;
   @Tested TestedClass2 tested2;
   @Injectable Runnable firstAction;
   @Injectable final int someValue = 1;
   @Injectable Runnable action;
   @Injectable String textValue = "test";
   @Injectable String anotherText = "name2";

   @Test
   public void injectAllAnnotatedInjectionPoints(
      @Injectable("2") int anotherValue, @Injectable Runnable secondAction, @Injectable Runnable anotherAction,
      @Injectable("true") boolean unused, @Injectable("propertyValue") String systemProperty)
   {
      assertSame(firstAction, tested1.action1);
      assertSame(secondAction, tested1.action2);
      assertEquals(1, tested1.someValue);
      assertEquals(2, tested1.anotherValue);
      assertEquals("textValue", tested1.stringFieldWithValue);
      assertEquals(123.45, tested1.numericFieldWithValue, 0);
      assertEquals("propertyValue", tested1.systemProperty);

      assertEquals(1, tested2.someValue);
      assertSame(action, tested2.action);
      assertSame(anotherAction, tested2.anotherAction);
      assertSame(textValue, tested2.text);
      assertSame(anotherText, tested2.anotherText);
      assertNull(tested2.optionalAction);
   }

   @Test(expected = IllegalStateException.class)
   public void failForAnnotatedFieldWhichLacksAnInjectable()
   {
      fail("Must fail before starting");
   }

   @Test(expected = IllegalStateException.class)
   public void failForAnnotatedFieldHavingAnInjectableOfTheSameTypeWhichWasAlreadyConsumed(
      @Injectable Runnable secondAction)
   {
      fail("Must fail before starting");
   }
}
