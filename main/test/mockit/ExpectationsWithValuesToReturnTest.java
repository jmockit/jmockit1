/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.junit.rules.*;

import static java.util.Arrays.*;

import static org.junit.Assert.*;

import mockit.internal.*;

public final class ExpectationsWithValuesToReturnTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      private static String doInternal() { return "123"; }

      void provideSomeService() { throw new RuntimeException("Should not occur"); }

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
      @SuppressWarnings("CollectionDeclaredAsConcreteClass")
      List<?> getAList() { return null; }
      Set<?> getSetItems() { return null; }
      SortedSet<?> getSortedSetItems() { return null; }
      Map<?, ?> getMapItems() { return null; }
      SortedMap<?, ?> getSortedMapItems() { return null; }
      ListIterator<?> getListIterator() { return null; }
      Iterator<?> getIterator() { return null; }
      Iterable<?> getIterable() { return null; }

      Collection<Number> getNumbers() { return null; }
      List<Number> getNumberList() { return null; }
      Set<String> getStringSet() { return null; }
      SortedSet<Number> getSortedNumberSet() { return null; }
      Iterator<String> getStringIterator() { return null; }
      ListIterator<Float> getFloatIterator() { return null; }
      Iterable<Number> getNumberIterable() { return null; }
      Queue<Number> getNumberQueue() { return null; }

      int[] getIntArray() { return null; }
      int[][] getInt2Array() { return null; }
      byte[] getByteArray() { return null; }
      Byte[] getByteWrapperArray() { return null; }
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
   }

   @Test
   public void returnsExpectedValuesWithStrictExpectations(@Mocked final Collaborator mock)
   {
      new StrictExpectations() {{
         mock.getValue(); returns(3);
         Collaborator.doInternal(); returns("test");
      }};

      assertEquals(3, mock.getValue());
      assertEquals("test", Collaborator.doInternal());
   }

   @Test
   public void returnsExpectedValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); returns(3);
         Collaborator.doInternal(); returns("test");
      }};

      assertEquals(3, mock.getValue());
      assertEquals("test", Collaborator.doInternal());
   }

   @Test
   public void recordsMultipleExpectedValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); returns(1); returns(2); returns(3);
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
      assertEquals(3, mock.getValue());
   }

   @Test
   public void returnsMultipleExpectedValuesWithMoreInvocationsAllowed(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); returns(1); returns(2); times = 3;
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
      assertEquals(2, mock.getValue());
   }

   @Test
   public void returnsDefaultValuesForPrimitiveAndWrapperReturnTypes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         mock.getInteger();
         mock.getByteValue();
         mock.getByteWrapper();
         mock.getShortValue();
         mock.getShortWrapper();
         mock.getLongValue();
         mock.getLongWrapper();
         mock.getFloatValue();
         mock.getFloatWrapper();
         mock.getDoubleValue();
         mock.getDoubleWrapper();
         mock.getCharValue();
         mock.getCharacter();
         mock.getBooleanValue();
         mock.getBooleanWrapper();
         Collaborator.doInternal();
      }};

      assertEquals(0, mock.getValue());
      assertEquals(0, mock.getInteger().intValue());
      assertEquals((byte) 0, mock.getByteValue());
      assertEquals((byte) 0, mock.getByteWrapper().byteValue());
      assertEquals((short) 0, mock.getShortValue());
      assertEquals((short) 0, mock.getShortWrapper().shortValue());
      assertEquals(0L, mock.getLongValue());
      assertEquals(0L, mock.getLongWrapper().longValue());
      assertEquals(0.0F, mock.getFloatValue(), 0.0);
      assertEquals(0, mock.getFloatWrapper(), 0);
      assertEquals(0.0, mock.getDoubleValue(), 0.0);
      assertEquals(0, mock.getDoubleWrapper(), 0);
      assertEquals('\0', mock.getCharValue());
      assertEquals('\0', mock.getCharacter().charValue());
      assertFalse(mock.getBooleanValue());
      assertFalse(mock.getBooleanWrapper());
      assertNull(Collaborator.doInternal());
   }

   @Test
   public void returnsDefaultValuesForCollectionValuedReturnTypes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getItems();
         mock.getListItems();
         mock.getSetItems();
         mock.getSortedSetItems();
         mock.getMapItems();
         mock.getSortedMapItems();
      }};

      assertSame(Collections.emptyList(), mock.getItems());
      assertSame(Collections.emptyList(), mock.getListItems());
      assertSame(Collections.emptySet(), mock.getSetItems());
      assertEquals(Collections.emptySet(), mock.getSortedSetItems());
      assertSame(Collections.emptyMap(), mock.getMapItems());
      assertEquals(Collections.emptyMap(), mock.getSortedMapItems());
   }

   @Test
   public void returnsDefaultValuesForArrayValuedReturnTypes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getIntArray();
         mock.getInt2Array();
         mock.getByteArray();
         mock.getShortArray();
         mock.getShortWrapperArray();
         mock.getLongArray();
         mock.getLong2Array();
         mock.getFloatArray();
         mock.getDoubleArray();
         mock.getCharArray();
         mock.getBooleanArray();
         mock.getStringArray();
         mock.getString2Array();
      }};

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

   @SuppressWarnings("PrimitiveArrayArgumentToVariableArgMethod")
   @Test
   public void returnsMultipleValuesInSequenceUsingVarargs()
   {
      final Collaborator collaborator = new Collaborator();
      final char[] charArray = {'a', 'b', 'c'};

      new StrictExpectations(collaborator) {{
         collaborator.getBooleanValue(); returns(true, false);
         collaborator.getShortValue(); returns((short) 1, (short) 2, (short) 3);
         collaborator.getShortWrapper(); returns((short) 5, (short) 6, (short) -7, (short) -8);
         collaborator.getCharArray(); returns(charArray);
         collaborator.getCharArray(); returns(new char[0], new char[] {'x'});
      }};

      assertTrue(collaborator.getBooleanValue());
      assertFalse(collaborator.getBooleanValue());

      assertEquals(1, collaborator.getShortValue());
      assertEquals(2, collaborator.getShortValue());
      assertEquals(3, collaborator.getShortValue());

      assertEquals(5, collaborator.getShortWrapper().shortValue());
      assertEquals(6, collaborator.getShortWrapper().shortValue());
      assertEquals(-7, collaborator.getShortWrapper().shortValue());
      assertEquals(-8, collaborator.getShortWrapper().shortValue());

      assertArrayEquals(charArray, collaborator.getCharArray());
      assertArrayEquals(new char[0], collaborator.getCharArray());
      assertArrayEquals(new char[]{'x'}, collaborator.getCharArray());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingArray()
   {
      final Collaborator collaborator = new Collaborator();
      final boolean[] booleanArray = {true, false};
      final int[] intArray = {1, 2, 3};
      final Character[] charArray = {'a', 'b', 'c'};

      new Expectations(collaborator) {{
         collaborator.getBooleanValue(); returns(booleanArray);
         collaborator.getBooleanWrapper(); returns(booleanArray);
         collaborator.getInteger(); returns(intArray);
         collaborator.getValue(); returns(intArray);
         collaborator.getCharValue(); returns(charArray);
      }};

      assertTrue(collaborator.getBooleanValue());
      assertFalse(collaborator.getBooleanValue());

      assertTrue(collaborator.getBooleanWrapper());
      assertFalse(collaborator.getBooleanWrapper());

      assertEquals(1, collaborator.getInteger().intValue());
      assertEquals(2, collaborator.getInteger().intValue());
      assertEquals(3, collaborator.getInteger().intValue());

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
         collaborator.getValue(); returns(intValues);
      }};

      assertEquals(3, collaborator.getValue());
      assertEquals(2, collaborator.getValue());
      assertEquals(1, collaborator.getValue());
   }

   @Test
   public void returnsMultipleValuesInSequenceUsingCollection()
   {
      final Collaborator collaborator = new Collaborator();
      final Set<Boolean> booleanSet = new LinkedHashSet<Boolean>(asList(true, false));
      final Collection<Integer> intCol = asList(1, 2, 3);
      final List<Character> charList = asList('a', 'b', 'c');

      new Expectations(collaborator) {{
         collaborator.getBooleanWrapper(); returns(booleanSet);
         collaborator.getInteger(); returns(intCol);
         collaborator.getCharValue(); returns(charList);
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
   public void recordMultipleResultValuesInSequenceUsingIterator()
   {
      final Collaborator collaborator = new Collaborator();
      final Collection<String> strCol = asList("ab", "cde", "Xyz");

      new Expectations(collaborator) {{
         collaborator.getString(); returns(strCol.iterator());
      }};

      assertEquals("ab", collaborator.getString());
      assertEquals("cde", collaborator.getString());
      assertEquals("Xyz", collaborator.getString());
   }

   @Test
   public void returnsMultipleValuesFromMethodWithReturnTypeOfObject(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.getObject();
         returns(1, 2);
         returns(new int[] {1, 2});
         returns("test", 'X');
         returns(asList(5L, 67L));
      }};

      assertEquals(1, collaborator.getObject());
      assertEquals(2, collaborator.getObject());
      assertArrayEquals(new int[] {1, 2}, (int[]) collaborator.getObject());
      assertEquals("test", collaborator.getObject());
      assertEquals('X', collaborator.getObject());
      assertEquals(asList(5L, 67L), collaborator.getObject());
   }

   @Test
   public void returnsMultipleValuesFromGenericMethod(@Mocked final Callable<Integer> callable) throws Exception
   {
      new Expectations() {{
         callable.call();
         returns(3, 2, 1);
      }};

      assertEquals(3, callable.call().intValue());
      assertEquals(2, callable.call().intValue());
      assertEquals(1, callable.call().intValue());
   }

   @Test
   public void recordResultsForMethodsThatReturnCollections()
   {
      final Collaborator collaborator = new Collaborator();
      final Collection<String> strCol = asList("ab", "cde");
      final List<Byte> byteList = asList((byte) 5, (byte) 68);
      final Set<Character> charSet = new HashSet<Character>(asList('g', 't', 'x'));
      final SortedSet<String> sortedSet = new TreeSet<String>(asList("hpq", "Abc"));

      new Expectations(collaborator) {{
         collaborator.getItems(); returns(strCol);
         collaborator.getListItems(); returns(byteList);
         collaborator.getSetItems(); returns(charSet);
         collaborator.getSortedSetItems(); returns(sortedSet);
      }};

      assertSame(strCol, collaborator.getItems());
      assertSame(byteList, collaborator.getListItems());
      assertSame(charSet, collaborator.getSetItems());
      assertSame(sortedSet, collaborator.getSortedSetItems());
   }

   @Test
   public void recordResultForMethodThatReturnsIterator()
   {
      final Collaborator collaborator = new Collaborator();
      final Iterator<String> itr = asList("ab", "cde").iterator();

      new Expectations(collaborator) {{
         collaborator.getIterator(); returns(itr);
      }};

      assertSame(itr, collaborator.getIterator());
   }

   @Test
   public void returnsValueOfIncompatibleTypeForMethodReturningArray(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getBooleanArray(); returns(new HashSet());
         mock.getStringArray(); returns(Collections.emptyList());
         mock.getIntArray(); returns(new short[0]);
      }};

      try { mock.getBooleanArray(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getStringArray(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getIntArray(); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void returnsValueOfIncompatibleTypeForMethodReturningCollection(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getListItems(); returns(Collections.emptySet());
         mock.getSetItems(); returns(new ArrayList());
         mock.getItems(); returns(new char[0]);
      }};

      try { mock.getListItems(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getSetItems(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getItems(); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void returnsValueOfIncompatibleTypeForMethodReturningIterator(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getIterator();
         returns(Collections.emptySet());
         returns(asList("a", true, 123));
         returns(new char[] {'A', 'b'});
      }};

      try { mock.getIterator(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getIterator(); fail(); } catch (ClassCastException ignore) {}
      try { mock.getIterator(); fail(); } catch (ClassCastException ignore) {}
   }

   @Test
   public void returnsEmptyArrayForSimpleReturnType(@Mocked final Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);

      new Expectations() {{
         mock.getString();
         returns(new String[0]);
      }};
   }

   @Test
   public void returnsEmptyCollectionForSimpleReturnType(@Mocked final Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);

      new Expectations() {{
         mock.getString();
         returns(Collections.emptyList());
      }};
   }

   @Test
   public void returnsMockedCollectionForSimpleReturnType(
      @Mocked final Collaborator mock, @Mocked final List<String> values)
   {
      thrown.expect(IllegalArgumentException.class);

      new Expectations() {{
         mock.getString();
         result = values;
      }};
   }

   @Test
   public void returnsEmptyIteratorForSimpleReturnType(@Mocked final Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);

      new Expectations() {{
         mock.getString();
         returns(Collections.emptySet().iterator());
      }};
   }

   @Test
   public void recordReturnValueForVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService();
         returns(123);
      }};

      mock.provideSomeService();
   }

   @Test
   public void recordReturnValueForConstructor(@Mocked Collaborator mock)
   {
      new Expectations() {{
         new Collaborator();
         returns("test");
      }};

      new Collaborator();
   }

   @Test
   public void recordMultipleReturnValuesForVoidMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService();
         returns(null, 123, "abc");
      }};

      mock.provideSomeService();
      mock.provideSomeService();
   }

   @Test
   public void recordMultipleReturnValuesForConstructor(@Mocked Collaborator mock)
   {
      new StrictExpectations() {{
         new Collaborator();
         returns(123, null, "abc");
      }};

      new Collaborator();
      new Collaborator();
      new Collaborator();

      try {
         new Collaborator();
         fail();
      }
      catch (UnexpectedInvocation e) {
         assertTrue(e.getMessage().startsWith("Unexpected invocation "));
      }
   }

   @Test
   public void recordResultsForCollectionAndListReturningMethodsUsingVarargs(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getItems(); returns(1, "2", 3.0);
         mock.getListItems(); returns("a", true);
      }};

      Collaborator collaborator = new Collaborator();
      assertArrayEquals(new Object[] {1, "2", 3.0}, collaborator.getItems().toArray());
      assertArrayEquals(new Object[] {"a", true}, collaborator.getListItems().toArray());
   }

   @Test
   public void recordResultsForIterableReturningMethodUsingVarargs(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getIterable(); returns(true, "Xyz", 3.6);
      }};

      int i = 0;
      Object[] expectedValues = {true, "Xyz", 3.6};

      for (Object value : mock.getIterable()) {
         assertEquals(expectedValues[i++], value);
      }
   }

   @Test
   public void recordResultsForIteratorReturningMethodUsingVarargs(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getIterator();
         returns("ab", "cde", 1, 3);
      }};

      Iterator<?> itr = new Collaborator().getIterator();
      assertEquals("ab", itr.next());
      assertEquals("cde", itr.next());
      assertEquals(1, itr.next());
      assertEquals(3, itr.next());
   }

   @Test
   public void recordResultsForSetReturningMethodUsingVarargs(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getSetItems(); returns(4.0, "aB", 2);
         mock.getSortedSetItems(); returns(1, 5, 123);
      }};

      assertArrayEquals(new Object[] {4.0, "aB", 2}, mock.getSetItems().toArray());
      assertArrayEquals(new Object[] {1, 5, 123}, mock.getSortedSetItems().toArray());
   }

   @Test
   public void recordResultsForArrayReturningMethodsUsingVarargs()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.getIntArray(); returns(1, 2, 3, 4);
         collaborator.getLongArray(); returns(1023, 20234L, 354);
         collaborator.getByteArray(); returns(0, -4, 5);
         collaborator.getByteWrapperArray(); returns(0, -4, 5);
         collaborator.getCharArray(); returns('a', 'B');
         collaborator.getShortArray(); returns(-1, 3, 0);
         collaborator.getShortWrapperArray(); returns(-1, 3, 0);
         collaborator.getFloatArray(); returns(-0.1F, 5.6F, 7);
         collaborator.getDoubleArray(); returns(4.1, 15, -7.0E2);
         collaborator.getStringArray(); returns("aX", null, "B2 m");
      }};

      assertArrayEquals(new int[] {1, 2, 3, 4}, collaborator.getIntArray());
      assertArrayEquals(new long[] {1023, 20234, 354}, collaborator.getLongArray());
      assertArrayEquals(new byte[] {0, -4, 5}, collaborator.getByteArray());
      assertArrayEquals(new Byte[] {0, -4, 5}, collaborator.getByteWrapperArray());
      assertArrayEquals(new char[] {'a', 'B'}, collaborator.getCharArray());
      assertArrayEquals(new short[] {-1, 3, 0}, collaborator.getShortArray());
      assertArrayEquals(new Short[] {-1, 3, 0}, collaborator.getShortWrapperArray());
      assertArrayEquals(new float[] {-0.1F, 5.6F, 7}, collaborator.getFloatArray(), 0.0F);
      assertArrayEquals(new double[] {4.0, 15, -7.0001E2}, collaborator.getDoubleArray(), 0.1);
      assertArrayEquals(new String[] {"aX", null, "B2 m"}, collaborator.getStringArray());
   }

   @Test
   public void recordMultipleIteratorsToBeReturnedFromMethodThatReturnsIterator(@Mocked final Collaborator mock)
   {
      final Iterator<?> firstResult = new ArrayList<Object>().listIterator();
      final ListIterator<?> secondResult = new LinkedList<Object>().listIterator();
      final Iterator<?> thirdResult = new ArrayList<Object>().iterator();

      new Expectations() {{
         mock.getListIterator();
         returns(firstResult, secondResult);

         mock.getIterator();
         returns(firstResult, secondResult, thirdResult);
      }};

      assertSame(firstResult, mock.getListIterator());
      assertSame(secondResult, mock.getListIterator());

      assertSame(firstResult, mock.getIterator());
      assertSame(secondResult, mock.getIterator());
      assertSame(thirdResult, mock.getIterator());
   }

   @Test
   public void recordMultipleListsToBeReturnedFromMethodThatReturnsList(@Mocked final Collaborator mock)
   {
      final List<?> firstResult = new ArrayList<Object>();
      final List<?> secondResult = new ArrayList<Object>();
      final List<?> thirdResult = new LinkedList<Object>();

      new Expectations() {{
         mock.getAList();
         returns(firstResult, secondResult);

         mock.getListItems();
         returns(firstResult, secondResult, thirdResult);
      }};

      assertSame(firstResult, mock.getAList());
      assertSame(secondResult, mock.getAList());

      assertSame(firstResult, mock.getListItems());
      assertSame(secondResult, mock.getListItems());
      assertSame(thirdResult, mock.getListItems());
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
}
