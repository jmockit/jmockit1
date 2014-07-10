/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.*;

final class CaptureOfNewInstancesForFields extends CaptureOfNewInstances
{
   void resetCaptureCount(@NotNull Field mockField)
   {
      for (List<Capture> fieldsWithCapture : baseTypeToCaptures.values()) {
         resetCaptureCount(mockField, fieldsWithCapture);
      }
   }

   private void resetCaptureCount(@NotNull Field mockField, @NotNull List<Capture> fieldsWithCapture)
   {
      for (Capture fieldWithCapture : fieldsWithCapture) {
         if (fieldWithCapture.typeMetadata.field == mockField) {
            fieldWithCapture.reset();
         }
      }
   }
}
