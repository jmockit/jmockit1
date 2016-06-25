/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.util.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.expectations.transformation.InvocationBlockModifier.*;
import static mockit.external.asm.Opcodes.*;

final class ArgumentCapturing
{
   @Nonnull private final InvocationBlockModifier modifier;
   @Nullable private List<Capture> captures;
   private boolean parameterForCapture;
   @Nullable private String capturedTypeDesc;
   private boolean capturesFound;

   ArgumentCapturing(@Nonnull InvocationBlockModifier modifier) { this.modifier = modifier; }

   boolean registerMatcher(boolean withCaptureMethod, @Nonnull String methodDesc, int lastLoadedVarIndex)
   {
      if (withCaptureMethod && "(Ljava/lang/Object;)Ljava/util/List;".equals(methodDesc)) {
         return false;
      }

      if (withCaptureMethod) {
         if (methodDesc.contains("List")) {
            if (lastLoadedVarIndex > 0) {
               Capture capture = modifier.new Capture(lastLoadedVarIndex);
               addCapture(capture);
            }

            parameterForCapture = false;
         }
         else {
            parameterForCapture = true;
         }
      }
      else {
         parameterForCapture = false;
      }

      return true;
   }

   void registerTypeToCaptureIfApplicable(int opcode, @Nonnull String typeDesc)
   {
      if (opcode == CHECKCAST && parameterForCapture) {
         capturedTypeDesc = typeDesc;
      }
   }

   void registerTypeToCaptureIntoListIfApplicable(int varIndex, @Nonnull String signature)
   {
      if (signature.startsWith("Ljava/util/List<")) {
         String typeDesc = signature.substring(16, signature.length() - 2);
         Type type = Type.getType(typeDesc);
         ActiveInvocations.varIndexToTypeDesc.put(varIndex, type.getInternalName());
      }
   }

   void registerAssignmentToCaptureVariableIfApplicable(int opcode, int varIndex)
   {
      if (opcode >= ISTORE && opcode <= ASTORE && parameterForCapture) {
         Capture capture = modifier.new Capture(opcode, varIndex, capturedTypeDesc);
         addCapture(capture);
         parameterForCapture = false;
         capturedTypeDesc = null;
      }
   }

   private void addCapture(@Nonnull Capture capture)
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
