/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.util.*;

import org.jetbrains.annotations.*;

import static mockit.external.asm4.Opcodes.*;

import mockit.internal.expectations.transformation.InvocationBlockModifier.Capture;

final class ArgumentCapturing
{
   boolean justAfterWithCaptureInvocation;
   @Nullable private List<Capture> captures;
   private boolean parameterForCapture;
   @Nullable private String capturedTypeDesc;

   boolean registerMatcher(@NotNull String methodName, @NotNull String methodDesc)
   {
      boolean withCaptureMethod = "withCapture".equals(methodName);

      if (withCaptureMethod && "(Ljava/lang/Object;)Ljava/util/List;".equals(methodDesc)) {
         return false;
      }

      justAfterWithCaptureInvocation = withCaptureMethod;
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
}
