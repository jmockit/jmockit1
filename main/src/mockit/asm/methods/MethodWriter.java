package mockit.asm.methods;

import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.annotations.*;
import mockit.asm.classes.*;
import mockit.asm.constantPool.*;
import mockit.asm.controlFlow.*;
import mockit.asm.exceptionHandling.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.types.*;
import mockit.asm.util.*;
import static mockit.asm.jvmConstants.Opcodes.*;

/**
 * A {@link MethodVisitor} that generates methods in bytecode form. Each visit method of this class appends the bytecode corresponding to
 * the visited instruction to a byte vector, in the order these methods are called.
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyFields", "OverlyComplexClass"})
public final class MethodWriter extends MethodVisitor
{
   /**
    * The class writer to which this method must be added.
    */
   @Nonnull public final ClassWriter cw;

   /**
    * The index of the constant pool item that contains the name of this method.
    */
   private final int nameItemIndex;

   /**
    * The index of the constant pool item that contains the descriptor of this method.
    */
   private final int descItemIndex;

   /**
    * The descriptor of this method.
    */
   @Nonnull private final String descriptor;

   @Nullable private final SignatureWriter signatureWriter;

   /**
    * If not zero, indicates that the code of this method must be copied from <code>cw.code</code>. More precisely, this field gives the
    * index of the first byte to be copied from <code>cw.code</code>.
    */
   @Nonnegative int classReaderOffset;

   /**
    * If not zero, indicates that the code of this method must be copied from <code>cw.cr</code>. More precisely, this field gives the
    * number of bytes to be copied from <code>cw.code</code>.
    */
   @Nonnegative int classReaderLength;

   @Nullable private final ExceptionsWriter exceptionsWriter;

   /**
    * The runtime visible parameter annotations of this method, if any.
    */
   @Nullable private AnnotationVisitor[] parameterAnnotations;

   /**
    * The bytecode of this method.
    */
   @Nonnull private final ByteVector code;

   @Nonnull private final ExceptionHandling exceptionHandling;
   @Nonnull private final StackMapTableWriter stackMapTableWriter;
   @Nonnull private final LocalVariableTableWriter localVariableTableWriter;
   @Nonnull private final LineNumberTableWriter lineNumberTableWriter;
   @Nonnull private final CFGAnalysis cfgAnalysis;

   private final boolean computeFrames;

   /**
    * Initializes this MethodWriter.
    *
    * @param cw            the class writer in which the method must be added
    * @param access        the method's access flags (see {@link Opcodes})
    * @param name          the method's name
    * @param desc          the method's descriptor (see {@link JavaType})
    * @param signature     the method's signature
    * @param exceptions    the internal names of the method's exceptions
    * @param computeFrames <tt>true</tt> if the stack map tables must be recomputed from scratch
    */
   public MethodWriter(
      @Nonnull ClassWriter cw, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature,
      @Nullable String[] exceptions, boolean computeFrames
   ) {
      super(cw.getConstantPoolGeneration(), "<init>".equals(name) ? access | Access.CONSTRUCTOR : access);
      this.cw = cw;
      nameItemIndex = cp.newUTF8(name);
      descItemIndex = cp.newUTF8(desc);
      descriptor = desc;
      signatureWriter = signature == null ? null : new SignatureWriter(cp, signature);
      exceptionsWriter = exceptions == null ? null : new ExceptionsWriter(cp, exceptions);
      code = new ByteVector();
      this.computeFrames = computeFrames;
      exceptionHandling = new ExceptionHandling(cp);
      stackMapTableWriter = new StackMapTableWriter(cp, cw.isJava6OrNewer(), access, desc);
      localVariableTableWriter = new LocalVariableTableWriter(cp);
      lineNumberTableWriter = new LineNumberTableWriter(cp);
      cfgAnalysis = new CFGAnalysis(cp, cw.getInternalClassName(), code, computeFrames);

      createMarkerAttributes(cw.getClassVersion());
   }

   @Nonnull @Override
   public AnnotationVisitor visitParameterAnnotation(@Nonnegative int parameter, @Nonnull String desc) {
      AnnotationVisitor aw = new AnnotationVisitor(cp, desc);

      if (parameterAnnotations == null) {
         int numParameters = JavaType.getArgumentTypes(descriptor).length;
         parameterAnnotations = new AnnotationVisitor[numParameters];
      }

      aw.setNext(parameterAnnotations[parameter]);
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
         code.put12(SIPUSH, operand);
      }
      else { // BIPUSH or NEWARRAY
         code.put11(opcode, operand);
      }
   }

   @Override
   public void visitVarInsn(int opcode, @Nonnegative int varIndex) {
      cfgAnalysis.updateCurrentBlockForLocalVariableInstruction(opcode, varIndex);

      updateMaxLocals(opcode, varIndex);

      // Adds the instruction to the bytecode of the method.
      if (varIndex < 4) {
         int opt;

         if (opcode < ISTORE) { // ILOAD_0
            opt = 26 + ((opcode - ILOAD) << 2) + varIndex;
         }
         else { // ISTORE_0
            opt = 59 + ((opcode - ISTORE) << 2) + varIndex;
         }

         code.putByte(opt);
      }
      else if (varIndex >= 256) {
         code.putByte(WIDE).put12(opcode, varIndex);
      }
      else {
         code.put11(opcode, varIndex);
      }

      if (opcode >= ISTORE && computeFrames && exceptionHandling.hasHandlers()) {
         visitLabel(new Label());
      }
   }

   private void updateMaxLocals(int opcode, @Nonnegative int varIndex) {
      int n = opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE ? varIndex + 2 : varIndex + 1;
      stackMapTableWriter.updateMaxLocals(n);
   }

   @Override
   public void visitTypeInsn(int opcode, @Nonnull String typeDesc) {
      StringItem typeItem = cp.newClassItem(typeDesc);
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
   public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
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
   public void visitInvokeDynamicInsn(@Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs) {
      DynamicItem invokeItem = cw.addInvokeDynamicReference(name, desc, bsm, bsmArgs);
      cfgAnalysis.updateCurrentBlockForInvokeInstruction(invokeItem, INVOKEDYNAMIC, desc);

      // Adds the instruction to the bytecode of the method.
      code.put12(INVOKEDYNAMIC, invokeItem.index);
      code.putShort(0);
   }

   @Override
   public void visitJumpInsn(int opcode, @Nonnull Label label) {
      Label nextInsn = cfgAnalysis.updateCurrentBlockForJumpInstruction(opcode, label);

      // Adds the instruction to the bytecode of the method.
      if (label.isResolved() && label.position - code.getLength() < Short.MIN_VALUE) {
         // Case of a backward jump with an offset < -32768. In this case we automatically replace GOTO with GOTO_W and IFxxx <l> with
         // IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the "opposite" opcode of IFxxx (i.e., IFNE for IFEQ) and where <l'> designates the
         // instruction just after the GOTO_W.
         if (opcode != GOTO) {
            // If the IF instruction is transformed into IFNOT GOTO_W the next instruction becomes the target of the IFNOT instruction.
            if (nextInsn != null) {
               nextInsn.markAsTarget();
            }

            code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1 : opcode ^ 1);
            code.putShort(8); // jump offset
         }

         code.putByte(GOTO_W);
         label.put(code, code.getLength() - 1, true);
      }
      else {
         // Case of a backward jump with an offset >= -32768, or of a forward jump with, of course, an unknown offset.
         // In these cases we store the offset in 2 bytes (which will be increased in resizeInstructions, if needed).
         code.putByte(opcode);
         label.put(code, code.getLength() - 1, false);
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
   public void visitIincInsn(@Nonnegative int varIndex, int increment) {
      cfgAnalysis.updateCurrentBlockForIINCInstruction(varIndex);

      // Updates max locals.
      int n = varIndex + 1;
      stackMapTableWriter.updateMaxLocals(n);

      // Adds the instruction to the bytecode of the method.
      if (varIndex > 255 || increment > 127 || increment < -128) {
         code.putByte(WIDE).put12(IINC, varIndex).putShort(increment);
      }
      else {
         code.putByte(IINC).put11(varIndex, increment);
      }
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {
      // Adds the instruction to the bytecode of the method.
      int source = code.getLength();
      code.putByte(TABLESWITCH);
      code.roundUpLength();
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
      int source = code.getLength();
      code.putByte(LOOKUPSWITCH);
      code.roundUpLength();
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
   public void visitTryCatchBlock(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
      exceptionHandling.addHandler(start, end, handler, type);
   }

   @Override
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      int localsCount = localVariableTableWriter.addLocalVariable(name, desc, signature, start, end, index);
      stackMapTableWriter.updateMaxLocals(localsCount);
   }

   @Override
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {
      lineNumberTableWriter.addLineNumber(line, start);
   }

   @Override
   public void visitMaxStack(@Nonnegative int maxStack) {
      int computedMaxStack;

      if (computeFrames) {
         exceptionHandling.completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames();

         Frame firstFrame = cfgAnalysis.getFirstFrame();
         stackMapTableWriter.createAndVisitFirstFrame(firstFrame, cw.getInternalClassName(), descriptor, classOrMemberAccess);

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

      stackMapTableWriter.setMaxStack(computedMaxStack);
   }

   @Nonnegative
   private int visitAllFramesToBeStoredInStackMap(@Nonnegative int max) {
      Label label = cfgAnalysis.getLabelForFirstBasicBlock();

      while (label != null) {
         Frame frame = label.getFrame();

         if (label.isStoringFrame()) {
            stackMapTableWriter.visitFrame(frame);
         }

         if (!label.isReachable()) {
            // Finds start and end of dead basic block.
            Label k = label.getSuccessor();
            int start = label.position;
            int end = (k == null ? code.getLength() : k.position) - 1;

            // If non empty basic block.
            if (end >= start) {
               max = Math.max(max, 1);

               replaceInstructionsWithNOPAndATHROW(start, end);

               stackMapTableWriter.emitFrameForUnreachableBlock(start);
               exceptionHandling.removeStartEndRange(label, k);
            }
         }

         label = label.getSuccessor();
      }

      return max;
   }

   // Replaces instructions with NOP ... NOP ATHROW.
   private void replaceInstructionsWithNOPAndATHROW(@Nonnegative int start, @Nonnegative int end) {
      byte[] data = code.getData();

      for (int i = start; i < end; i++) {
         data[i] = NOP;
      }

      //noinspection NumericCastThatLosesPrecision
      data[end] = (byte) ATHROW;
   }

   /**
    * Returns the size of the bytecode of this method.
    */
   @Nonnegative
   public int getSize() {
      if (classReaderOffset > 0) {
         return 6 + classReaderLength;
      }

      int size = 8 + getMarkerAttributesSize() + getAnnotationsSize() + getParameterAnnotationsSize();
      int codeLength = code.getLength();

      if (codeLength > 0) {
         if (codeLength > 65536) {
            throw new RuntimeException("Method code too large!");
         }

         cp.newUTF8("Code");

         size += 18 + codeLength + exceptionHandling.getSize();
         size += localVariableTableWriter.getSize();
         size += lineNumberTableWriter.getSize();
         size += stackMapTableWriter.getSize();
      }

      if (exceptionsWriter != null) {
         size += exceptionsWriter.getSize();
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
            AnnotationVisitor parameterAnnotation = parameterAnnotations[i];
            size += parameterAnnotation == null ? 0 : parameterAnnotation.getSize();
         }
      }

      return size;
   }

   /**
    * Puts the bytecode of this method in the given byte vector.
    */
   @Override
   protected void put(@Nonnull ByteVector out) {
      putAccess(out, Access.CONSTRUCTOR);
      out.putShort(nameItemIndex);
      out.putShort(descItemIndex);

      if (classReaderOffset > 0) {
         out.putByteArray(cw.code, classReaderOffset, classReaderLength);
         return;
      }

      putMethodAttributeCount(out);
      putMethodCode(out);

      if (exceptionsWriter != null) {
         exceptionsWriter.put(out);
      }

      putMarkerAttributes(out);

      if (signatureWriter != null) {
         signatureWriter.put(out);
      }

      putAnnotationAttributes(out);
   }

   private void putMethodAttributeCount(@Nonnull ByteVector out) {
      int attributeCount = getMarkerAttributeCount();

      if (code.getLength() > 0) {
         attributeCount++;
      }

      if (exceptionsWriter != null) {
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
      if (code.getLength() > 0) {
         putCodeSize(out);
         stackMapTableWriter.putMaxStackAndLocals(out);
         out.putInt(code.getLength()).putByteVector(code);
         exceptionHandling.put(out);

         int codeAttributeCount = localVariableTableWriter.getAttributeCount();

         if (lineNumberTableWriter.hasLineNumbers()) {
            codeAttributeCount++;
         }

         if (stackMapTableWriter.hasStackMap()) {
            codeAttributeCount++;
         }

         out.putShort(codeAttributeCount);
         localVariableTableWriter.put(out);
         lineNumberTableWriter.put(out);
         stackMapTableWriter.put(out);
      }
   }

   private void putCodeSize(@Nonnull ByteVector out) {
      int size =
         12 + code.getLength() +
         exceptionHandling.getSize() + localVariableTableWriter.getSize() + lineNumberTableWriter.getSize() + stackMapTableWriter.getSize();

      out.putShort(cp.newUTF8("Code")).putInt(size);
   }

   private void putAnnotationAttributes(@Nonnull ByteVector out) {
      putAnnotations(out);

      if (parameterAnnotations != null) {
         out.putShort(cp.newUTF8("RuntimeVisibleParameterAnnotations"));
         AnnotationVisitor.put(out, parameterAnnotations);
      }
   }

   @Nullable
   public Label getCurrentBlock() { return cfgAnalysis.getLabelForCurrentBasicBlock(); }
}