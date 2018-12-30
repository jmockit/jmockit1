/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.*;

final class MultiValuedConversion
{
   @Nonnull private final InvocationResults invocationResults;
   @Nonnull private final Class<?> returnType;
   @Nonnull private final Object valueToReturn;

   MultiValuedConversion(@Nonnull InvocationResults invocationResults, @Nonnull Class<?> returnType, @Nonnull Object valueToReturn) {
      this.invocationResults = invocationResults;
      this.returnType = returnType;
      this.valueToReturn = valueToReturn;
   }

   void addMultiValuedResultBasedOnTheReturnType(boolean valueIsArray) {
      if (returnType == void.class) {
         addMultiValuedResult(valueIsArray);
      }
      else if (returnType == Object.class) {
         invocationResults.addReturnValueResult(valueToReturn);
      }
      else if (valueIsArray && addCollectionOrMapWithElementsFromArray()) {
         return;
      }
      else if (hasReturnOfDifferentType()) {
         addMultiValuedResult(valueIsArray);
      }
      else {
         invocationResults.addReturnValueResult(valueToReturn);
      }
   }

   @SuppressWarnings("Since15")
   private void addMultiValuedResult(boolean valueIsArray) {
      if (valueIsArray) {
         invocationResults.addResults(valueToReturn);
      }
      else if (valueToReturn instanceof Iterable<?>) {
         if (JAVA8 && valueToReturn instanceof Collection && returnType.isAssignableFrom(Stream.class)) {
            invocationResults.addReturnValueResult(((Collection<?>) valueToReturn).stream());
         }
         else {
            invocationResults.addResults((Iterable<?>) valueToReturn);
         }
      }
      else {
         invocationResults.addDeferredResults((Iterator<?>) valueToReturn);
      }
   }

   private boolean hasReturnOfDifferentType() {
      return
         !returnType.isArray() &&
         !Iterable.class.isAssignableFrom(returnType) && !Iterator.class.isAssignableFrom(returnType) &&
         !returnType.isAssignableFrom(valueToReturn.getClass());
   }

   @SuppressWarnings("Since15")
   private boolean addCollectionOrMapWithElementsFromArray() {
      @Nonnegative int n = Array.getLength(valueToReturn);
      Object values = null;

      if (returnType.isAssignableFrom(ListIterator.class)) {
         List<Object> list = new ArrayList<>(n);
         addArrayElements(list, n);
         values = list.listIterator();
      }
      else if (returnType.isAssignableFrom(List.class)) {
         values = addArrayElements(new ArrayList<>(n), n);
      }
      else if (returnType.isAssignableFrom(Set.class)) {
         values = addArrayElements(new LinkedHashSet<>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedSet.class)) {
         values = addArrayElements(new TreeSet<>(), n);
      }
      else if (returnType.isAssignableFrom(Map.class)) {
         values = addArrayElements(new LinkedHashMap<>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedMap.class)) {
         values = addArrayElements(new TreeMap<>(), n);
      }
      else if (JAVA8 && returnType.isAssignableFrom(Stream.class)) {
         values = addArrayElements(new ArrayList<>(n), n).stream();
      }

      if (values != null) {
         invocationResults.addReturnValue(values);
         return true;
      }

      return false;
   }

   @Nonnull
   private Collection<?> addArrayElements(@Nonnull Collection<Object> values, @Nonnegative int elementCount) {
      for (int i = 0; i < elementCount; i++) {
         Object element = Array.get(valueToReturn, i);
         values.add(element);
      }

      return values;
   }

   @Nullable
   private Object addArrayElements(@Nonnull Map<Object, Object> values, @Nonnegative int elementPairCount) {
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
}
