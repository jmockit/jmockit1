/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.util.*;
import javax.annotation.*;

import mockit.asm.annotations.*;
import mockit.asm.classes.*;
import mockit.asm.controlFlow.*;
import mockit.asm.fields.*;
import mockit.asm.methods.*;
import mockit.coverage.data.*;
import mockit.coverage.lines.*;
import mockit.internal.*;
import static mockit.asm.jvmConstants.Access.*;
import static mockit.asm.jvmConstants.Opcodes.*;

final class CoverageModifier extends WrappingClassVisitor
{
   private static final Map<String, CoverageModifier> INNER_CLASS_MODIFIERS = new HashMap<>();
   private static final int FIELD_MODIFIERS_TO_IGNORE = FINAL + SYNTHETIC;

   @Nullable
   static byte[] recoverModifiedByteCodeIfAvailable(@Nonnull String innerClassName) {
      CoverageModifier modifier = INNER_CLASS_MODIFIERS.remove(innerClassName);
      return modifier == null ? null : modifier.toByteArray();
   }

   @Nullable private String internalClassName;
   @Nullable private String simpleClassName;
   @Nonnull private String sourceFileName;
   @Nullable private FileCoverageData fileData;
   private final boolean forInnerClass;
   private boolean forEnumClass;
   @Nullable private String kindOfTopLevelType;
   private int currentLine;

   CoverageModifier(@Nonnull ClassReader cr) { this(cr, false); }

   private CoverageModifier(@Nonnull ClassReader cr, boolean forInnerClass) {
      super(new ClassWriter(cr));
      sourceFileName = "";
      this.forInnerClass = forInnerClass;
   }

   private CoverageModifier(@Nonnull ClassReader cr, @Nonnull CoverageModifier other, @Nullable String simpleClassName) {
      this(cr, true);
      sourceFileName = other.sourceFileName;
      fileData = other.fileData;
      internalClassName = other.internalClassName;
      this.simpleClassName = simpleClassName;
   }

   @Override
   public void visit(int version, int access, @Nonnull String name, @Nonnull ClassInfo additionalInfo) {
      if ((access & SYNTHETIC) != 0) {
         throw new VisitInterruptedException();
      }

      boolean nestedType = name.indexOf('$') > 0;

      if (!nestedType && kindOfTopLevelType == null) {
         //noinspection ConstantConditions
         kindOfTopLevelType = getKindOfJavaType(access, additionalInfo.superName);
      }

      forEnumClass = (access & ENUM) != 0;

      String sourceFileDebugName = getSourceFileDebugName(additionalInfo);

      if (!forInnerClass) {
         extractClassAndSourceFileName(name);

         boolean cannotModify = (access & ANNOTATION) != 0;

         if (cannotModify) {
            throw VisitInterruptedException.INSTANCE;
         }

         registerAsInnerClassModifierIfApplicable(access, name, nestedType);
         createFileData(sourceFileDebugName);
      }

      cw.visit(version, access, name, additionalInfo);
   }

   @Nonnull
   private static String getKindOfJavaType(int typeModifiers, @Nonnull String superName) {
      if ((typeModifiers & ANNOTATION) != 0) return "annotation";
      if ((typeModifiers & INTERFACE) != 0) return "interface";
      if ((typeModifiers & ENUM) != 0) return "enum";
      if ((typeModifiers & ABSTRACT) != 0) return "abstractClass";
      if (superName.endsWith("Exception") || superName.endsWith("Error")) return "exception";
      return "class";
   }

   @Nonnull
   private static String getSourceFileDebugName(@Nonnull ClassInfo additionalInfo) {
      String sourceFileDebugName = additionalInfo.sourceFileName;

      if (sourceFileDebugName == null || !sourceFileDebugName.endsWith(".java")) {
         throw VisitInterruptedException.INSTANCE;
      }

      return sourceFileDebugName;
   }

   private void extractClassAndSourceFileName(@Nonnull String className) {
      internalClassName = className;
      int p = className.lastIndexOf('/');

      if (p < 0) {
         simpleClassName = className;
         sourceFileName = "";
      }
      else {
         simpleClassName = className.substring(p + 1);
         sourceFileName = className.substring(0, p + 1);
      }
   }

   private void registerAsInnerClassModifierIfApplicable(int access, @Nonnull String name, boolean nestedType) {
      if (!forEnumClass && (access & SUPER) != 0 && nestedType) {
         INNER_CLASS_MODIFIERS.put(name.replace('/', '.'), this);
      }
   }

   private void createFileData(@Nonnull String sourceFileDebugName) {
      sourceFileName += sourceFileDebugName;
      fileData = CoverageData.instance().getOrAddFile(sourceFileName, kindOfTopLevelType);
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      cw.visitInnerClass(name, outerName, innerName, access);

      if (forInnerClass || isSyntheticOrEnumClass(access) || !isNestedInsideClassBeingModified(name, outerName)) {
         return;
      }

      String innerClassName = name.replace('/', '.');

      if (INNER_CLASS_MODIFIERS.containsKey(innerClassName)) {
         return;
      }

      ClassReader innerCR = ClassFile.createClassReader(CoverageModifier.class.getClassLoader(), name);

      if (innerCR != null) {
         CoverageModifier innerClassModifier = new CoverageModifier(innerCR, this, innerName);
         innerCR.accept(innerClassModifier);
         INNER_CLASS_MODIFIERS.put(innerClassName, innerClassModifier);
      }
   }

   private static boolean isSyntheticOrEnumClass(int access) {
      return (access & SYNTHETIC) != 0 || access == STATIC + ENUM;
   }

   private boolean isNestedInsideClassBeingModified(@Nonnull String internalName, @Nullable String outerName) {
      String className = outerName == null ? internalName : outerName;
      int p = className.indexOf('$');
      String outerClassName = p < 0 ? className : className.substring(0, p);

      return outerClassName.equals(internalClassName);
   }

   @Override
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) {
      if (fileData != null && simpleClassName != null && (access & FIELD_MODIFIERS_TO_IGNORE) == 0) {
         fileData.dataCoverageInfo.addField(simpleClassName, name, (access & STATIC) != 0);
      }

      return cw.visitField(access, name, desc, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);

      if (fileData == null || (access & SYNTHETIC) != 0) {
         return mw;
      }

      if (name.charAt(0) == '<') {
         if (name.charAt(1) == 'c') {
            return forEnumClass ? mw : new StaticBlockModifier(mw);
         }

         return new ConstructorModifier(mw);
      }

      return new MethodModifier(mw);
   }

   private class BaseMethodModifier extends WrappingMethodVisitor {
      static final String DATA_RECORDING_CLASS = "mockit/coverage/TestRun";

      @Nonnull protected final List<Label> visitedLabels;
      @Nonnull private final List<Label> jumpTargetsForCurrentLine;
      @Nonnull private final List<Integer> pendingBranches;
      @Nonnull private final PerFileLineCoverage lineCoverageInfo;
      private int lineExpectingInstructionAfterJump;
      boolean assertFoundInCurrentLine;
      boolean ignoreUntilNextLabel;
      @SuppressWarnings("unused") private boolean foundPotentialAssertFalse;
      private int foundPotentialBooleanExpressionValue;
      int ignoreUntilNextSwitch;

      BaseMethodModifier(@Nonnull MethodWriter mw) {
         super(mw);
         visitedLabels = new ArrayList<>();
         jumpTargetsForCurrentLine = new ArrayList<>(4);
         pendingBranches = new ArrayList<>(6);

         assert fileData != null;
         lineCoverageInfo = fileData.getLineCoverageData();
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
         assert fileData != null;
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
      public void visitJumpInsn(int opcode, @Nonnull Label label) {
         if (
            currentLine == 0 || ignoreUntilNextLabel || ignoreUntilNextSwitch > 0 ||
            visitedLabels.contains(label) || !isConditionalJump(opcode)
         ) {
            assertFoundInCurrentLine = false;
            mw.visitJumpInsn(opcode, label);

            if (opcode == GOTO && foundPotentialBooleanExpressionValue == 1) {
               foundPotentialBooleanExpressionValue = 2;
            }

            return;
         }

         Label jumpingFrom = mw.getCurrentBlock();
         assert jumpingFrom != null;
         jumpingFrom.info = currentLine;

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

      final boolean isConditionalJump(int opcode) { return opcode != GOTO; }

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
         assert fileData != null;
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
            label.info = label.line > 0 ? label.line : currentLine;
            int targetBranchIndex = 2 * jumpTargetIndex + 1;
            pendingBranches.add(targetBranchIndex);
            assertFoundInCurrentLine = false;
         }

         foundPotentialBooleanExpressionValue = 0;
      }

      @Override
      public void visitInsn(int opcode) {
         if ((opcode == ICONST_0 || opcode == ICONST_1) && foundPotentialBooleanExpressionValue == 0) {
            generateCallToRegisterBranchTargetExecutionIfPending();
            foundPotentialBooleanExpressionValue = 1;
         }
         else {
            generateCallToRegisterBranchTargetExecutionIfPending();
         }

         mw.visitInsn(opcode);
      }

      @Override
      public void visitIntInsn(int opcode, int operand) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitIntInsn(opcode, operand);
      }

      @Override
      public void visitVarInsn(int opcode, @Nonnegative int varIndex) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitVarInsn(opcode, varIndex);
      }

      @Override
      public void visitTypeInsn(int opcode, @Nonnull String typeDesc) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitTypeInsn(opcode, typeDesc);
      }

      @Override
      public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitFieldInsn(opcode, owner, name, desc);

         if (opcode == GETSTATIC && "$assertionsDisabled".equals(name)) {
            assertFoundInCurrentLine = true;
            ignoreUntilNextLabel = true;
         }

         foundPotentialAssertFalse = true;
      }

      @Override
      public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitMethodInsn(opcode, owner, name, desc, itf);

         if (opcode == INVOKEVIRTUAL && "hashCode".equals(name) && "java/lang/String".equals(owner) && ignoreUntilNextSwitch == 0) {
            ignoreUntilNextSwitch = 1;
         }
      }

      @Override
      public void visitLdcInsn(@Nonnull Object cst) {
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
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitLookupSwitchInsn(dflt, keys, labels);
      }

      @Override
      public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitTableSwitchInsn(min, max, dflt, labels);
      }

      @Override
      public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitMultiANewArrayInsn(desc, dims);
      }
   }

   private class MethodOrConstructorModifier extends BaseMethodModifier {
      MethodOrConstructorModifier(@Nonnull MethodWriter mw) { super(mw); }

      @Override
      public final void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
         // TODO: need to also process field instructions inside accessor methods (STATIC + SYNTHETIC, "access$nnn")
         boolean getField = opcode == GETSTATIC || opcode == GETFIELD;
         boolean isStatic = opcode == PUTSTATIC || opcode == GETSTATIC;
         char fieldType = desc.charAt(0);
         boolean size2 = fieldType == 'J' || fieldType == 'D';
         String classAndFieldNames = null;
         boolean fieldHasData = false;

         if (!owner.startsWith("java/")) {
            classAndFieldNames = owner.substring(owner.lastIndexOf('/') + 1) + '.' + name;
            assert fileData != null;
            fieldHasData = fileData.dataCoverageInfo.isFieldWithCoverageData(classAndFieldNames);

            if (fieldHasData && !isStatic) {
               generateCodeToSaveInstanceReferenceOnTheStack(getField, size2);
            }
         }

         super.visitFieldInsn(opcode, owner, name, desc);

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

      private void generateCallToRegisterFieldCoverage(
         boolean getField, boolean isStatic, boolean size2, @Nonnull String classAndFieldNames
      ) {
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
      public final void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {
         if (ignoreUntilNextSwitch == 1) {
            ignoreUntilNextSwitch = 2;
         }

         super.visitLookupSwitchInsn(dflt, keys, labels);
      }
   }

   private final class MethodModifier extends MethodOrConstructorModifier {
      MethodModifier(@Nonnull MethodWriter mw) { super(mw); }

      @Override
      public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
         boolean isTestMethod = desc.startsWith("Lorg/junit/") || desc.startsWith("Lorg/testng/");

         if (isTestMethod) {
            throw VisitInterruptedException.INSTANCE;
         }

         return mw.visitAnnotation(desc);
      }
   }

   private final class ConstructorModifier extends MethodOrConstructorModifier {
      ConstructorModifier(@Nonnull MethodWriter mw) { super(mw); }
   }

   private final class StaticBlockModifier extends BaseMethodModifier {
      StaticBlockModifier(@Nonnull MethodWriter mw) { super(mw); }

      @Override
      public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
         // This is to ignore bytecode belonging to a static initialization block inserted in a regular line of code by the Java
         // compiler when the class contains at least one "assert" statement.
         // Otherwise, that line of code would always appear as partially covered when running with assertions enabled.
         if (opcode == INVOKEVIRTUAL && "java/lang/Class".equals(owner) && "desiredAssertionStatus".equals(name)) {
            assertFoundInCurrentLine = true;
            ignoreUntilNextLabel = true;
         }

         super.visitMethodInsn(opcode, owner, name, desc, itf);
      }
   }
}