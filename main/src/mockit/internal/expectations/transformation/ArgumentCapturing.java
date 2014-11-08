/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.util.*;

import org.jetbrains.annotations.*;

import static mockit.external.asm.Opcodes.*;

import mockit.internal.expectations.transformation.InvocationBlockModifier.Capture;

final class ArgumentCapturing
{
   @Nullable private List<Capture> captures;
   private boolean parameterForCapture;
   @Nullable private String capturedTypeDesc;
   private boolean capturesFound;

   boolean registerMatcher(boolean withCaptureMethod, @NotNull String methodDesc)
   {
      if (withCaptureMethod && "(Ljava/lang/Object;)Ljava/util/List;".equals(methodDesc)) {
         return false;
      }

      parameterForCapture = withCaptureMethod && !methodDesc.contains("List");
      return true;
   }

   void registerTypeToCaptureIfApplicable(int opcode, @NotNull String type)
   {
      if (opcode == CHECKCAST && parameterForCapture) {
         capturedTypeDesc = type;
      }
   }

   void registerAssignmentToCaptureVariableIfApplicable(
      @NotNull InvocationBlockModifier modifier, int opcode, int varIndex)
   {
      if (opcode >= ISTORE && opcode <= ASTORE && parameterForCapture) {
         Capture capture = modifier.createCapture(opcode, varIndex, capturedTypeDesc);
         addCapture(capture);
         parameterForCapture = false;
         capturedTypeDesc = null;
      }
   }

   private void addCapture(@NotNull Capture capture)
   {
      if (captures == null) {
         captures = new ArrayList<Capture>();
         capturesFound = true;
      }

      captures.add(capture);
   }

   void updateCaptureIfAny(int originalIndex, int newIndex)
   {
      if (captures != null) {
         for (int i = captures.size() - 1; i >= 0; i--) {
            Capture capture = captures.get(i);

            if (capture.fixParameterIndex(originalIndex, newIndex)) {
               break;
            }
         }
      }
   }

   void generateCallsToSetArgumentTypesToCaptureIfAny()
   {
      if (captures != null) {
         for (Capture capture : captures) {
            capture.generateCallToSetArgumentTypeIfNeeded();
         }
      }
   }

   void generateCallsToCaptureMatchedArgumentsIfPending()
   {
      if (captures != null) {
         for (Capture capture : captures) {
            capture.generateCodeToStoreCapturedValue();
         }

         captures = null;
      }
   }

   boolean hasCaptures() { return capturesFound; }
}
