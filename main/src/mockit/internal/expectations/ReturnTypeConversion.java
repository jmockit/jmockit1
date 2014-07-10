/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.util.*;

import static mockit.internal.util.ConstructorReflection.newInstanceUsingPublicConstructorIfAvailable;

final class ReturnTypeConversion
{
   private static final Class<?>[] STRING = {String.class};

   @NotNull private final Expectation expectation;
   @NotNull private final Class<?> returnType;
   @NotNull private final Object valueToReturn;

   ReturnTypeConversion(@NotNull Expectation expectation, @NotNull Class<?> returnType, @NotNull Object value)
   {
      this.expectation = expectation;
      this.returnType = returnType;
      valueToReturn = value;
   }

   void addConvertedValueOrValues()
   {
      boolean valueIsArray = valueToReturn.getClass().isArray();
      boolean valueIsIterable = valueToReturn instanceof Iterable<?>;

      if (valueIsArray || valueIsIterable || valueToReturn instanceof Iterator<?>) {
         if (returnType == void.class || hasReturnOfDifferentType()) {
            if (valueIsArray) {
               expectation.getResults().addReturnValues(valueToReturn);
            }
            else if (valueIsIterable) {
               expectation.getResults().addReturnValues((Iterable<?>) valueToReturn);
            }
            else {
               expectation.getResults().addDeferredReturnValues((Iterator<?>) valueToReturn);
            }

            return;
         }
      }

      expectation.substituteCascadedMockToBeReturnedIfNeeded();
      expectation.getResults().addReturnValue(valueToReturn);
   }

   private boolean hasReturnOfDifferentType()
   {
      return
         !returnType.isArray() &&
         !Iterable.class.isAssignableFrom(returnType) && !Iterator.class.isAssignableFrom(returnType) &&
         !returnType.isAssignableFrom(valueToReturn.getClass());
   }

   void addConvertedValue()
   {
      Class<?> wrapperType =
         AutoBoxing.isWrapperOfPrimitiveType(returnType) ? returnType : AutoBoxing.getWrapperType(returnType);
      Class<?> valueType = valueToReturn.getClass();

      if (valueType == wrapperType) {
         expectation.getResults().addReturnValueResult(valueToReturn);
      }
      else if (wrapperType != null && AutoBoxing.isWrapperOfPrimitiveType(valueType)) {
         addPrimitiveValueConvertingAsNeeded(wrapperType);
      }
      else {
         boolean valueIsArray = valueType.isArray();

         if (valueIsArray || valueToReturn instanceof Iterable<?> || valueToReturn instanceof Iterator<?>) {
            addMultiValuedResultBasedOnTheReturnType(valueIsArray);
         }
         else if (wrapperType != null) {
            throw newIncompatibleTypesException();
         }
         else {
            addResultFromSingleValue();
         }
      }
   }

   private void addMultiValuedResultBasedOnTheReturnType(boolean valueIsArray)
   {
      if (returnType == void.class) {
         addMultiValuedResult(valueIsArray);
      }
      else if (returnType == Object.class) {
         expectation.getResults().addReturnValueResult(valueToReturn);
      }
      else if (valueIsArray && addCollectionOrMapWithElementsFromArray()) {
         return;
      }
      else if (hasReturnOfDifferentType()) {
         addMultiValuedResult(valueIsArray);
      }
      else {
         expectation.getResults().addReturnValueResult(valueToReturn);
      }
   }

   private void addMultiValuedResult(boolean valueIsArray)
   {
      if (valueIsArray) {
         expectation.getResults().addResults(valueToReturn);
      }
      else if (valueToReturn instanceof Iterable<?>) {
         expectation.getResults().addResults((Iterable<?>) valueToReturn);
      }
      else {
         expectation.getResults().addDeferredResults((Iterator<?>) valueToReturn);
      }
   }

   private boolean addCollectionOrMapWithElementsFromArray()
   {
      int n = Array.getLength(valueToReturn);
      Object values = null;

      if (returnType.isAssignableFrom(ListIterator.class)) {
         List<Object> list = new ArrayList<Object>(n);
         addArrayElements(list, n);
         values = list.listIterator();
      }
      else if (returnType.isAssignableFrom(List.class)) {
         values = addArrayElements(new ArrayList<Object>(n), n);
      }
      else if (returnType.isAssignableFrom(Set.class)) {
         values = addArrayElements(new LinkedHashSet<Object>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedSet.class)) {
         values = addArrayElements(new TreeSet<Object>(), n);
      }
      else if (returnType.isAssignableFrom(Map.class)) {
         values = addArrayElements(new LinkedHashMap<Object, Object>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedMap.class)) {
         values = addArrayElements(new TreeMap<Object, Object>(), n);
      }

      if (values != null) {
         expectation.getResults().addReturnValue(values);
         return true;
      }

      return false;
   }

   @NotNull
   private Object addArrayElements(@NotNull Collection<Object> values, int elementCount)
   {
      for (int i = 0; i < elementCount; i++) {
         Object element = Array.get(valueToReturn, i);
         values.add(element);
      }

      return values;
   }

   @Nullable
   private Object addArrayElements(@NotNull Map<Object, Object> values, int elementPairCount)
   {
      for (int i = 0; i < elementPairCount; i++) {
         Object keyAndValue = Array.get(valueToReturn, i);

         if (keyAndValue == null || !keyAndValue.getClass().isArray()) {
            return null;
         }

         Object key = Array.get(keyAndValue, 0);
         Object element = Array.getLength(keyAndValue) > 1 ? Array.get(keyAndValue, 1) : null;
         values.put(key, element);
      }

      return values;
   }

   private void addResultFromSingleValue()
   {
      if (returnType == Object.class) {
         expectation.getResults().addReturnValueResult(valueToReturn);
      }
      else if (returnType == void.class) {
         throw newIncompatibleTypesException();
      }
      else if (returnType.isArray()) {
         Object array = Array.newInstance(returnType.getComponentType(), 1);
         Array.set(array, 0, valueToReturn);
         expectation.getResults().addReturnValueResult(array);
      }
      else if (returnType.isAssignableFrom(ArrayList.class)) {
         addCollectionWithSingleElement(new ArrayList<Object>(1));
      }
      else if (returnType.isAssignableFrom(LinkedList.class)) {
         addCollectionWithSingleElement(new LinkedList<Object>());
      }
      else if (returnType.isAssignableFrom(HashSet.class)) {
         addCollectionWithSingleElement(new HashSet<Object>(1));
      }
      else if (returnType.isAssignableFrom(TreeSet.class)) {
         addCollectionWithSingleElement(new TreeSet<Object>());
      }
      else if (returnType.isAssignableFrom(ListIterator.class)) {
         List<Object> l = new ArrayList<Object>(1);
         l.add(valueToReturn);
         expectation.getResults().addReturnValueResult(l.listIterator());
      }
      else if (valueToReturn instanceof CharSequence) {
         addCharSequence((CharSequence) valueToReturn);
      }
      else {
         Class<?> primitiveType = AutoBoxing.getPrimitiveType(valueToReturn.getClass());

         if (primitiveType != null) {
            Class<?>[] parameterType = {primitiveType};
            Object convertedValue =
               newInstanceUsingPublicConstructorIfAvailable(returnType, parameterType, valueToReturn);

            if (convertedValue == null) {
               convertedValue =
                  MethodReflection.invokePublicIfAvailable(returnType, null, "valueOf", parameterType, valueToReturn);
            }

            if (convertedValue != null) {
               expectation.getResults().addReturnValueResult(convertedValue);
               return;
            }
         }

         throw newIncompatibleTypesException();
      }
   }

   private void addCollectionWithSingleElement(@NotNull Collection<Object> container)
   {
      container.add(valueToReturn);
      expectation.getResults().addReturnValueResult(container);
   }

   private void addCharSequence(@NotNull CharSequence textualValue)
   {
      Object convertedValue = textualValue;

      if (returnType.isAssignableFrom(ByteArrayInputStream.class)) {
         convertedValue = new ByteArrayInputStream(textualValue.toString().getBytes());
      }
      else if (returnType.isAssignableFrom(StringReader.class)) {
         convertedValue = new StringReader(textualValue.toString());
      }
      else if (!(textualValue instanceof StringBuilder) && returnType.isAssignableFrom(StringBuilder.class)) {
         convertedValue = new StringBuilder(textualValue);
      }
      else if (!(textualValue instanceof CharBuffer) && returnType.isAssignableFrom(CharBuffer.class)) {
         convertedValue = CharBuffer.wrap(textualValue);
      }
      else {
         Object valueFromText = newInstanceUsingPublicConstructorIfAvailable(returnType, STRING, textualValue);

         if (valueFromText != null) {
            convertedValue = valueFromText;
         }
      }

      expectation.getResults().addReturnValueResult(convertedValue);
   }

   @NotNull private IllegalArgumentException newIncompatibleTypesException()
   {
      ExpectedInvocation invocation = expectation.invocation;
      String valueTypeName = valueToReturn.getClass().getName().replace("java.lang.", "");
      String returnTypeName = returnType.getName().replace("java.lang.", "");

      StringBuilder msg = new StringBuilder(200);
      msg.append("Value of type ").append(valueTypeName);
      msg.append(" incompatible with return type ").append(returnTypeName).append(" of ");
      msg.append(new MethodFormatter(invocation.getClassDesc(), invocation.getMethodNameAndDescription()));

      return new IllegalArgumentException(msg.toString());
   }

   private void addPrimitiveValueConvertingAsNeeded(@NotNull Class<?> targetType)
   {
      Object convertedValue = null;

      if (valueToReturn instanceof Number) {
         convertedValue = convertFromNumber(targetType, (Number) valueToReturn);
      }
      else if (valueToReturn instanceof Character) {
         convertedValue = convertFromChar(targetType, (Character) valueToReturn);
      }

      if (convertedValue == null) {
         throw newIncompatibleTypesException();
      }

      expectation.getResults().addReturnValueResult(convertedValue);
   }

   @Nullable
   private Object convertFromNumber(@NotNull Class<?> targetType, @NotNull Number number)
   {
      if (targetType == Integer.class) {
         return number.intValue();
      }
      else if (targetType == Short.class) {
         return number.shortValue();
      }
      else if (targetType == Long.class) {
         return number.longValue();
      }
      else if (targetType == Byte.class) {
         return number.byteValue();
      }
      else if (targetType == Double.class) {
         return number.doubleValue();
      }
      else if (targetType == Float.class) {
         return number.floatValue();
      }
      else if (targetType == Character.class) {
         return (char) number.intValue();
      }

      return null;
   }

   @Nullable
   private Object convertFromChar(@NotNull Class<?> targetType, char c)
   {
      if (targetType == Integer.class) {
         return (int) c;
      }
      else if (targetType == Short.class) {
         return (short) c;
      }
      else if (targetType == Long.class) {
         return (long) c;
      }
      else if (targetType == Byte.class) {
         //noinspection NumericCastThatLosesPrecision
         return (byte) c;
      }
      else if (targetType == Double.class) {
         return (double) c;
      }
      else if (targetType == Float.class) {
         return (float) c;
      }

      return null;
   }
}
