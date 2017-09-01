/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class ExpectationsUsingResultFieldTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      static String doInternal() { return "123"; }

      void provideSomeService() {}

      int getValue() { return -1; }
      Integer getInteger() { return -1; }
      byte getByteValue() { return -1; }
      Byte getByteWrapper() { return -1; }
      short getShortValue() { return -1; }
      Short getShortWrapper() { return -1; }
      long getLongValue() { return -1; }
      Long getLongWrapper() { return -1L; }
      float getFloatValue() { return -1.0F; }
      Float getFloatWrapper() { return -1.0F; }
      double getDoubleValue() { return -1.0; }
      Double getDoubleWrapper() { return -1.0; }
      char getCharValue() { return '1'; }
      Character getCharacter() { return '1'; }
      boolean getBooleanValue() { return true; }
      Boolean getBooleanWrapper() { return true; }
      String getString() { return ""; }
      Object getObject() { return null; }

      Collection<?> getItems() { return null; }
      List<?> getListItems() { return null; }
      Set<?> getSetItems() { return null; }
      SortedSet<?> getSortedSetItems() { return null; }
      Map<?, ?> getMapItems() { return null; }
      SortedMap<?, ?> getSortedMapItems() { return null; }
      Iterator<?> getIterator() { return null; }
      ListIterator<?> getListIterator() { return null; }
      Iterable<?> getIterable() { return null; }

      int[] getIntArray() { return null; }
      int[][] getInt2Array() { return null; }
      byte[] getByteArray() { return null; }
      short[] getShortArray() { return null; }
      Short[] getShortWrapperArray() { return null; }
      long[] getLongArray() { return null; }
      long[][] getLong2Array() { return null; }
      float[] getFloatArray() { return null; }
      double[] getDoubleArray() { return null; }
      char[] getCharArray() { return null; }
      boolean[] getBooleanArray() { return null; }
      String[] getStringArray() { return null; }
      String[][] getString2Array() { return null; }
      <T extends Number> T[] getArrayOfGenericElements(@SuppressWarnings("unused") int i) { return null; }

      Collection<Number> getNumbers() { return null; }
      List<Number> getNumberList() { return null; }
      Set<String> getStringSet() { return null; }
      SortedSet<Number> getSortedNumberSet() { return null; }
      Iterator<String> getStringIterator() { return null; }
      ListIterator<Float> getFloatIterator() { return null; }
      Iterable<Number> getNumberIterable() { return null; }
      Queue<Number> getNumberQueue() { return null; }
   }

   @Test
   public void returnsExpectedValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); result = 3;
         Collaborator.doInternal(); result = "test";
      }};

      assertEquals(3, mock.getValue());
      assertEquals("test", Collaborator.doInternal());
   }

   @Test
   public void recordThrownException(@Mocked final Collaborator mock)
   {
      thrown.expect(ArithmeticException.class);

      new Expectations() {{
         mock.provideSomeService(); result = new ArithmeticException("test");
      }};

      mock.provideSomeService();
   }

   @Test
   public void recordThrownError(@Mocked final Collaborator mock)
   {
      thrown.expect(LinkageError.class);

      new Expectations() {{
         mock.provideSomeService(); result = new LinkageError("test");
      }};

      mock.provideSomeService();
   }

   @Test
   public void returnsMultipleExpectedValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); result = 1; result = 2; result = 3;
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
      assertEquals(3, mock.getValue());
   }

   @Test
   public void returnsMultipleExpectedValuesWithMoreInvocationsAllowed(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); result = 1; result = 2; times = 3;
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
      assertEquals(2, mock.getValue());
   }

   @Test
   public void returnsNullAsDefaultValueForMethodsReturningStringOrObject(@Mocked Collaborator mock)
   {
      assertNull(mock.getString());
      assertNull(Collaborator.doInternal());
      assertNull(mock.getObject());
   }

   @Test
   public void returnsDefaultValuesForPrimitiveAndWrapperReturnTypes(@Mocked Collaborator mock)
   {
      assertEquals(0, mock.getValue());
      assertEquals(0, mock.getInteger().intValue());
      assertEquals((byte) 0, mock.getByteValue());
      assertEquals(0, mock.getByteWrapper().intValue());
      assertEquals((short) 0, mock.getShortValue());
      assertEquals(0, mock.getShortWrapper().intValue());
      assertEquals(0L, mock.getLongValue());
      assertEquals(0L, mock.getLongWrapper().longValue());
      assertEquals(0.0F, mock.getFloatValue(), 0.0);
      assertEquals(0.0F, mock.getFloatWrapper(), 0);
      assertEquals(0.0, mock.getDoubleValue(), 0.0);
      assertEquals(0.0, mock.getDoubleWrapper(), 0);
      assertEquals('\0', mock.getCharValue());
      assertEquals('\0', mock.getCharacter().charValue());
      assertFalse(mock.getBooleanValue());
      assertFalse(mock.getBooleanWrapper());
   }

   @Test
   public void returnsDefaultValuesForCollectionValuedReturnTypes(@Mocked Collaborator mock)
   {
      assertSame(Collections.emptyList(), mock.getItems());
      assertSame(Collections.emptyList(), mock.getListItems());
      assertSame(Collections.emptySet(), mock.getSetItems());
      assertEquals(Collections.emptySet(), mock.getSortedSetItems());
      assertSame(Collections.emptyMap(), mock.getMapItems());
      assertEquals(Collections.emptyMap(), mock.getSortedMapItems());
   }

   @Test
   public void returnsDefaultValuesForIteratorAndIterableReturnTypes(@Mocked Collaborator mock)
   {
      assertFalse(mock.getIterator().hasNext());
      assertFalse(mock.getListIterator().hasNext());
      assertFalse(mock.getIterable().iterator().hasNext());
   }

   @Test
   public void returnsDefaultValuesForArrayValuedReturnTypes(@Mocked Collaborator mock)
   {
      assertArrayEquals(new int[0], mock.getIntArray());
      assertArrayEquals(new int[0][0], mock.getInt2Array());
      assertArrayEquals(new byte[0], mock.getByteArray());
      assertArrayEquals(new short[0], mock.getShortArray());
      assertArrayEquals(new Short[0], mock.getShortWrapperArray());
      assertArrayEquals(new long[0], mock.getLongArray());
      assertArrayEquals(new long[0][0], mock.getLong2Array());
      assertArrayEquals(new float[0], mock.getFloatArray(), 0.0F);
      assertArrayEquals(new double[0], mock.getDoubleArray(), 0.0);
      assertArrayEquals(new char[0], mock.getCharArray());
      assertEquals(0, mock.getBooleanArray().length);
      assertArrayEquals(new String[0], mock.getStringArray());
      assertArrayEquals(new String[0][0], mock.getString2Array());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingCollection()
   {
      final Collaborator collaborator = new Collaborator();
      final Set<Boolean> booleanSet = new LinkedHashSet<Boolean>(asList(true, false));
      final Collection<Integer> intCol = asList(1, 2, 3);
      final List<Character> charList = asList('a', 'b', 'c');

      new Expectations(collaborator) {{
         collaborator.getBooleanWrapper(); result = booleanSet;
         collaborator.getInteger(); result = intCol;
         collaborator.getCharValue(); result = charList;
      }};

      assertTrue(collaborator.getBooleanWrapper());
      assertFalse(collaborator.getBooleanWrapper());

      assertEquals(1, collaborator.getInteger().intValue());
      assertEquals(2, collaborator.getInteger().intValue());
      assertEquals(3, collaborator.getInteger().intValue());

      assertEquals('a', collaborator.getCharValue());
      assertEquals('b', collaborator.getCharValue());
      assertEquals('c', collaborator.getCharValue());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingIterator()
   {
      final Collaborator collaborator = new Collaborator();
      final Collection<String> strCol = asList("ab", "cde", "Xyz");

      new Expectations(collaborator) {{
         collaborator.getString(); result = strCol.iterator();
      }};

      assertEquals("ab", collaborator.getString());
      assertEquals("cde", collaborator.getString());
      assertEquals("Xyz", collaborator.getString());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingArray(@Injectable final Collaborator collaborator)
   {
      final boolean[] arrayOfBooleanPrimitives = {true, false};
      final Boolean[] arrayOfBooleanWrappers = {Boolean.TRUE, Boolean.FALSE};
      final int[] intArray = {1, 2, 3};
      final Character[] charArray = {'a', 'b', 'c'};

      new Expectations() {{
         collaborator.getBooleanValue(); result = arrayOfBooleanPrimitives;
         collaborator.getBooleanWrapper(); result = arrayOfBooleanWrappers;
         collaborator.getValue(); result = intArray;
         collaborator.getCharValue(); result = charArray;
      }};

      assertTrue(collaborator.getBooleanValue());
      assertFalse(collaborator.getBooleanValue());

      assertTrue(collaborator.getBooleanWrapper());
      assertFalse(collaborator.getBooleanWrapper());

      assertEquals(1, collaborator.getValue());
      assertEquals(2, collaborator.getValue());
      assertEquals(3, collaborator.getValue());

      assertEquals('a', collaborator.getCharValue());
      assertEquals('b', collaborator.getCharValue());
      assertEquals('c', collaborator.getCharValue());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingIterable(@Injectable final Collaborator collaborator)
   {
      final Iterable<Integer> intValues = new Iterable<Integer>() {
         @Override public Iterator<Integer> iterator() { return asList(3, 2, 1).iterator(); }
      };

      new Expectations() {{
         collaborator.getValue(); result = intValues;
      }};

      assertEquals(3, collaborator.getValue());
      assertEquals(2, collaborator.getValue());
      assertEquals(1, collaborator.getValue());
   }

   @Test
   public void returnsMultipleValuesFromMethodWithReturnTypeOfObject(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.getObject();
         result = new int[] {1, 2};
         result = new Object[] {"test", 'X'};
         result = asList(5L, 67L);
         result = null;
      }};

      assertArrayEquals(new int[] {1, 2}, (int[]) collaborator.getObject());
      assertArrayEquals(new Object[] {"test", 'X'}, (Object[]) collaborator.getObject());
      //noinspection AssertEqualsBetweenInconvertibleTypes
      assertEquals(asList(5L, 67L), collaborator.getObject());
      assertNull(collaborator.getObject());
      assertNull(collaborator.getObject());
   }

   @Test
   public void returnsEmptyArrayForMethodWithReturnTypeOfObject(@Mocked final Collaborator mock)
   {
      final String[] emptyArray = {};

      new Expectations() {{
         mock.getObject();
         result = emptyArray;
      }};

      assertSame(emptyArray, mock.getObject());
   }

   @Test
   public void returnsMultipleValuesFromGenericMethod(@Mocked final Callable<Integer> callable) throws Exception
   {
      new Expectations() {{
         callable.call();
         result = new int[] {3, 2, 1};
      }};

      Integer firstCall = callable.call();
      assertEquals(3, firstCall.intValue());
      assertEquals(2, callable.call().intValue());
      assertEquals(1, callable.call().intValue());
   }

   @Test
   public void returnsSpecifiedCollectionsForMethodsThatReturnCollections()
   {
      final Collaborator collaborator = new Collaborator();
      final Collection<String> strCol = asList("ab", "cde");
      final List<Byte> byteList = asList((byte) 5, (byte) 68);
      final Set<Character> charSet = new HashSet<Character>(asList('g', 't', 'x'));
      final SortedSet<String> sortedSet = new TreeSet<String>(asList("hpq", "Abc"));

      new Expectations(collaborator) {{
         collaborator.getItems(); result = strCol;
         collaborator.getListItems(); result = byteList;
         collaborator.getSetItems(); result = charSet;
         collaborator.getSortedSetItems(); result = sortedSet;
      }};

      assertSame(strCol, collaborator.getItems());
      assertSame(byteList, collaborator.getListItems());
      assertSame(charSet, collaborator.getSetItems());
      assertSame(sortedSet, collaborator.getSortedSetItems());
   }

   @Test
   public void returnsSpecifiedIteratorForMethodThatReturnsIterator()
   {
      final Collaborator collaborator = new Collaborator();
      final Iterator<String> itr = asList("ab", "cde").iterator();

      new Expectations(collaborator) {{
         collaborator.getIterator(); result = itr;
      }};

      assertSame(itr, collaborator.getIterator());
   }

   @Test
   public void returnsValueOfIncompatibleTypeForMethodReturningArray(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getBooleanArray(); result = new HashSet();
         mock.getStringArray(); result = Collections.emptyList();
         mock.getIntArray(); result = new short[] {1, 2};
      }};

      try { mock.getBooleanArray(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getStringArray(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getIntArray(); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void returnsValueOfIncompatibleTypeForMethodReturningCollection(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getListItems(); result = Collections.emptySet();
         mock.getSetItems(); result = new ArrayList();
      }};

      try { mock.getListItems(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getSetItems(); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void attemptToReturnValueOfTypeSetFromMethodReturningIterator(@Mocked final Collaborator mock)
   {
      thrown.expect(ClassCastException.class);

      new Expectations() {{
         mock.getIterator(); result = Collections.emptySet();
      }};

      mock.getIterator();
   }

   @Test
   public void attemptToReturnValueOfTypeListFromMethodReturningIterator(@Mocked final Collaborator mock)
   {
      thrown.expect(ClassCastException.class);

      new Expectations() {{
         mock.getIterator(); result = asList("a", true, 123);
      }};

      mock.getIterator();
   }

   @Test
   public void returnIterableOrIteratorFromRecordedArray(@Injectable final Collaborator mock)
   {
      final String[] items = {"Abc", "test"};
      final int[] listItems = {1, 2, 3};
      final boolean[] iterable = {false, true};
      final Boolean[] iterator = {true, false, true};
      final Object[] listIterator = {"test", 123, true};

      new Expectations() {{
         mock.getItems(); result = items;
         mock.getListItems(); result = listItems;
         mock.getSetItems(); result = new char[] {'A', 'c', 'b', 'A'};
         mock.getSortedSetItems(); result = new Object[] {"test", "123", "abc"};
         mock.getIterable(); result = iterable;
         mock.getIterator(); result = iterator;
         mock.getListIterator(); result = listIterator;
      }};

      assertEquals(Arrays.toString(items), mock.getItems().toString());
      assertEquals(Arrays.toString(listItems), mock.getListItems().toString());
      assertEquals("[A, c, b]", mock.getSetItems().toString());
      assertEquals("[123, abc, test]", mock.getSortedSetItems().toString());
      assertEquals(Arrays.toString(iterable), mock.getIterable().toString());
      assertEquals(asList(iterator), fromIterator(mock.getIterator()));
      assertEquals(asList(listIterator), fromIterator(mock.getListIterator()));
   }

   private List<?> fromIterator(Iterator<?> itr)
   {
      List<Object> values = new ArrayList<Object>();

      while (itr.hasNext()) {
         values.add(itr.next());
      }

      return values;
   }

   @Test
   public void returnMapFromRecordedTwoDimensionalArray(
      @Injectable final Collaborator mock1, @Injectable final Collaborator mock2)
   {
      final int[][] sortedItems1 = {{13, 1}, {2, 2}, {31, 3}, {5, 4}};
      final Object[][] items2 = {{1, "first"}, {2}, {3, true}};

      new Expectations() {{
         mock1.getMapItems(); result = new String[][] {{"Abc", "first"}, {"test", "Second"}, {"Xyz", null}};
         mock1.getSortedMapItems(); result = sortedItems1;
         mock2.getMapItems(); result = items2;
      }};

      assertEquals("{Abc=first, test=Second, Xyz=null}", mock1.getMapItems().toString());
      assertEquals("{2=2, 5=4, 13=1, 31=3}", mock1.getSortedMapItems().toString());
      assertEquals("{1=first, 2=null, 3=true}", mock2.getMapItems().toString());
   }

   @Test
   public void recordNullReturnValueForConstructor(@Mocked Collaborator mock)
   {
      new Expectations() {{ new Collaborator(); result = null; }};

      new Collaborator().provideSomeService();
   }

   @Test
   public void recordNullReturnValueForVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); result = null; }};

      new Collaborator().provideSomeService();
   }

   @Test
   public void recordNullReturnValueForVoidMethodAndThenAThrownError(@Mocked final Collaborator mock)
   {
      thrown.expect(UnknownError.class);

      new Expectations() {{
         mock.provideSomeService();
         result = null;
         result = new UnknownError();
      }};

      try {
         mock.provideSomeService();
      }
      catch (Throwable ignore) {
         fail();
      }

      mock.provideSomeService();
   }

   @Test
   public void throwExceptionFromSecondInvocationOfConstructor(@Mocked Collaborator mock)
   {
      thrown.expect(NoSuchElementException.class);

      new Expectations() {{
         new Collaborator();
         result = null; result = new NoSuchElementException();
      }};

      try {
         new Collaborator();
      }
      catch (NoSuchElementException ignore) {
         fail();
      }

      new Collaborator();
   }

   @Test
   public void recordReturnValueForVoidMethod(@Mocked final Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("incompatible with return type void");
      thrown.expectMessage("Integer");

      new Expectations() {{
         mock.provideSomeService();
         result = 123;
      }};
   }

   @Test
   public void recordConsecutiveReturnValuesForVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService();
         result = new int[] {123, 45}; // will have the effect of allowing two invocations
      }};

      mock.provideSomeService();
      mock.provideSomeService();
   }

   @Test
   public void recordReturnValueForConstructor(@Mocked Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("String");
      thrown.expectMessage("incompatible with return type void");

      new Expectations() {{
         new Collaborator();
         result = "test"; // invalid, throws IllegalArgumentException
      }};
   }

   @Test
   public void recordReturnValuesMixedWithThrowablesForNonVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getString();
         result = asList("Abc", new IllegalStateException(), "DEF", null, new UnknownError());
      }};

      Collaborator c = new Collaborator();
      assertEquals("Abc", c.getString());
      try { c.getString(); fail(); } catch (IllegalStateException ignored) {}
      assertEquals("DEF", c.getString());
      assertNull(c.getString());
      try { c.getString(); fail(); } catch (UnknownError ignored) {}
      try { c.getString(); fail(); } catch (UnknownError ignored) {}
   }

   @Test
   public void recordExceptionFollowedByNullReturnValueForVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService();
         result = new IllegalArgumentException();
         result = null;
      }};

      try { mock.provideSomeService(); fail(); } catch (IllegalArgumentException ignored) {}
      mock.provideSomeService();
   }

   @Test
   public void recordArraysOfGenericElementTypes(@Mocked final Collaborator mock)
   {
      final Integer[] integerValues = {1, 2};
      final Number[] numberValues = {5L, 12.5F};
      final String[] stringValues = {"a", "b"};

      new Expectations() {{
         mock.getArrayOfGenericElements(1); result = integerValues;
         mock.getArrayOfGenericElements(2); result = numberValues;
         mock.getArrayOfGenericElements(3); result = stringValues;
      }};

      assertSame(numberValues, mock.getArrayOfGenericElements(2));
      assertSame(integerValues, mock.getArrayOfGenericElements(1));
      try { mock.getArrayOfGenericElements(3); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void createArrayFromSingleRecordedValueOfTheElementType(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getIntArray(); result = 123;
         mock.getStringArray(); result = "test";
      }};

      assertArrayEquals(new int[] {123}, mock.getIntArray());
      assertArrayEquals(new String[] {"test"}, mock.getStringArray());
   }

   @Test
   public void createAppropriateContainerFromSingleRecordedValueOfTheElementType(@Mocked final Collaborator mock)
   {
      final Double d = 1.2;
      final Float f = 3.45F;
      final BigDecimal price = new BigDecimal("123.45");

      new Expectations() {{
         mock.getNumbers(); result = 123;
         mock.getNumberList(); result = 45L;
         mock.getStringSet(); result = "test";
         mock.getSortedNumberSet(); result = d;
         mock.getNumberIterable(); result = price;
         mock.getNumberQueue(); result = d;
         mock.getStringIterator(); result = "Abc";
         mock.getFloatIterator(); result = f;
      }};

      assertContainerWithSingleElement(mock.getNumbers(), 123);
      assertContainerWithSingleElement(mock.getNumberList(), 45L);
      assertContainerWithSingleElement(mock.getStringSet(), "test");
      assertContainerWithSingleElement(mock.getSortedNumberSet(), d);
      assertContainerWithSingleElement(mock.getNumberIterable(), price);
      assertContainerWithSingleElement(mock.getNumberQueue(), d);
      assertContainerWithSingleElement(mock.getStringIterator(), "Abc");
      assertContainerWithSingleElement(mock.getFloatIterator(), f);
   }

   void assertContainerWithSingleElement(Iterable<?> container, Object expectedElement)
   {
      assertContainerWithSingleElement(container.iterator(), expectedElement);
   }

   void assertContainerWithSingleElement(Iterator<?> container, Object expectedElement)
   {
      assertTrue(container.hasNext());
      assertSame(expectedElement, container.next());
      assertFalse(container.hasNext());
   }
}