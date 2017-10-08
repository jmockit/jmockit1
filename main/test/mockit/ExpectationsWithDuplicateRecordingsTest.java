/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;

import org.junit.*;

public final class ExpectationsWithDuplicateRecordingsTest
{
   @SuppressWarnings({"unused", "NumericCastThatLosesPrecision"})
   static class Blah
   {
      void setValue(int value) {}
      String doSomething(boolean b) { return ""; }
      String doSomething(String s) { return ""; }
      long doSomething(Long l) { return -1L; }
      long doSomething(Long l, Object o) { return 1L; }
      int truncate(double d) { return (int) d; }
      int truncate(float f) { return (int) f; }
      Boolean doSomething(int i) { return true; }
      int doSomething(char c) { return 123; }
   }

   @Mocked Blah mock;

   @Test
   public void recordSameMethodWithDisjunctiveArgumentMatchers()
   {
      new Expectations() {{
         mock.doSomething(withEqual(1)); result = false;
         mock.doSomething(withNotEqual(1)); result = true;
      }};

      assertFalse(mock.doSomething(1));
      assertTrue(mock.doSomething(2));
      assertTrue(mock.doSomething(0));
      assertFalse(mock.doSomething(1));
   }

   @Test
   public void recordAmbiguousExpectationsUsingArgumentMatchers()
   {
      new Expectations() {{
         mock.doSomething(withNotEqual('x')); result = 1;
         mock.doSomething(anyChar); result = 2;
      }};

      assertEquals(1, mock.doSomething('W'));
      assertEquals(2, mock.doSomething('x'));
   }

   @Test
   public void recordSameMethodWithIdenticalArgumentMatchers()
   {
      new Expectations() {{
         mock.doSomething(anyInt); result = false;
         mock.doSomething(anyInt); result = true; // overrides the previous expectation

         mock.doSomething(withNotEqual(5L), withInstanceOf(String.class)); result = 1L;
         mock.doSomething(withNotEqual(5L), withInstanceOf(String.class)); result = 2L; // same here
      }};

      assertTrue(mock.doSomething(1));
      assertTrue(mock.doSomething(0));

      assertEquals(2, mock.doSomething(null, "test 1"));
      assertEquals(2, mock.doSomething(1L, "test 2"));
   }

   @Test
   public void recordSameMethodWithOverlappingArgumentMatchers()
   {
      new Expectations() {{
         mock.doSomething(withEqual(0)); result = false;
         mock.doSomething(anyInt); result = true;

         mock.doSomething((Long) withNull()); result = 1L;
         mock.doSomething((Long) any); result = 2L;
      }};

      assertTrue(mock.doSomething(1));
      assertFalse(mock.doSomething(0));

      assertEquals(1, mock.doSomething((Long) null));
      assertEquals(2, mock.doSomething(1L));
   }

   @Test
   public void recordSameMethodWithExactArgumentAndArgMatcherButInWrongOrderOfSpecificity()
   {
      new Expectations() {{
         mock.doSomething(anyInt); result = false;
         mock.doSomething(1); result = true; // overrides the previous one (most specific should come first)
      }};

      assertTrue(mock.doSomething(1)); // matches last recorded expectation
      assertFalse(mock.doSomething(2)); // matches no expectation
   }

   @Test
   public void recordSameMethodWithArgumentsOrMatchersOfVaryingSpecificity()
   {
      new Expectations() {{
         mock.doSomething(true); result = null;
         mock.doSomething(anyBoolean); result = "a";

         mock.doSomething(1); result = true;
         mock.doSomething(anyInt); result = false;

         mock.doSomething(withEqual('c')); result = 1;
         mock.doSomething(anyChar); result = 2;

         mock.doSomething((String) withNull());
         mock.doSomething(withEqual("str")); result = "b";
         mock.doSomething(anyString); result = "c";
      }};

      assertEquals("a", mock.doSomething(false)); // matches only one expectation
      assertNull(mock.doSomething(true)); // matches two, but most specific was recorded first

      assertTrue(mock.doSomething(1)); // matches two, but most specific came first
      assertFalse(mock.doSomething(2)); // matches only one expectation

      assertEquals(1, mock.doSomething('c')); // matches the first and most specific
      assertEquals(2, mock.doSomething('3')); // matches only one
      assertEquals(2, mock.doSomething('x')); // matches only one

      assertNull(mock.doSomething((String) null)); // matches one specific expectation
      assertEquals("b", mock.doSomething("str")); // matches another specific expectation
      assertEquals("c", mock.doSomething("")); // matches the non-specific expectation
   }

   @Test
   public void recordSameMethodWithOpposingMatchers()
   {
      new Expectations() {{
         mock.doSomething(this.<String>withNull()); result = "null";
         mock.doSomething(this.<String>withNotNull()); result = "non-null";
      }};

      assertEquals("non-null", mock.doSomething("XYZ"));
      assertEquals("null", mock.doSomething((String) null));
   }

   @Test
   public void recordAmbiguousExpectationsUsingTheSameMatcherButWithDifferentArguments()
   {
      new Expectations() {{
         mock.doSomething(withNotEqual('A')); result = 1;
         mock.doSomething(withNotEqual('B')); result = 2; // overrides the previous expectation

         mock.doSomething(withAny(1)); result = false;
         mock.doSomething(withAny(2)); result = true; // overrides the previous expectation
      }};

      assertEquals(2, mock.doSomething('A'));
      assertEquals(0, mock.doSomething('B'));
      assertEquals(2, mock.doSomething(' '));

      assertTrue(mock.doSomething(3));
      assertTrue(mock.doSomething(1));
      assertTrue(mock.doSomething(2));
   }

   @Test @SuppressWarnings("unused")
   public void recordUnambiguousExpectationsUsingTheSameMatcherButWithDifferentArguments()
   {
      new Expectations() {{
         mock.doSomething(withEqual("abc")); result = "first";
         mock.doSomething(withEqual("XYZ")); result = "second";

         mock.truncate(withEqual(5.0, 0.1)); result = 1;
         mock.truncate(withEqual(-5.0, 0.005)); result = 2;

         mock.truncate(withEqual(300.0F, 2)); result = 1;
         mock.truncate(withEqual(123.5F, 1)); result = 2;

         mock.doSomething(withSameInstance('A')); result = 1;
         mock.doSomething(withSameInstance('B')); result = 2;

         mock.doSomething(1L, withInstanceOf(Long.class)); result = 1L;
         mock.doSomething(1L, withInstanceOf(Integer.class)); result = 2L;

         mock.doSomething(2L, withInstanceLike(123L)); result = 1L;
         mock.doSomething(2L, withInstanceLike(123)); result = 2L;

         mock.doSomething(withPrefix("Abc")); result = "Ap";
         mock.doSomething(withPrefix("Xyz")); result = "Bp";

         mock.doSomething(withSuffix("S")); result = "As";
         mock.doSomething(withSuffix("suf")); result = "Bs";

         mock.doSomething(withSubstring("abc")); result = "sub1";
         mock.doSomething(withSubstring("5X")); result = "sub2";

         mock.doSomething(withMatch("[A-F]+")); result = "letter";
         mock.doSomething(withMatch("[0-9]+")); result = "digit";

         mock.doSomething(with(new Delegate<Boolean>() { boolean matches(boolean b) { return b; } })); result = "T";
         mock.doSomething(with(new Delegate<Boolean>() { boolean matches(boolean b) { return !b; } })); result = "F";

         mock.doSomething(with(new Delegate<Integer>() { boolean matches(int i) { return i > 0; } })); result = true;
         mock.doSomething(with(new Delegate<Integer>() { boolean matches(int i) { return i < 0; } })); result = false;

         mock.doSomething(with(new Delegate<Long>() { boolean matches(Long l) { return l == null; } })); result = 1L;
         mock.doSomething(with(new Delegate<Long>() { boolean matches(Long l) { return l != null; } })); result = 2L;
      }};

      assertEquals("second", mock.doSomething("XYZ"));
      assertEquals("first", mock.doSomething("abc"));

      assertEquals(2, mock.truncate(-5.001));
      assertEquals(1, mock.truncate(4.92));

      assertEquals(2, mock.truncate(123.5F));
      assertEquals(1, mock.truncate(301.9F));

      assertEquals(0, mock.doSomething(' '));
      assertEquals(1, mock.doSomething('A'));
      assertEquals(2, mock.doSomething('B'));

      assertEquals(2L, mock.doSomething(1L, 123));
      assertEquals(1L, mock.doSomething(1L, 123L));

      assertEquals(2L, mock.doSomething(2L, 123));
      assertEquals(1L, mock.doSomething(2L, 123L));

      assertEquals("Bp", mock.doSomething("XyzAbc"));
      assertEquals("Ap", mock.doSomething("AbcXyz"));

      assertEquals("Bs", mock.doSomething("asDfsuf"));
      assertEquals("As", mock.doSomething("sfj43S"));

      assertEquals("sub2", mock.doSomething(" 5X  234 TY"));
      assertEquals("sub1", mock.doSomething("AsdabcJ343"));

      assertEquals("digit", mock.doSomething("34502"));
      assertEquals("letter", mock.doSomething("AEFCB"));

      assertEquals("T", mock.doSomething(true));
      assertEquals("F", mock.doSomething(false));

      assertTrue(mock.doSomething(34));
      assertFalse(mock.doSomething(0));
      assertFalse(mock.doSomething(-12));

      assertEquals(1L, mock.doSomething((Long) null));
      assertEquals(2L, mock.doSomething(123L));
   }

   @Test
   public void recordUnambiguousExpectationsWithSameMatcherForOneParameterAndDifferentArgumentsForAnother()
   {
      Blah b = new Blah();

      new Expectations() {{
         mock.doSomething(anyLong, "A"); result = 1L;
         mock.doSomething(anyLong, "B"); result = 2L;
      }};

      assertEquals(1L, b.doSomething(1L, "A"));
      assertEquals(2L, b.doSomething(2L, "B"));
      assertEquals(0L, b.doSomething(1L, "c"));
      assertEquals(1L, b.doSomething(1L, "A"));
      assertEquals(2L, b.doSomething(0L, "B"));
      assertEquals(0L, b.doSomething(0L, null));
   }

   @Test
   public void recordOverlappingExpectationsWithSameMatcherForOneParameterAndDifferentArgumentsForAnother()
   {
      Blah b = new Blah();

      new Expectations() {{
         mock.doSomething(anyLong, "A"); result = 1L;
         mock.doSomething(anyLong, null); result = 2L;
      }};

      assertEquals(1L, b.doSomething(1L, "A"));
      assertEquals(2L, b.doSomething(2L, "B"));
      assertEquals(2L, b.doSomething(1L, "c"));
      assertEquals(1L, b.doSomething(2L, "A"));
      assertEquals(2L, b.doSomething(0L, "B"));
      assertEquals(2L, b.doSomething(0L, null));
   }

   @Test
   public void recordMultipleNonStrictExpectationsWithSameDelegate()
   {
      final Delegate<String> delegate = new Delegate<String>() {
         @SuppressWarnings("unused")
         boolean matches(String arg) { return !arg.isEmpty(); }
      };

      new Expectations() {{
         mock.doSomething(with(delegate)); result = "first";
      }};

      assertEquals("first", mock.doSomething("test1"));

      new Expectations() {{
         mock.doSomething(with(delegate)); result = "second";
      }};

      assertEquals("second", mock.doSomething("test2"));
   }
}
