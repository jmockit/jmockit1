/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.util.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.lines.*;
import mockit.coverage.paths.*;
import mockit.external.asm.*;
import mockit.internal.*;
import static mockit.coverage.Metrics.*;
import static mockit.external.asm.Opcodes.*;

final class CoverageModifier extends WrappingClassVisitor
{
   private static final Map<String, CoverageModifier> INNER_CLASS_MODIFIERS = new HashMap<String, CoverageModifier>();
   private static final int FIELD_MODIFIERS_TO_IGNORE = Access.FINAL + Access.SYNTHETIC;
   private static final int MAX_CONDITIONS = Integer.getInteger("jmockit-coverage-maxConditions", 10);
   private static final boolean WITH_PATH_OR_DATA_COVERAGE = PathCoverage.active || DataCoverage.active;

   @Nullable
   static byte[] recoverModifiedByteCodeIfAvailable(@Nonnull String innerClassName)
   {
      CoverageModifier modifier = INNER_CLASS_MODIFIERS.remove(innerClassName);
      return modifier == null ? null : modifier.toByteArray();
   }

   @Nullable
   static ClassReader createClassReader(@Nonnull Class<?> aClass)
   {
      return ClassFile.createClassReader(aClass.getClassLoader(), aClass.getName().replace('.', '/'));
   }

   @Nullable private String internalClassName;
   @Nullable private String simpleClassName;
   @Nonnull private String sourceFileName;
   @Nullable private FileCoverageData fileData;
   private boolean cannotModify;
   private final boolean forInnerClass;
   private boolean forEnumClass;
   @Nullable private String kindOfTopLevelType;
   private int currentLine;

   CoverageModifier(@Nonnull ClassReader cr) { this(cr, false); }

   private CoverageModifier(@Nonnull ClassReader cr, boolean forInnerClass)
   {
      super(new ClassWriter(cr));
      sourceFileName = "";
      this.forInnerClass = forInnerClass;
   }

   private CoverageModifier(@Nonnull ClassReader cr, @Nonnull CoverageModifier other, @Nullable String simpleClassName)
   {
      this(cr, true);
      sourceFileName = other.sourceFileName;
      fileData = other.fileData;
      internalClassName = other.internalClassName;
      this.simpleClassName = simpleClassName;
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, String superName,
      @Nullable String[] interfaces)
   {
      if ((access & Access.SYNTHETIC) != 0) {
         throw new VisitInterruptedException();
      }

      boolean nestedType = name.indexOf('$') > 0;

      if (!nestedType && kindOfTopLevelType == null) {
         kindOfTopLevelType = getKindOfJavaType(access, superName);
      }

      forEnumClass = (access & Access.ENUM) != 0;

      if (!forInnerClass) {
         internalClassName = name;
         int p = name.lastIndexOf('/');

         if (p < 0) {
            simpleClassName = name;
            sourceFileName = "";
         }
         else {
            simpleClassName = name.substring(p + 1);
            sourceFileName = name.substring(0, p + 1);
         }

         cannotModify = (access & Access.ANNOTATION) != 0;

         if (!forEnumClass && (access & Access.SUPER) != 0 && nestedType) {
            INNER_CLASS_MODIFIERS.put(name.replace('/', '.'), this);
         }
      }

      cw.visit(version, access, name, signature, superName, interfaces);
   }

   @Nonnull
   private static String getKindOfJavaType(int typeModifiers, @Nonnull String superName)
   {
      if ((typeModifiers & Access.ANNOTATION) != 0) return "annotation";
      if ((typeModifiers & Access.INTERFACE) != 0) return "interface";
      if ((typeModifiers & Access.ENUM) != 0) return "enum";
      if ((typeModifiers & Access.ABSTRACT) != 0) return "abstractClass";
      if (superName.endsWith("Exception") || superName.endsWith("Error")) return "exception";
      return "class";
   }

   @Override
   public void visitSource(@Nullable String file)
   {
      if (file == null || !file.endsWith(".java")) {
         throw VisitInterruptedException.INSTANCE;
      }

      if (!forInnerClass) {
         if (cannotModify) {
            throw VisitInterruptedException.INSTANCE;
         }

         sourceFileName += file;
         fileData = CoverageData.instance().getOrAddFile(sourceFileName, kindOfTopLevelType);
      }

      cw.visitSource(file);
   }

   @Override
   public void visitInnerClass(
      @Nonnull String internalName, @Nullable String outerName, @Nullable String innerName, int access)
   {
      cw.visitInnerClass(internalName, outerName, innerName, access);

      if (
         forInnerClass ||
         isSyntheticOrEnumClass(access) ||
         !isNestedInsideClassBeingModified(internalName, outerName)
      ) {
         return;
      }

      String innerClassName = internalName.replace('/', '.');

      if (INNER_CLASS_MODIFIERS.containsKey(innerClassName)) {
         return;
      }

      ClassReader innerCR = ClassFile.createClassReader(CoverageModifier.class.getClassLoader(), internalName);

      if (innerCR != null) {
         CoverageModifier innerClassModifier = new CoverageModifier(innerCR, this, innerName);
         innerCR.accept(innerClassModifier);
         INNER_CLASS_MODIFIERS.put(innerClassName, innerClassModifier);
      }
   }

   private static boolean isSyntheticOrEnumClass(int access)
   {
      return (access & Access.SYNTHETIC) != 0 || access == Access.STATIC + Access.ENUM;
   }

   private boolean isNestedInsideClassBeingModified(@Nonnull String internalName, @Nullable String outerName)
   {
      String className = outerName == null ? internalName : outerName;
      int p = className.indexOf('$');
      String outerClassName = p < 0 ? className : className.substring(0, p);

      return outerClassName.equals(internalClassName);
   }

   @Override
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value)
   {
      if (
         fileData != null && simpleClassName != null &&
         (access & FIELD_MODIFIERS_TO_IGNORE) == 0 && DataCoverage.active
      ) {
         fileData.dataCoverageInfo.addField(simpleClassName, name, (access & Access.STATIC) != 0);
      }

      return cw.visitField(access, name, desc, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);

      if (fileData == null || (access & Access.SYNTHETIC) != 0) {
         return mw;
      }

      if (name.charAt(0) == '<') {
         if (name.charAt(1) == 'c') {
            return forEnumClass ? mw : new StaticBlockModifier(mw);
         }

         if (WITH_PATH_OR_DATA_COVERAGE) {
            return new ConstructorModifier(mw);
         }
      }

      return WITH_PATH_OR_DATA_COVERAGE ? new MethodModifier(mw) : new BaseMethodModifier(mw);
   }

   private class BaseMethodModifier extends WrappingMethodVisitor
   {
      static final String DATA_RECORDING_CLASS = "mockit/coverage/TestRun";

      @Nonnull protected final List<Label> visitedLabels;
      @Nonnull private final List<Label> jumpTargetsForCurrentLine;
      @Nonnull private final List<Integer> pendingBranches;
      @Nonnull private final PerFileLineCoverage lineCoverageInfo;
      private int lineExpectingInstructionAfterJump;
      protected boolean assertFoundInCurrentLine;
      protected boolean ignoreUntilNextLabel;
      @SuppressWarnings("unused") private boolean foundPotentialAssertFalse;
      private int foundPotentialBooleanExpressionValue;
      protected int ignoreUntilNextSwitch;

      BaseMethodModifier(@Nonnull MethodWriter mw)
      {
         super(mw);
         visitedLabels = new ArrayList<Label>();
         jumpTargetsForCurrentLine = new ArrayList<Label>(4);
         pendingBranches = new ArrayList<Integer>(6);

         assert fileData != null;
         lineCoverageInfo = fileData.getLineCoverageData();
      }

      @Override
      public void visitLineNumber(int line, @Nonnull Label start)
      {
         if (!pendingBranches.isEmpty()) {
            pendingBranches.clear();
         }

         lineCoverageInfo.addLine(line);
         currentLine = line;

         jumpTargetsForCurrentLine.clear();

         generateCallToRegisterLineExecution();

         mw.visitLineNumber(line, start);
      }

      private void generateCallToRegisterLineExecution()
      {
         assert fileData != null;
         mw.visitIntInsn(SIPUSH, fileData.index);
         pushCurrentLineOnTheStack();
         mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, "lineExecuted", "(II)V", false);
      }

      private void pushCurrentLineOnTheStack()
      {
         if (currentLine <= Short.MAX_VALUE) {
            mw.visitIntInsn(SIPUSH, currentLine);
         }
         else {
            mw.visitLdcInsn(currentLine);
         }
      }

      @Override
      public void visitJumpInsn(int opcode, @Nonnull Label label)
      {
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

      protected final boolean isConditionalJump(int opcode) { return opcode != GOTO; }

      private void generateCallToRegisterBranchTargetExecutionIfPending()
      {
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

      private void generateCallToRegisterBranchTargetExecution(int branchIndex)
      {
         assert fileData != null;
         mw.visitIntInsn(SIPUSH, fileData.index);
         pushCurrentLineOnTheStack();
         mw.visitIntInsn(SIPUSH, branchIndex);
         mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, "branchExecuted", "(III)V", false);
      }

      @Override
      public void visitLabel(@Nonnull Label label)
      {
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
      public void visitInsn(int opcode)
      {
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
      public void visitIntInsn(int opcode, int operand)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitIntInsn(opcode, operand);
      }

      @Override
      public void visitVarInsn(int opcode, int varIndex)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitVarInsn(opcode, varIndex);
      }

      @Override
      public void visitTypeInsn(int opcode, @Nonnull String desc)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitTypeInsn(opcode, desc);
      }

      @Override
      public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitFieldInsn(opcode, owner, name, desc);

         if (opcode == GETSTATIC && "$assertionsDisabled".equals(name)) {
            assertFoundInCurrentLine = true;
            ignoreUntilNextLabel = true;
         }

         foundPotentialAssertFalse = true;
      }

      @Override
      public void visitMethodInsn(
         int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitMethodInsn(opcode, owner, name, desc, itf);

         if (
            opcode == INVOKEVIRTUAL && "hashCode".equals(name) && "java/lang/String".equals(owner) &&
            ignoreUntilNextSwitch == 0
         ) {
            ignoreUntilNextSwitch = 1;
         }
      }

      @Override
      public void visitLdcInsn(@Nonnull Object cst)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitLdcInsn(cst);
      }

      @Override
      public void visitIincInsn(int varIndex, int increment)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitIincInsn(varIndex, increment);
      }

      @Override
      public void visitTryCatchBlock(
         @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitTryCatchBlock(start, end, handler, type);
      }

      @Override
      public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitLookupSwitchInsn(dflt, keys, labels);
      }

      @Override
      public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitTableSwitchInsn(min, max, dflt, labels);
      }

      @Override
      public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims)
      {
         generateCallToRegisterBranchTargetExecutionIfPending();
         mw.visitMultiANewArrayInsn(desc, dims);
      }
   }

   private class MethodOrConstructorModifier extends BaseMethodModifier
   {
      @Nullable private NodeBuilder nodeBuilder;
      @Nullable private Label entryPoint;
      private int jumpCount;

      MethodOrConstructorModifier(@Nonnull MethodWriter mw)
      {
         super(mw);
         nodeBuilder = new NodeBuilder();
      }

      @Override
      public final void visitLabel(@Nonnull Label label)
      {
         if (nodeBuilder == null || ignoreUntilNextSwitch > 0) {
            super.visitLabel(label);
            return;
         }

         int line = label.line;

         if (entryPoint == null) {
            entryPoint = new Label();
            mw.visitLabel(entryPoint);
            mw.visitLineNumber(line, entryPoint);
            nodeBuilder.handleEntry(line);
            generateCallToRegisterNodeReached(0);
         }

         super.visitLabel(label);

         int newNodeIndex = nodeBuilder.handleJumpTarget(label, line > 0 ? line : currentLine);
         generateCallToRegisterNodeReached(newNodeIndex);
      }

      private void generateCallToRegisterNodeReached(int nodeIndex)
      {
         if (nodeIndex >= 0) {
            assert nodeBuilder != null;
            mw.visitLdcInsn(sourceFileName);
            mw.visitLdcInsn(nodeBuilder.firstLine);
            mw.visitIntInsn(SIPUSH, nodeIndex);
            mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, "nodeReached", "(Ljava/lang/String;II)V", false);
         }
      }

      @Override
      public final void visitJumpInsn(int opcode, @Nonnull Label label)
      {
         if (
            nodeBuilder == null || entryPoint == null || ignoreUntilNextSwitch > 0 ||
            visitedLabels.contains(label)
         ) {
            super.visitJumpInsn(opcode, label);
            return;
         }

         boolean conditional = isConditionalJump(opcode);

         if (conditional && ++jumpCount > MAX_CONDITIONS) {
            nodeBuilder = null;
         }
         else {
            int nodeIndex = nodeBuilder.handleJump(label, currentLine, conditional);
            generateCallToRegisterNodeReached(nodeIndex);
         }

         super.visitJumpInsn(opcode, label);
      }

      @Override
      public final void visitInsn(int opcode)
      {
         if (nodeBuilder != null) {
            if (opcode >= IRETURN && opcode <= RETURN || opcode == ATHROW) {
               int newNodeIndex = nodeBuilder.handleExit(currentLine);
               generateCallToRegisterNodeReached(newNodeIndex);
            }
            else {
               handleRegularInstruction(opcode);
            }
         }

         super.visitInsn(opcode);
      }

      private void handleRegularInstruction(int opcode)
      {
         if (nodeBuilder != null && ignoreUntilNextSwitch == 0) {
            int nodeIndex = nodeBuilder.handleRegularInstruction(currentLine, opcode);
            generateCallToRegisterNodeReached(nodeIndex);
         }
      }

      @Override
      public final void visitIntInsn(int opcode, int operand)
      {
         super.visitIntInsn(opcode, operand);
         handleRegularInstruction(opcode);
      }

      @Override
      public final void visitIincInsn(int varIndex, int increment)
      {
         super.visitIincInsn(varIndex, increment);
         handleRegularInstruction(IINC);
      }

      @Override
      public final void visitLdcInsn(@Nonnull Object cst)
      {
         super.visitLdcInsn(cst);
         handleRegularInstruction(LDC);
      }

      @Override
      public final void visitTypeInsn(int opcode, @Nonnull String desc)
      {
         super.visitTypeInsn(opcode, desc);
         handleRegularInstruction(opcode);
      }

      @Override
      public final void visitVarInsn(int opcode, int varIndex)
      {
         super.visitVarInsn(opcode, varIndex);
         handleRegularInstruction(opcode);
      }

      @Override
      public final void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc)
      {
         if (!DataCoverage.active) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
         }

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

         handleRegularInstruction(opcode);
      }

      private void generateCodeToSaveInstanceReferenceOnTheStack(boolean getField, boolean size2)
      {
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
         boolean getField, boolean isStatic, boolean size2, @Nonnull String classAndFieldNames)
      {
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
            isStatic ?
               "(Ljava/lang/String;Ljava/lang/String;)V" : "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

         mw.visitMethodInsn(INVOKESTATIC, DATA_RECORDING_CLASS, methodToCall, methodDesc, false);
      }

      @Override
      public final void visitMethodInsn(
         int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
      {
         super.visitMethodInsn(opcode, owner, name, desc, itf);
         handleRegularInstruction(opcode);
      }

      @Override
      public final void visitTryCatchBlock(
         @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type)
      {
         super.visitTryCatchBlock(start, end, handler, type);
         handleRegularInstruction(0);
      }

      @Override
      public final void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels)
      {
         if (ignoreUntilNextSwitch == 1) {
            ignoreUntilNextSwitch = 2;
         }
         else if (nodeBuilder != null) {
            int nodeIndex = nodeBuilder.handleForwardJumpsToNewTargets(dflt, labels, currentLine);
            generateCallToRegisterNodeReached(nodeIndex);
            ignoreUntilNextSwitch = 0;
         }

         super.visitLookupSwitchInsn(dflt, keys, labels);
      }

      @Override
      public final void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels)
      {
         if (nodeBuilder != null && ignoreUntilNextSwitch == 0) {
            int nodeIndex = nodeBuilder.handleForwardJumpsToNewTargets(dflt, labels, currentLine);
            generateCallToRegisterNodeReached(nodeIndex);
         }

         super.visitTableSwitchInsn(min, max, dflt, labels);
      }

      @Override
      public final void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims)
      {
         super.visitMultiANewArrayInsn(desc, dims);
         handleRegularInstruction(MULTIANEWARRAY);
      }

      @Override
      public final void visitEnd()
      {
         if (currentLine > 0 && nodeBuilder != null && nodeBuilder.hasNodes() && fileData != null) {
            MethodCoverageData methodData = new MethodCoverageData();
            methodData.buildPaths(currentLine, nodeBuilder);
            fileData.addMethod(methodData);
         }
      }
   }

   private final class MethodModifier extends MethodOrConstructorModifier
   {
      MethodModifier(@Nonnull MethodWriter mw) { super(mw); }

      @Override
      public AnnotationVisitor visitAnnotation(@Nonnull String desc)
      {
         boolean isTestMethod = desc.startsWith("Lorg/junit/") || desc.startsWith("Lorg/testng/");

         if (isTestMethod) {
            throw VisitInterruptedException.INSTANCE;
         }

         return mw.visitAnnotation(desc);
      }
   }

   private final class ConstructorModifier extends MethodOrConstructorModifier
   {
      ConstructorModifier(@Nonnull MethodWriter mw) { super(mw); }
   }

   private final class StaticBlockModifier extends BaseMethodModifier
   {
      StaticBlockModifier(@Nonnull MethodWriter mw) { super(mw); }

      @Override
      public void visitMethodInsn(
         int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
      {
         // This is to ignore bytecode belonging to a static initialization block inserted in a regular line of code by
         // the Java compiler when the class contains at least one "assert" statement. Otherwise, that line of code
         // would always appear as partially covered when running with assertions enabled.
         if (opcode == INVOKEVIRTUAL && "java/lang/Class".equals(owner) && "desiredAssertionStatus".equals(name)) {
            assertFoundInCurrentLine = true;
            ignoreUntilNextLabel = true;
         }

         super.visitMethodInsn(opcode, owner, name, desc, itf);
      }
   }
}
