/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassInjectedFromMockParametersTest
{
   enum AnEnum { Abc, Xyz }
   public static final class TestedClass
   {
      private int i;
      private String s;
      private boolean b;
      private char[] chars;
      AnEnum enumValue;

      public TestedClass(boolean b) { this.b = b; }

      public TestedClass(int i, String s, boolean b, char... chars)
      {
         this.i = i;
         this.s = s;
         this.b = b;
         this.chars = chars;
      }

      public TestedClass(boolean b1, byte b2, boolean b3)
      {
         b = b1;
         chars = new char[] {(char) b2, b3 ? 'X' : 'x'};
      }

      public TestedClass(char first, char second, char third)
      {
         chars = new char[] {first, second, third};
      }
   }

   @Tested TestedClass tested;

   @Test(expected = IllegalArgumentException.class)
   public void attemptToInstantiateTestedClassWithNoInjectables()
   {
   }

   @Test
   public void instantiateTestedObjectFromMockParametersUsingOneConstructor(
      @Injectable("Text") String s, @Injectable("123") int mock1, @Injectable("true") boolean mock2,
      @Injectable("A") char c1, @Injectable("bB") char c2)
   {
      assertEquals("Text", s);
      assertEquals(s, tested.s);
      assertEquals(mock1, tested.i);
      assertEquals(mock2, tested.b);
      assertEquals(2, tested.chars.length);
      assertEquals(c1, tested.chars[0]);
      assertEquals(c2, tested.chars[1]);
      assertEquals('b', c2);
   }

   @Test
   public void instantiateTestedObjectFromMockParametersUsingAnotherConstructor(
      @Injectable("true") boolean b1, @Injectable("true") boolean b3, @Injectable("65") byte b2)
   {
      assertTrue(tested.b);
      assertEquals('A', tested.chars[0]);
      assertEquals('X', tested.chars[1]);
   }

   @Test
   public void instantiateTestedObjectUsingConstructorWithMultipleParametersOfTheSameTypeMatchedByName(
      @Injectable("S") char second, @Injectable("T") char third, @Injectable("F") char first)
   {
      assertArrayEquals(new char[] {'F', 'S', 'T'}, tested.chars);
   }

   @Test
   public void setEnumFieldInTestedObjectFromValueProvidedInParameter(
      @Injectable("false") boolean flag, @Injectable("Xyz") AnEnum enumVal)
   {
      assertSame(AnEnum.Xyz, tested.enumValue);
   }
}
