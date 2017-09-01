/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class ExpectationsWithValuesToReturnTest
{
   static class Collaborator
   {
      static String doInternal() { return "123"; }

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

   @Test
   public void returnsMultipleValuesInSequenceUsingVarargs()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.getBooleanValue(); returns(true, false);
         collaborator.getShortValue(); returns((short) 1, (short) 2, (short) 3);
         collaborator.getShortWrapper(); returns((short) 5, (short) 6, (short) -7, (short) -8);
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

      assertArrayEquals(new char[0], collaborator.getCharArray());
      assertArrayEquals(new char[]{'x'}, collaborator.getCharArray());
   }

   @Test
   public void returnsNonNullValueFollowedByNullUsingVarargs(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.getString();
         //noinspection NullArgumentToVariableArgMethod
         returns("non-null", null);
      }};

      assertEquals("non-null", collaborator.getString());
      assertNull(collaborator.getString());
      assertNull(collaborator.getString());
   }

   @Test
   public void returnsMultipleValuesFromMethodWithReturnTypeOfObject(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.getObject();
         returns(1, 2);
         returns("test", 'X');
      }};

      assertEquals(1, collaborator.getObject());
      assertEquals(2, collaborator.getObject());
      assertEquals("test", collaborator.getObject());
      assertEquals('X', collaborator.getObject());
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
}
