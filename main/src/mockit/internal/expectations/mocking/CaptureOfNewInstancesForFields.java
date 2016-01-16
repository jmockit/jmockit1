/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

final class CaptureOfNewInstancesForFields extends CaptureOfNewInstances
{
   void resetCaptureCount(@Nonnull Field mockField)
   {
      Collection<List<Capture>> capturesForAllBaseTypes = getCapturesForAllBaseTypes();

      for (List<Capture> fieldsWithCapture : capturesForAllBaseTypes) {
         resetCaptureCount(mockField, fieldsWithCapture);
      }
   }

   private static void resetCaptureCount(@Nonnull Field mockField, @Nonnull List<Capture> fieldsWithCapture)
   {
      for (Capture fieldWithCapture : fieldsWithCapture) {
         if (fieldWithCapture.typeMetadata.field == mockField) {
            fieldWithCapture.reset();
         }
      }
   }
}
