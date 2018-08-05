/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.containsReference;

final class PartiallyMockedInstances
{
   @Nonnull private final List<?> dynamicMockInstancesToMatch;

   PartiallyMockedInstances(@Nonnull List<?> dynamicMockInstancesToMatch) {
      this.dynamicMockInstancesToMatch = dynamicMockInstancesToMatch;
   }

   boolean isToBeMatchedOnInstance(@Nonnull Object mock) {
      return containsReference(dynamicMockInstancesToMatch, mock);
   }

   boolean isDynamicMockInstanceOrClass(@Nonnull Object invokedInstance, @Nonnull Object invocationInstance) {
      if (containsReference(dynamicMockInstancesToMatch, invokedInstance)) {
         return true;
      }

      Class<?> invokedClass = invocationInstance.getClass();

      for (Object dynamicMock : dynamicMockInstancesToMatch) {
         if (dynamicMock.getClass() == invokedClass) {
            return true;
         }
      }

      return false;
   }
}
