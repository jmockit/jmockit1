/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.util.*;
import javax.annotation.*;

import mockit.asm.annotations.*;
import mockit.asm.controlFlow.*;
import mockit.asm.methods.*;
import mockit.coverage.data.*;
import mockit.coverage.lines.*;
import static mockit.asm.jvmConstants.Opcodes.*;

final class MethodModifier extends WrappingMethodVisitor
{
   private static final String DATA_RECORDING_CLASS = "mockit/coverage/TestRun";

   @Nonnull private final List<Label> visitedLabels;
   @Nonnull private final List<Label> jumpTargetsForCurrentLine;
   @Nonnull private final List<Integer> pendingBranches;
   @Nonnull private final String sourceFileName;
   @Nonnull private final FileCoverageData fileData;
   @Nonnull private final PerFileLineCoverage lineCoverageInfo;
   @Nonnegative private int currentLine;
   @Nonnegative private int lineExpectingInstructionAfterJump;
   private boolean assertFoundInCurrentLine;
   private boolean ignoreUntilNextLabel;
   @SuppressWarnings("unused") private boolean foundPotentialAssertFalse;
   @Nonnegative private int foundPotentialBooleanExpressionValue;
   private boolean foundInterestingInstruction;
   @Nonnegative int ignoreUntilNextSwitch;

   MethodModifier(@Nonnull MethodWriter mw, @Nonnull String sourceFileName, @Nonnull FileCoverageData fileData) {
      super(mw);
      visitedLabels = new ArrayList<>();
      jumpTargetsForCurrentLine = new ArrayList<>(4);
      pendingBranches = new ArrayList<>(6);
      this.sourceFileName = sourceFileName;
      this.fileData = fileData;
      lineCoverageInfo = fileData.getLineCoverageData();
   }

   @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      boolean isTestMethod = desc.startsWith("Lorg/junit/") || desc.startsWith("Lorg/testng/");

      if (isTestMethod) {
         throw VisitInterruptedException.INSTANCE;
      }

      return mw.visitAnnotation(desc);
   }

   @Override
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {
      if (!pendingBranches.isEmpty()) {
         pendingBranches.clear();
      }

      lineCoverageInfo.addLine(line);
      currentLine = line;

      jumpTargetsForCurrentLine.clear();

      generateCallToRegisterLineExecution();

      mw.visitLineNumber(line, start);
   }

   private void generateCallToRegisterLineExecution() {
      mw.visitIntInsn(SIPUSH, fileData.index);
      pushCurrentLineOnTheStack();
      mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, "lineExecuted", "(II)V", false);
   }

   private void pushCurrentLineOnTheStack() {
      if (currentLine <= Short.MAX_VALUE) {
         mw.visitIntInsn(SIPUSH, currentLine);
      }
      else {
         mw.visitLdcInsn(currentLine);
      }
   }

   @Override
   public void visitJumpInsn(@Nonnegative int opcode, @Nonnull Label label) {
      if (opcode == GOTO || currentLine == 0 || ignoreUntilNextLabel || ignoreUntilNextSwitch > 0 || visitedLabels.contains(label)) {
         assertFoundInCurrentLine = false;
         mw.visitJumpInsn(opcode, label);

         if (opcode == GOTO && foundPotentialBooleanExpressionValue == 1) {
            foundPotentialBooleanExpressionValue = 2;
         }

         return;
      }

      Label jumpingFrom = mw.getCurrentBlock();
      assert jumpingFrom != null;
      jumpingFrom.jumpTargetLine = currentLine;

      if (!jumpTargetsForCurrentLine.contains(label)) {
         jumpTargetsForCurrentLine.add(label);
      }

      LineCoverageData lineData = lineCoverageInfo.getOrCreateLineData(currentLine);
      int sourceBranchIndex = lineData.addBranchingPoint(jumpingFrom, label);
      pendingBranches.add(sourceBranchIndex);

      if (assertFoundInCurrentLine) {
         BranchCoverageData branchData = lineCoverageInfo.getBranchData(currentLine, sourceBranchIndex + 1);
         branchData.markAsUnreachable();
      }

      mw.visitJumpInsn(opcode, label);
      lineExpectingInstructionAfterJump = 0;
      generateCallToRegisterBranchTargetExecutionIfPending();
      lineExpectingInstructionAfterJump = currentLine;
   }

   private void generateCallToRegisterBranchTargetExecutionIfPending() {
      if (ignoreUntilNextLabel || ignoreUntilNextSwitch > 0) {
         return;
      }

      foundPotentialAssertFalse = false;
      foundPotentialBooleanExpressionValue = 0;

      if (!pendingBranches.isEmpty()) {
         for (Integer pendingBranchIndex : pendingBranches) {
            generateCallToRegisterBranchTargetExecution(pendingBranchIndex);
         }

         pendingBranches.clear();
      }

      if (lineExpectingInstructionAfterJump > 0) {
         if (currentLine > lineExpectingInstructionAfterJump) {
            lineCoverageInfo.markLastLineSegmentAsEmpty(lineExpectingInstructionAfterJump);
         }

         lineExpectingInstructionAfterJump = 0;
      }
   }

   private void generateCallToRegisterBranchTargetExecution(@Nonnegative int branchIndex) {
      mw.visitIntInsn(SIPUSH, fileData.index);
      pushCurrentLineOnTheStack();
      mw.visitIntInsn(SIPUSH, branchIndex);
      mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, "branchExecuted", "(III)V", false);
   }

   @Override
   public void visitLabel(@Nonnull Label label) {
      if (ignoreUntilNextLabel || ignoreUntilNextSwitch > 0) {
         mw.visitLabel(label);
         ignoreUntilNextLabel = false;
         return;
      }

      visitedLabels.add(label);
      mw.visitLabel(label);

      int jumpTargetIndex = jumpTargetsForCurrentLine.indexOf(label);

      if (jumpTargetIndex >= 0) {
         label.jumpTargetLine = label.line > 0 ? label.line : currentLine;
         int targetBranchIndex = 2 * jumpTargetIndex + 1;
         pendingBranches.add(targetBranchIndex);
         assertFoundInCurrentLine = false;
      }

      foundPotentialBooleanExpressionValue = 0;
   }

   @Override
   public void visitInsn(@Nonnegative int opcode) {
      boolean isReturn = opcode >= IRETURN && opcode <= RETURN;

      if (!isReturn && !isDefaultReturnValue(opcode)) {
         foundInterestingInstruction = true;
      }

      if (isReturn && !foundInterestingInstruction && visitedLabels.size() == 1) {
         lineCoverageInfo.getOrCreateLineData(currentLine).markAsUnreachable();
      }
      else if ((opcode == ICONST_0 || opcode == ICONST_1) && foundPotentialBooleanExpressionValue == 0) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         foundPotentialBooleanExpressionValue = 1;
      }
      else {
         generateCallToRegisterBranchTargetExecutionIfPending();
      }

      mw.visitInsn(opcode);
   }

   private static boolean isDefaultReturnValue(@Nonnegative int opcode) {
      return opcode == ACONST_NULL || opcode == ICONST_0 || opcode == LCONST_0 || opcode == FCONST_0 || opcode == DCONST_0;
   }

   @Override
   public void visitIntInsn(@Nonnegative int opcode, int operand) {
      foundInterestingInstruction = true;
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitIntInsn(opcode, operand);
   }

   @Override
   public void visitVarInsn(@Nonnegative int opcode, @Nonnegative int varIndex) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitVarInsn(opcode, varIndex);
   }

   @Override
   public void visitTypeInsn(@Nonnegative int opcode, @Nonnull String typeDesc) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitTypeInsn(opcode, typeDesc);
   }

   @Override
   public void visitFieldInsn(@Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      // TODO: need to also process field instructions inside accessor methods (STATIC + SYNTHETIC, "access$nnn")
      boolean getField = opcode == GETSTATIC || opcode == GETFIELD;
      boolean isStatic = opcode == PUTSTATIC || opcode == GETSTATIC;
      char fieldType = desc.charAt(0);
      boolean size2 = fieldType == 'J' || fieldType == 'D';
      String classAndFieldNames = null;
      boolean fieldHasData = false;

      if (!owner.startsWith("java/")) {
         classAndFieldNames = owner.substring(owner.lastIndexOf('/') + 1) + '.' + name;
         fieldHasData = fileData.dataCoverageInfo.isFieldWithCoverageData(classAndFieldNames);

         if (fieldHasData && !isStatic) {
            generateCodeToSaveInstanceReferenceOnTheStack(getField, size2);
         }
      }

      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitFieldInsn(opcode, owner, name, desc);

      if (opcode == GETSTATIC && "$assertionsDisabled".equals(name)) {
         assertFoundInCurrentLine = true;
         ignoreUntilNextLabel = true;
      }

      foundPotentialAssertFalse = true;

      if (fieldHasData) {
         generateCallToRegisterFieldCoverage(getField, isStatic, size2, classAndFieldNames);
      }
   }

   private void generateCodeToSaveInstanceReferenceOnTheStack(boolean getField, boolean size2) {
      if (getField) {
         mw.visitInsn(DUP);
      }
      else if (size2) {
         mw.visitInsn(DUP2_X1);
         mw.visitInsn(POP2);
         mw.visitInsn(DUP_X2);
         mw.visitInsn(DUP_X2);
         mw.visitInsn(POP);
      }
      else {
         mw.visitInsn(DUP_X1);
         mw.visitInsn(POP);
         mw.visitInsn(DUP_X1);
         mw.visitInsn(DUP_X1);
         mw.visitInsn(POP);
      }
   }

   private void generateCallToRegisterFieldCoverage(boolean getField, boolean isStatic, boolean size2, @Nonnull String classAndFieldNames) {
      if (!isStatic && getField) {
         if (size2) {
            mw.visitInsn(DUP2_X1);
            mw.visitInsn(POP2);
         }
         else {
            mw.visitInsn(DUP_X1);
            mw.visitInsn(POP);
         }
      }

      mw.visitLdcInsn(sourceFileName);
      mw.visitLdcInsn(classAndFieldNames);

      String methodToCall = getField ? "fieldRead" : "fieldAssigned";
      String methodDesc =
         isStatic ? "(Ljava/lang/String;Ljava/lang/String;)V" : "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

      mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, methodToCall, methodDesc, false);
   }

   @Override
   public void visitMethodInsn(@Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
      // This is to ignore bytecode belonging to a static initialization block inserted in a regular line of code by the Java
      // compiler when the class contains at least one "assert" statement.
      // Otherwise, that line of code would always appear as partially covered when running with assertions enabled.
      if (opcode == INVOKEVIRTUAL && "java/lang/Class".equals(owner) && "desiredAssertionStatus".equals(name)) {
         assertFoundInCurrentLine = true;
         ignoreUntilNextLabel = true;
      }

      if (opcode != INVOKESPECIAL || !"()V".equals(desc)) {
         foundInterestingInstruction = true;
      }

      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitMethodInsn(opcode, owner, name, desc, itf);

      if (opcode == INVOKEVIRTUAL && "hashCode".equals(name) && "java/lang/String".equals(owner) && ignoreUntilNextSwitch == 0) {
         ignoreUntilNextSwitch = 1;
      }
   }

   @Override
   public void visitLdcInsn(@Nonnull Object cst) {
      foundInterestingInstruction = true;
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitLdcInsn(cst);
   }

   @Override
   public void visitIincInsn(@Nonnegative int varIndex, int increment) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitIincInsn(varIndex, increment);
   }

   @Override
   public void visitTryCatchBlock(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitTryCatchBlock(start, end, handler, type);
   }

   @Override
   public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {
      if (ignoreUntilNextSwitch == 1) {
         ignoreUntilNextSwitch = 2;
      }

      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitLookupSwitchInsn(dflt, keys, labels);
   }

   @Override
   public void visitTableSwitchInsn(@Nonnegative int min, @Nonnegative int max, @Nonnull Label dflt, @Nonnull Label... labels) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {
      generateCallToRegisterBranchTargetExecutionIfPending();
      mw.visitMultiANewArrayInsn(desc, dims);
   }

   @Override
   public void visitMaxStack(@Nonnegative int maxStack) {
      if (maxStack > 1) {
         lineCoverageInfo.markLineAsReachable(currentLine);
      }

      mw.visitMaxStack(maxStack);
   }
}