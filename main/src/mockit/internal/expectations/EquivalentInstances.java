/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;

final class EquivalentInstances
{
   @Nonnull final Map<Object, Object> instanceMap;
   @Nonnull final Map<Object, Object> replacementMap;

   EquivalentInstances() {
      instanceMap = new IdentityHashMap<>();
      replacementMap = new IdentityHashMap<>();
   }

   void registerReplacementInstanceIfApplicable(@Nullable Object mock, @Nonnull ExpectedInvocation invocation) {
      Object replacementInstance = invocation.replacementInstance;

      if (replacementInstance != null && replacementInstance != invocation.instance) {
         replacementMap.put(mock, replacementInstance);
      }
   }

   boolean isEquivalentInstance(@Nonnull Object invocationInstance, @Nonnull Object invokedInstance) {
      return
         invocationInstance == invokedInstance ||
         invocationInstance == replacementMap.get(invokedInstance) ||
         invocationInstance == instanceMap.get(invokedInstance) ||
         invokedInstance == instanceMap.get(invocationInstance);
   }

   boolean areNonEquivalentInstances(@Nonnull Object invocationInstance, @Nonnull Object invokedInstance) {
      boolean recordedInstanceMatchingAnyInstance = !isMatchingInstance(invocationInstance);
      boolean invokedInstanceMatchingSpecificInstance = isMatchingInstance(invokedInstance);
      return recordedInstanceMatchingAnyInstance && invokedInstanceMatchingSpecificInstance;
   }

   private boolean isMatchingInstance(@Nonnull Object instance) {
      return
         instanceMap.containsKey(instance)    || instanceMap.containsValue(instance) ||
         replacementMap.containsKey(instance) || replacementMap.containsValue(instance);
   }

   boolean areMatchingInstances(boolean matchInstance, @Nonnull Object mock1, @Nonnull Object mock2) {
      if (matchInstance) {
         return isEquivalentInstance(mock1, mock2);
      }

      return !areInDifferentEquivalenceSets(mock1, mock2);
   }

   private boolean areInDifferentEquivalenceSets(@Nonnull Object mock1, @Nonnull Object mock2) {
      if (mock1 == mock2 || instanceMap.isEmpty()) {
         return false;
      }

      Object mock1Equivalent = instanceMap.get(mock1);
      Object mock2Equivalent = instanceMap.get(mock2);

      if (mock1Equivalent == mock2 || mock2Equivalent == mock1) {
         return false;
      }

      //noinspection SimplifiableIfStatement
      if (mock1Equivalent != null && mock2Equivalent != null) {
         return true;
      }

      return instanceMapHasMocksInSeparateEntries(mock1, mock2);
   }

   private boolean instanceMapHasMocksInSeparateEntries(@Nonnull Object mock1, @Nonnull Object mock2) {
      boolean found1 = false;
      boolean found2 = false;

      for (Entry<Object, Object> entry : instanceMap.entrySet()) {
         if (!found1 && isInMapEntry(entry, mock1)) {
            found1 = true;
         }

         if (!found2 && isInMapEntry(entry, mock2)) {
            found2 = true;
         }

         if (found1 && found2) {
            return true;
         }
      }

      return false;
   }

   private static boolean isInMapEntry(@Nonnull Entry<Object, Object> mapEntry, @Nonnull Object mock) {
      return mapEntry.getKey() == mock || mapEntry.getValue() == mock;
   }

   @Nullable
   Object getReplacementInstanceForMethodInvocation(@Nonnull Object invokedInstance, @Nonnull String methodNameAndDesc) {
      return methodNameAndDesc.charAt(0) == '<' ? null : replacementMap.get(invokedInstance);
   }

   boolean isReplacementInstance(@Nonnull Object invokedInstance, @Nonnull String methodNameAndDesc) {
      return
         methodNameAndDesc.charAt(0) != '<' && (
            replacementMap.containsKey(invokedInstance) || replacementMap.containsValue(invokedInstance)
         );
   }
}
