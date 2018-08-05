/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.util.*;
import static mockit.internal.util.Utilities.containsReference;

final class InstanceBasedMatching
{
   @Nonnull private final List<Class<?>> mockedTypesToMatchOnInstances;

   InstanceBasedMatching() { mockedTypesToMatchOnInstances = new LinkedList<>(); }

   void discoverMockedTypesToMatchOnInstances(@Nonnull List<Class<?>> targetClasses) {
      int numClasses = targetClasses.size();

      if (numClasses > 1) {
         for (int i = 0; i < numClasses; i++) {
            Class<?> targetClass = targetClasses.get(i);

            if (targetClasses.lastIndexOf(targetClass) > i) {
               addMockedTypeToMatchOnInstance(targetClass);
            }
         }
      }
   }

   private void addMockedTypeToMatchOnInstance(@Nonnull Class<?> mockedType) {
      if (!containsReference(mockedTypesToMatchOnInstances, mockedType)) {
         mockedTypesToMatchOnInstances.add(mockedType);
      }
   }

   boolean isToBeMatchedOnInstance(@Nonnull Object mock) {
      Class<?> mockedClass = GeneratedClasses.getMockedClass(mock);
      return containsReference(mockedTypesToMatchOnInstances, mockedClass);
   }
}
