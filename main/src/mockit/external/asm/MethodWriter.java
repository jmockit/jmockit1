/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Opcodes.*;

/**
 * A {@link MethodVisitor} that generates methods in bytecode form. Each visit method of this class appends the bytecode
 * corresponding to the visited instruction to a byte vector, in the order these methods are called.
 */
public final class MethodWriter extends MethodVisitor
{
   /**
    * The class writer to which this method must be added.
    */
   @Nonnull final ClassWriter cw;

   /**
    * The index of the constant pool item that contains the name of this method.
    */
   private final int name;

   /**
    * The index of the constant pool item that contains the descriptor of this method.
    */
   private final int desc;

   /**
    * The descriptor of this method.
    */
   @Nonnull final String descriptor;

   @Nullable private final SignatureWriter signatureWriter;

   /**
    * If not zero, indicates that the code of this method must be copied from the ClassReader associated to this writer
    * in <code>cw.cr</code>. More precisely, this field gives the index of the first byte to copied from
    * <code>cw.cr.code</code>.
    */
   @Nonnegative int classReaderOffset;

   /**
    * If not zero, indicates that the code of this method must be copied from the ClassReader associated to this writer
    * in <code>cw.cr</code>. More precisely, this field gives the number of bytes to copied from <code>cw.cr.code</code>.
    */
   @Nonnegative int classReaderLength;

   @Nullable private final ThrowsClause throwsClause;

   /**
    * The runtime visible parameter annotations of this method. May be <tt>null</tt>.
    */
   @Nullable private AnnotationWriter[] parameterAnnotations;

   /**
    * The bytecode of this method.
    */
   @Nonnull private final ByteVector code;

   @Nonnull private final FrameAndStackComputation frameAndStack;
   @Nonnull private final ExceptionHandling exceptionHandling;
   @Nonnull private final LocalVariables localVariables;
   @Nonnull private final LineNumberWriter lineNumbers;
   @Nonnull private final CFGAnalysis cfgAnalysis;

   private final boolean computeFrames;

   /**
    * Initializes the MethodWriter.
    *
    * @param cw            the class writer in which the method must be added.
    * @param access        the method's access flags (see {@link Opcodes}).
    * @param name          the method's name.
    * @param desc          the method's descriptor (see {@link JavaType}).
    * @param signature     the method's signature. May be <tt>null</tt>.
    * @param exceptions    the internal names of the method's exceptions. May be <tt>null</tt>.
    * @param computeFrames <tt>true</tt> if the stack map tables must be recomputed from scratch.
    */
   MethodWriter(
      @Nonnull ClassWriter cw, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature,
      @Nullable String[] exceptions, boolean computeFrames
   ) {
      this.cw = cw;
      cp = cw.cp;
      this.access = "<init>".equals(name) ? (access | Access.CONSTRUCTOR) : access;
      this.name = cp.newUTF8(name);
      this.desc = cp.newUTF8(desc);
      descriptor = desc;
      signatureWriter = signature == null ? null : new SignatureWriter(cp, signature);
      throwsClause = exceptions == null ? null : new ThrowsClause(cp, exceptions);
      code = new ByteVector();
      this.computeFrames = computeFrames;
      frameAndStack = new FrameAndStackComputation(this, access, desc);
      exceptionHandling = new ExceptionHandling(cp);
      localVariables = new LocalVariables(cp);
      lineNumbers = new LineNumberWriter(cp);
      cfgAnalysis = new CFGAnalysis(cw, code, computeFrames);

      createMarkerAttributes(cw.version);
   }

   @Nonnull @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return addAnnotation(desc);
   }

   @Nonnull @Override
   public AnnotationVisitor visitParameterAnnotation(@Nonnegative int parameter, @Nonnull String desc) {
      AnnotationWriter aw = new AnnotationWriter(cp, desc);

      if (parameterAnnotations == null) {
         int numParameters = JavaType.getArgumentTypes(descriptor).length;
         parameterAnnotations = new AnnotationWriter[numParameters];
      }

      aw.next = parameterAnnotations[parameter];
      parameterAnnotations[parameter] = aw;

      return aw;
   }

   @Override
   public void visitInsn(int opcode) {
      // Adds the instruction to the bytecode of the method.
      code.putByte(opcode);

      cfgAnalysis.updateCurrentBlockForZeroOperandInstruction(opcode);
   }

   @Override
   public void visitIntInsn(int opcode, int operand) {
      cfgAnalysis.updateCurrentBlockForSingleIntOperandInstruction(opcode, operand);

      // Adds the instruction to the bytecode of the method.
      if (opcode == SIPUSH) {
         code.put12(opcode, operand);
      }
      else { // BIPUSH or NEWARRAY
         code.put11(opcode, operand);
      }
   }

   @Override
   public void visitVarInsn(int opcode, @Nonnegative int var) {
      cfgAnalysis.updateCurrentBlockForLocalVariableInstruction(opcode, var);

      updateMaxLocals(opcode, var);

      // Adds the instruction to the bytecode of the method.
      if (var < 4) {
         int opt;

         if (opcode < ISTORE) { // ILOAD_0
            opt = 26 + ((opcode - ILOAD) << 2) + var;
         }
         else { // ISTORE_0
            opt = 59 + ((opcode - ISTORE) << 2) + var;
         }

         code.putByte(opt);
      }
      else if (var >= 256) {
         code.putByte(WIDE).put12(opcode, var);
      }
      else {
         code.put11(opcode, var);
      }

      if (opcode >= ISTORE && computeFrames && exceptionHandling.hasHandlers()) {
         visitLabel(new Label());
      }
   }

   private void updateMaxLocals(int opcode, @Nonnegative int var) {
      int n = opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE ? var + 2 : var + 1;
      frameAndStack.updateMaxLocals(n);
   }

   @Override
   public void visitTypeInsn(int opcode, @Nonnull String type) {
      StringItem typeItem = cp.newClassItem(type);
      cfgAnalysis.updateCurrentBlockForTypeInstruction(opcode, typeItem);

      // Adds the instruction to the bytecode of the method.
      code.put12(opcode, typeItem.index);
   }

   @Override
   public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      ClassMemberItem fieldItem = cp.newFieldItem(owner, name, desc);
      cfgAnalysis.updateCurrentBlockForFieldInstruction(opcode, fieldItem, desc);

      // Adds the instruction to the bytecode of the method.
      code.put12(opcode, fieldItem.index);
   }

   @Override
   public void visitMethodInsn(
      int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf
   ) {
      ClassMemberItem invokeItem = cp.newMethodItem(owner, name, desc, itf);
      cfgAnalysis.updateCurrentBlockForInvokeInstruction(invokeItem, opcode, desc);

      // Adds the instruction to the bytecode of the method.
      code.put12(opcode, invokeItem.index);

      if (opcode == INVOKEINTERFACE) {
         int argSize = invokeItem.getArgSizeComputingIfNeeded(desc);
         code.put11(argSize >> 2, 0);
      }
   }

   @Override
   public void visitInvokeDynamicInsn(
      @Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs
   ) {
      //noinspection ConstantConditions
      InvokeDynamicItem invokeItem = cw.bootstrapMethods.addInvokeDynamicReference(name, desc, bsm, bsmArgs);
      cfgAnalysis.updateCurrentBlockForInvokeInstruction(invokeItem, INVOKEDYNAMIC, desc);

      // Adds the instruction to the bytecode of the method.
      code.put12(INVOKEDYNAMIC, invokeItem.index);
      code.putShort(0);
   }

   @Override
   public void visitJumpInsn(int opcode, @Nonnull Label label) {
      Label nextInsn = cfgAnalysis.updateCurrentBlockForJumpInstruction(opcode, label);

      // Adds the instruction to the bytecode of the method.
      if (label.isResolved() && label.position - code.length < Short.MIN_VALUE) {
         // Case of a backward jump with an offset < -32768. In this case we automatically replace GOTO with GOTO_W and
         // IFxxx <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the "opposite" opcode of IFxxx
         // (i.e., IFNE for IFEQ) and where <l'> designates the instruction just after the GOTO_W.
         if (opcode == GOTO) {
            code.putByte(GOTO_W);
         }
         else {
            // If the IF instruction is transformed into IFNOT GOTO_W the next instruction becomes the target of the
            // IFNOT instruction.
            if (nextInsn != null) {
               nextInsn.markAsTarget();
            }

            code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1 : opcode ^ 1);
            code.putShort(8); // jump offset
            code.putByte(GOTO_W);
         }

         label.put(code, code.length - 1, true);
      }
      else {
         // Case of a backward jump with an offset >= -32768, or of a forward jump with, of course, an unknown offset.
         // In these cases we store the offset in 2 bytes (which will be increased in resizeInstructions, if needed).
         code.putByte(opcode);
         label.put(code, code.length - 1, false);
      }

      cfgAnalysis.updateCurrentBlockForJumpTarget(opcode, nextInsn);
   }

   @Override
   public void visitLabel(@Nonnull Label label) {
      cfgAnalysis.updateCurrentBlockForLabelBeforeNextInstruction(label);
   }

   @Override
   public void visitLdcInsn(@Nonnull Object cst) {
      Item constItem = cp.newConstItem(cst);
      cfgAnalysis.updateCurrentBlockForLDCInstruction(constItem);

      // Adds the instruction to the bytecode of the method.
      int index = constItem.index;

      if (constItem instanceof LongValueItem) {
         code.put12(LDC2_W, index);
      }
      else if (index >= 256) {
         code.put12(LDC_W, index);
      }
      else {
         code.put11(LDC, index);
      }
   }

   @Override
   public void visitIincInsn(int var, int increment) {
      cfgAnalysis.updateCurrentBlockForIINCInstruction(var);

      // Updates max locals.
      int n = var + 1;
      frameAndStack.updateMaxLocals(n);

      // Adds the instruction to the bytecode of the method.
      if (var > 255 || increment > 127 || increment < -128) {
         code.putByte(WIDE).put12(IINC, var).putShort(increment);
      }
      else {
         code.putByte(IINC).put11(var, increment);
      }
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {
      // Adds the instruction to the bytecode of the method.
      int source = code.length;
      code.putByte(TABLESWITCH);
      code.increaseLengthBy((4 - code.length % 4) % 4);
      dflt.put(code, source, true);
      code.putInt(min).putInt(max);

      for (Label label : labels) {
         label.put(code, source, true);
      }

      cfgAnalysis.updateCurrentBlockForSwitchInstruction(dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {
      // Adds the instruction to the bytecode of the method.
      int source = code.length;
      code.putByte(LOOKUPSWITCH);
      code.increaseLengthBy((4 - code.length % 4) % 4);
      dflt.put(code, source, true);
      code.putInt(labels.length);

      for (int i = 0; i < labels.length; i++) {
         code.putInt(keys[i]);
         labels[i].put(code, source, true);
      }

      cfgAnalysis.updateCurrentBlockForSwitchInstruction(dflt, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {
      StringItem arrayTypeItem = cp.newClassItem(desc);
      cfgAnalysis.updateCurrentBlockForMULTIANEWARRAYInstruction(arrayTypeItem, dims);

      // Adds the instruction to the bytecode of the method.
      code.put12(MULTIANEWARRAY, arrayTypeItem.index).putByte(dims);
   }

   @Override
   public void visitTryCatchBlock(
      @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type
   ) {
      exceptionHandling.addHandler(start, end, handler, type);
   }

   @Override
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      int localsCount = localVariables.addLocalVariable(name, desc, signature, start, end, index);
      frameAndStack.updateMaxLocals(localsCount);
   }

   @Override
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {
      lineNumbers.addLineNumber(line, start);
   }

   @Override
   public void visitMaxStack(@Nonnegative int maxStack) {
      int computedMaxStack;

      if (computeFrames) {
         exceptionHandling.completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames();

         Frame firstFrame = cfgAnalysis.getFirstFrame();
         frameAndStack.createAndVisitFirstFrame(firstFrame);

         computedMaxStack = cfgAnalysis.computeMaxStackSizeFromComputedFrames();
         computedMaxStack = visitAllFramesToBeStoredInStackMap(computedMaxStack);

         exceptionHandling.countNumberOfHandlers();
      }
      else {
         // TODO: figure out if/when the next call is needed, since no tests fail if commented out
         exceptionHandling.completeControlFlowGraphWithExceptionHandlerBlocks();

         computedMaxStack = cfgAnalysis.computeMaxStackSize();
         computedMaxStack = Math.max(maxStack, computedMaxStack);
      }

      frameAndStack.setMaxStack(computedMaxStack);
   }

   @Nonnegative
   private int visitAllFramesToBeStoredInStackMap(@Nonnegative int max) {
      Label label = cfgAnalysis.getLabelForFirstBasicBlock();
      Frame frame;

      while (label != null) {
         frame = label.frame;

         if (label.isStoringFrame()) {
            frameAndStack.visitFrame(frame);
         }

         if (!label.isReachable()) {
            // Finds start and end of dead basic block.
            Label k = label.successor;
            int start = label.position;
            int end = (k == null ? code.length : k.position) - 1;

            // If non empty basic block.
            if (end >= start) {
               max = Math.max(max, 1);

               replaceInstructionsWithNOPAndATHROW(start, end);

               frameAndStack.emitFrameForUnreachableBlock(start);
               exceptionHandling.removeStartEndRange(label, k);
            }
         }

         label = label.successor;
      }

      return max;
   }

   // Replaces instructions with NOP ... NOP ATHROW.
   private void replaceInstructionsWithNOPAndATHROW(@Nonnegative int start, @Nonnegative int end) {
      byte[] data = code.data;

      for (int i = start; i < end; i++) {
         data[i] = NOP;
      }

      data[end] = (byte) ATHROW;
   }

   /**
    * Returns the size of the bytecode of this method.
    */
   @Nonnegative
   int getSize() {
      if (classReaderOffset > 0) {
         return 6 + classReaderLength;
      }

      int size = 8 + getMarkerAttributesSize() + getAnnotationsSize() + getParameterAnnotationsSize();
      int codeLength = code.length;

      if (codeLength > 0) {
         if (codeLength > 65536) {
            throw new RuntimeException("Method code too large!");
         }

         cp.newUTF8("Code");

         size += 18 + codeLength + exceptionHandling.getSize();
         size += localVariables.getSizeWhileAddingConstantPoolItems();
         size += lineNumbers.getSize();
         size += frameAndStack.getSizeWhileAddingConstantPoolItem();
      }

      if (throwsClause != null) {
         size += throwsClause.getSize();
      }

      if (signatureWriter != null) {
         size += signatureWriter.getSize();
      }

      return size;
   }

   @Nonnegative
   private int getParameterAnnotationsSize() {
      int size = 0;

      if (parameterAnnotations != null) {
         cp.newUTF8("RuntimeVisibleParameterAnnotations");

         int n = parameterAnnotations.length;
         size += 7 + 2 * n;

         for (int i = n - 1; i >= 0; i--) {
            AnnotationWriter parameterAnnotation = parameterAnnotations[i];
            size += parameterAnnotation == null ? 0 : parameterAnnotation.getSize();
         }
      }

      return size;
   }

   /**
    * Puts the bytecode of this method in the given byte vector.
    */
   @Override
   void put(@Nonnull ByteVector out) {
      putAccess(out, Access.CONSTRUCTOR);
      out.putShort(name);
      out.putShort(desc);

      if (classReaderOffset > 0) {
         out.putByteArray(cw.cr.code, classReaderOffset, classReaderLength);
         return;
      }

      putMethodAttributeCount(out);
      putMethodCode(out);

      if (throwsClause != null) {
         throwsClause.put(out);
      }

      putMarkerAttributes(out);

      if (signatureWriter != null) {
         signatureWriter.put(out);
      }

      putAnnotationAttributes(out);
   }

   private void putMethodAttributeCount(@Nonnull ByteVector out) {
      int attributeCount = getMarkerAttributeCount();

      if (code.length > 0) {
         attributeCount++;
      }

      if (throwsClause != null) {
         attributeCount++;
      }

      if (signatureWriter != null) {
         attributeCount++;
      }

      if (annotations != null) {
         attributeCount++;
      }

      if (parameterAnnotations != null) {
         attributeCount++;
      }

      out.putShort(attributeCount);
   }

   private void putMethodCode(@Nonnull ByteVector out) {
      if (code.length > 0) {
         putCodeSize(out);
         frameAndStack.putMaxStackAndLocals(out);
         out.putInt(code.length).putByteVector(code);
         exceptionHandling.put(out);

         int codeAttributeCount = localVariables.getAttributeCount();

         if (lineNumbers.hasLineNumbers()) {
            codeAttributeCount++;
         }

         if (frameAndStack.hasStackMap()) {
            codeAttributeCount++;
         }

         out.putShort(codeAttributeCount);
         localVariables.put(out);
         lineNumbers.put(out);
         frameAndStack.put(out);
      }
   }

   private void putCodeSize(@Nonnull ByteVector out) {
      int size =
         12 + code.length +
         exceptionHandling.getSize() + localVariables.getSize() + lineNumbers.getSize() + frameAndStack.getSize();

      out.putShort(cp.newUTF8("Code")).putInt(size);
   }

   private void putAnnotationAttributes(@Nonnull ByteVector out) {
      putAnnotations(out);

      if (parameterAnnotations != null) {
         out.putShort(cp.newUTF8("RuntimeVisibleParameterAnnotations"));
         AnnotationWriter.put(out, parameterAnnotations);
      }
   }

   @Nullable
   public Label getCurrentBlock() { return cfgAnalysis.getLabelForCurrentBasicBlock(); }
}
