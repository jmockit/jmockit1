package mockit.external.asm;

import javax.annotation.*;

import mockit.external.asm.ClassReader.*;
import mockit.external.asm.Item.*;
import static mockit.external.asm.MethodReader.InstructionType.*;
import static mockit.external.asm.Opcodes.*;

final class MethodReader extends AnnotatedReader
{
   /**
    * Constants that subdivide the 220 {@linkplain Opcodes instruction opcodes} in 18 types of instructions.
    * Such types vary in the number and size of arguments the instruction takes (no argument, a signed byte, a signed
    * short), on whether it takes a local variable index, a jump target label, etc. Some types contain a single
    * instruction, such as LDC and IINC.
    */
   interface InstructionType
   {
      int NOARG       = 0; // instructions without any argument
      int SBYTE       = 1; // instructions with a signed byte argument
      int SHORT       = 2; // instructions with a signed short argument
      int VAR         = 3; // instructions with a local variable index argument
      int IMPLVAR     = 4; // instructions with an implicit local variable index argument
      int TYPE_INSN   = 5; // instructions with a type descriptor argument
      int FIELDORMETH = 6; // field and method invocations instructions
      int ITFMETH     = 7; // INVOKEINTERFACE/INVOKEDYNAMIC instruction
      int INDYMETH    = 8; // INVOKEDYNAMIC instruction
      int LABEL       = 9; // instructions with a 2 bytes bytecode offset label
      int LABELW     = 10; // instructions with a 4 bytes bytecode offset label
      int LDC_INSN   = 11; // the LDC instruction
      int LDCW_INSN  = 12; // the LDC_W and LDC2_W instructions
      int IINC_INSN  = 13; // the IINC instruction
      int TABL_INSN  = 14; // the TABLESWITCH instruction
      int LOOK_INSN  = 15; // the LOOKUPSWITCH instruction
      int MANA_INSN  = 16; // the MULTIANEWARRAY instruction
      int WIDE_INSN  = 17; // the WIDE instruction
   }

   /**
    * The {@linkplain InstructionType instruction types} of all JVM opcodes, one value for each instruction opcode.
    */
   private static final byte[] INSTRUCTION_TYPE;
   static {
      String s =
         "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD" +
         "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
         "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA" +
         "AAAAGGGGGGGHIFBFAAFFAARQJJKKJJJJJJJJJJJJJJJJJJ";
      int n = s.length();
      byte[] types = new byte[n];

      for (int i = 0; i < n; i++) {
         types[i] = (byte) (s.charAt(i) - 'A');
      }

      INSTRUCTION_TYPE = types;
   }

   @Nonnull private final ClassReader cr;

   @Nullable private String[] throwsClauseTypes;
   @Nullable private String signature;

   /**
    * The access flags of the method currently being parsed.
    */
   private int access;

   /**
    * The name of the method currently being parsed.
    */
   private String name;

   /**
    * The descriptor of the method currently being parsed.
    */
   private String desc;

   @Nonnegative private int methodStartCodeIndex;
   @Nonnegative private int bodyStartCodeIndex;

   /**
    * The label objects, indexed by bytecode offset, of the method currently being parsed (only bytecode offsets for
    * which a label is needed have a non null associated <tt>Label</tt> object).
    */
   @Nonnull private Label[] labels;

   /**
    * The visitor to visit the method being read.
    */
   private MethodVisitor mv;

   MethodReader(@Nonnull ClassReader cr) {
      super(cr);
      this.cr = cr;
   }

   /**
    * Reads each method and constructor in the class, making the {@linkplain #cr class reader}'s
    * {@linkplain ClassReader#cv visitor} visit it.
    */
   void readMethods(@Nonnegative int codeIndex) {
      this.codeIndex = codeIndex;
      int methodCount = readUnsignedShort();

      for (int i = methodCount; i > 0; i--) {
         readMethod();
      }
   }

   private void readMethod() {
      readMethodDeclaration();
      int annotationsCodeIndex = 0;
      int parameterAnnotationsCodeIndex = 0;

      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         String attrName = readNonnullUTF8();
         int codeOffset = readInt();

         if ("Code".equals(attrName)) {
            bodyStartCodeIndex = codeIndex;
         }
         else if ("Exceptions".equals(attrName)) {
            readExceptionsInThrowsClause();
            continue;
         }
         else if ("Signature".equals(attrName)) {
            signature = readNonnullUTF8();
            continue;
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            annotationsCodeIndex = codeIndex;
         }
         else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
            parameterAnnotationsCodeIndex = codeIndex;
         }

         codeIndex += codeOffset;
      }

      int codeIndex = this.codeIndex;
      readMethodBody(annotationsCodeIndex, parameterAnnotationsCodeIndex);
      this.codeIndex = codeIndex;
   }

   private void readMethodDeclaration() {
      access = readUnsignedShort();
      name = readNonnullUTF8();
      desc = readNonnullUTF8();

      methodStartCodeIndex = codeIndex;
      bodyStartCodeIndex = 0;
      throwsClauseTypes = null;
      signature = null;
   }

   private void readExceptionsInThrowsClause() {
      int n = readUnsignedShort();
      String[] typeDescs = new String[n];

      for (int i = 0; i < n; i++) {
         typeDescs[i] = readNonnullClass();
      }

      throwsClauseTypes = typeDescs;
   }

   private void readMethodBody(@Nonnegative int annotationsCodeIndex, @Nonnegative int parameterAnnotationsCodeIndex) {
      mv = cr.cv.visitMethod(access, name, desc, signature, throwsClauseTypes);

      if (mv == null) {
         return;
      }

      if (mv instanceof MethodWriter) {
         copyMethodBody();
         return;
      }

      if (annotationsCodeIndex > 0) {
         codeIndex = annotationsCodeIndex;
         readAnnotationValues();
      }

      if (parameterAnnotationsCodeIndex > 0) {
         codeIndex = parameterAnnotationsCodeIndex;
         readParameterAnnotations();
      }

      if (bodyStartCodeIndex > 0) {
         int flags = cr.flags;

         if ((flags & Flags.SKIP_CODE) == 0) {
            codeIndex = bodyStartCodeIndex;
            readCode((flags & Flags.SKIP_DEBUG) == 0);
         }
      }

      mv.visitEnd();
   }

   /**
    * If the returned <tt>MethodVisitor</tt> is in fact a <tt>MethodWriter</tt>, it means there is no method adapter
    * between the reader and the writer.
    * In addition, it's assumed that the writer's constant pool was copied from this reader (mw.cw.cr == this.cr), and
    * the signature of the method has not been changed; then, we skip all visit events and just copy the original code
    * of the method to the writer.
    */
   private void copyMethodBody() {
      // We do not copy directly the code into MethodWriter to save a byte array copy operation.
      // The real copy will be done in ClassWriter.toByteArray().
      MethodWriter mw = (MethodWriter) mv;
      mw.classReaderOffset = methodStartCodeIndex;
      mw.classReaderLength = codeIndex - methodStartCodeIndex;
   }

   private void readAnnotationValues() {
      int valueCount = readUnsignedShort();

      while (valueCount > 0) {
         String desc = readNonnullUTF8();
         AnnotationVisitor av = mv.visitAnnotation(desc);
         codeIndex = annotationReader.readNamedAnnotationValues(codeIndex, av);
         valueCount--;
      }
   }

   private void readParameterAnnotations() {
      int parameters = readByte();

      for (int i = 0; i < parameters; i++) {
         readParameterAnnotations(i);
      }
   }

   private void readParameterAnnotations(@Nonnegative int parameterIndex) {
      int annotationCount = readUnsignedShort();

      while (annotationCount > 0) {
         String desc = readNonnullUTF8();
         AnnotationVisitor av = mv.visitParameterAnnotation(parameterIndex, desc);
         codeIndex = annotationReader.readNamedAnnotationValues(codeIndex, av);
         annotationCount--;
      }
   }

   private void readCode(boolean readDebugInfo) {
      int maxStack = readUnsignedShort();
      codeIndex += 2; // maxLocals

      int codeLength = readInt();
      labels = new Label[codeLength + 2];

      // Reads the bytecode to find the labels.
      int codeStartIndex = codeIndex;
      int codeEndIndex = codeStartIndex + codeLength;

      readAllLabelsInCodeBlock(codeStartIndex, codeEndIndex);
      readTryCatchBlocks();

      // Reads the code attributes.
      int varTableCodeIndex = 0;
      int[] typeTable = null;

      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         String attrName = readNonnullUTF8();
         int codeOffset = readInt();

         if (readDebugInfo) {
            if ("LocalVariableTable".equals(attrName)) {
               varTableCodeIndex = codeIndex;
               readLocalVariableTable();
            }
            else if ("LocalVariableTypeTable".equals(attrName)) {
               typeTable = readLocalVariableTypeTable();
            }
            else if ("LineNumberTable".equals(attrName)) {
               readLineNumberTable();
            }
            else {
               codeIndex += codeOffset;
            }
         }
      }

      readBytecodeInstructionsInCodeBlock(readDebugInfo, codeStartIndex, codeEndIndex);
      readEndLabel(codeLength);
      readLocalVariableTables(varTableCodeIndex, typeTable);
      mv.visitMaxStack(maxStack);
   }

   private void readAllLabelsInCodeBlock(@Nonnegative int codeStart, @Nonnegative int codeEnd) {
      getOrCreateLabel(codeEnd - codeStart + 1);

      while (codeIndex < codeEnd) {
         int offset = codeIndex - codeStart;
         readLabelForInstructionIfAny(offset);
      }
   }

   @Nonnull
   private Label getOrCreateLabel(@Nonnegative int offset) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         labels[offset] = label;
      }

      return label;
   }

   private void readLabelForInstructionIfAny(@Nonnegative int offset) {
      int opcode = readByte();
      byte instructionType = INSTRUCTION_TYPE[opcode];
      boolean tablInsn = instructionType == TABL_INSN;

      if (tablInsn || instructionType == LOOK_INSN) {
         readSwitchInstruction(offset, tablInsn);
      }
      else {
         int codeOffset = readNonSwitchInstruction(offset, instructionType);
         codeIndex += codeOffset;
      }
   }

   private void readSwitchInstruction(@Nonnegative int offset, boolean tableNotLookup) {
      // Skips 0 to 3 padding bytes.
      codeIndex += 3 - (offset & 3);

      // Reads instruction.
      int labelOffset = readInt();
      getOrCreateLabel(offset + labelOffset);

      int caseCount;
      int offsetStep;
      int finalOffsetStep;

      if (tableNotLookup) {
         int firstCase = readInt(codeIndex);
         int lastCase = readInt(codeIndex + 4);
         caseCount = lastCase - firstCase + 1;
         offsetStep = 4;
         finalOffsetStep = 8;
      }
      else {
         caseCount = readInt(codeIndex);
         offsetStep = 8;
         finalOffsetStep = 4;
      }

      while (caseCount > 0) {
         int caseOffset = readInt(codeIndex + 8);
         getOrCreateLabel(offset + caseOffset);
         codeIndex += offsetStep;
         caseCount--;
      }

      codeIndex += finalOffsetStep;
   }

   /**
    * @return the offset for codeIndex
    */
   @Nonnegative
   private int readNonSwitchInstruction(@Nonnegative int offset, byte instructionType) {
      switch (instructionType) {
         case NOARG: case IMPLVAR: return 0;
         case LABEL:  getOrCreateLabel(offset + readShort()); return 0;
         case LABELW: getOrCreateLabel(offset + readInt());   return 0;
         case WIDE_INSN: int opcode = readByte(); return opcode == IINC ? 4 : 2;
         case VAR: case SBYTE: case LDC_INSN: return 1;
         case SHORT: case LDCW_INSN: case FIELDORMETH: case TYPE_INSN: case IINC_INSN: return 2;
         case ITFMETH: case INDYMETH: return 4;
         case MANA_INSN: default: return 3;
      }
   }

   /**
    * Reads the try catch entries to find the labels, and also visits them.
    */
   private void readTryCatchBlocks() {
      for (int blockCount = readUnsignedShort(); blockCount > 0; blockCount--) {
         Label start   = getOrCreateLabel(readUnsignedShort());
         Label end     = getOrCreateLabel(readUnsignedShort());
         Label handler = getOrCreateLabel(readUnsignedShort());
         String type   = readUTF8(readItem());

         mv.visitTryCatchBlock(start, end, handler, type);
      }
   }

   private void readLocalVariableTable() {
      for (int localVarCount = readUnsignedShort(); localVarCount > 0; localVarCount--) {
         int labelOffset = readUnsignedShort();
         getOrCreateDebugLabel(labelOffset);

         labelOffset += readUnsignedShort();
         getOrCreateDebugLabel(labelOffset);

         codeIndex += 6;
      }
   }

   @Nonnull
   private Label getOrCreateDebugLabel(@Nonnegative int offset) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         label.markAsDebug();
         labels[offset] = label;
      }

      return label;
   }

   @Nonnull
   private int[] readLocalVariableTypeTable() {
      int typeTableSize = 3 * readUnsignedShort();
      int[] typeTable = new int[typeTableSize];

      while (typeTableSize > 0) {
         int startIndex = readUnsignedShort();
         int signatureCodeIndex = codeIndex + 4;
         codeIndex += 6;
         int varIndex = readUnsignedShort();

         typeTable[--typeTableSize] = signatureCodeIndex;
         typeTable[--typeTableSize] = varIndex;
         typeTable[--typeTableSize] = startIndex;
      }

      return typeTable;
   }

   private void readLineNumberTable() {
      for (int lineCount = readUnsignedShort(); lineCount > 0; lineCount--) {
         int labelOffset = readUnsignedShort();
         Label debugLabel = getOrCreateDebugLabel(labelOffset);
         debugLabel.line = readUnsignedShort();
      }
   }

   @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   private void readBytecodeInstructionsInCodeBlock(
      boolean readDebugInfo, @Nonnegative int codeStart, @Nonnegative int codeEnd
   ) {
      int codeIndex = codeStart;

      while (codeIndex < codeEnd) {
         int offset = codeIndex - codeStart;
         visitLabelAndLineNumber(readDebugInfo, offset);

         int opcode = readByte(codeIndex);

         switch (INSTRUCTION_TYPE[opcode]) {
            case NOARG:     mv.visitInsn(opcode); codeIndex++; break;
            case IMPLVAR:   readImplicitVarInstruction(opcode); codeIndex++; break;
            case LABEL:     codeIndex = readJump(codeIndex, opcode, offset); break;
            case LABELW:    codeIndex = readWideJump(codeIndex, opcode, offset); break;
            case WIDE_INSN: codeIndex = readWideInstruction(codeIndex); break;
            case TABL_INSN: codeIndex = readTableSwitchInstruction(codeIndex, offset); break;
            case LOOK_INSN: codeIndex = readLookupSwitchInstruction(codeIndex, offset); break;
            case VAR:       codeIndex = readVariableAccessInstruction(codeIndex, opcode); break;
            case SBYTE:     codeIndex = readInstructionTakingASignedByte(codeIndex, opcode); break;
            case SHORT:     codeIndex = readInstructionTakingASignedShort(codeIndex, opcode); break;
            case LDC_INSN:  codeIndex = readLDC(codeIndex); break;
            case LDCW_INSN: codeIndex = readLDCW(codeIndex); break;
            case FIELDORMETH: case ITFMETH: codeIndex = readFieldOrInvokeInstruction(codeIndex, opcode); break;
            case INDYMETH:  codeIndex = readInvokeDynamicInstruction(codeIndex); break;
            case TYPE_INSN: codeIndex = readTypeInsn(codeIndex, opcode); break;
            case IINC_INSN: codeIndex = readIInc(codeIndex); break;
            case MANA_INSN: default: codeIndex = readMultiANewArray(codeIndex);
         }
      }
   }

   @Nonnegative
   private int readJump(@Nonnegative int codeIndex, int opcode, @Nonnegative int offset) {
      short targetIndex = readShort(codeIndex + 1);
      Label targetLabel = labels[offset + targetIndex];
      mv.visitJumpInsn(opcode, targetLabel);
      return codeIndex + 3;
   }

   @Nonnegative
   private int readWideJump(@Nonnegative int codeIndex, int opcode, @Nonnegative int offset) {
      int targetIndex = readInt(codeIndex + 1);
      Label targetLabelW = labels[offset + targetIndex];
      mv.visitJumpInsn(opcode - 33, targetLabelW);
      return codeIndex + 5;
   }

   @Nonnegative
   private int readVariableAccessInstruction(@Nonnegative int codeIndex, int opcode) {
      int var = readByte(codeIndex + 1);
      mv.visitVarInsn(opcode, var);
      return codeIndex + 2;
   }

   @Nonnegative
   private int readInstructionTakingASignedByte(@Nonnegative int codeIndex, int opcode) {
      byte byteOperand = code[codeIndex + 1];
      mv.visitIntInsn(opcode, byteOperand);
      return codeIndex + 2;
   }

   @Nonnegative
   private int readInstructionTakingASignedShort(@Nonnegative int codeIndex, int opcode) {
      int shortOperand = readShort(codeIndex + 1);
      mv.visitIntInsn(opcode, shortOperand);
      return codeIndex + 3;
   }

   @Nonnegative
   private int readLDC(@Nonnegative int codeIndex) {
      int ldcIndex = readByte(codeIndex + 1);
      Object cst = readConst(ldcIndex);
      mv.visitLdcInsn(cst);
      return codeIndex + 2;
   }

   @Nonnegative
   private int readLDCW(@Nonnegative int codeIndex) {
      Object cstWide = readConstItem(codeIndex + 1);
      mv.visitLdcInsn(cstWide);
      return codeIndex + 3;
   }

   @Nonnegative
   private int readInvokeDynamicInstruction(@Nonnegative int codeIndex) {
      int cpIndex = readItem(codeIndex + 1);
      int bsmStartIndex = readUnsignedShort(cpIndex);
      @SuppressWarnings("ConstantConditions") int bsmIndex = cr.bootstrapMethods[bsmStartIndex];
      Handle bsm = (Handle) readConstItem(bsmIndex);
      int bsmArgCount = readUnsignedShort(bsmIndex + 2);
      Object[] bsmArgs = new Object[bsmArgCount];
      bsmIndex += 4;

      for (int i = 0; i < bsmArgCount; i++) {
         bsmArgs[i] = readConstItem(bsmIndex);
         bsmIndex += 2;
      }

      cpIndex = readItem(cpIndex + 2);
      String name = readNonnullUTF8(cpIndex);
      String desc = readNonnullUTF8(cpIndex + 2);

      mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      codeIndex += 5;

      return codeIndex;
   }

   @Nonnegative
   private int readTypeInsn(@Nonnegative int codeIndex, int opcode) {
      String typeDesc = readNonnullClass(codeIndex + 1);
      mv.visitTypeInsn(opcode, typeDesc);
      return codeIndex + 3;
   }

   @Nonnegative
   private int readIInc(@Nonnegative int codeIndex) {
      int var = readByte(codeIndex + 1);
      byte increment = code[codeIndex + 2];
      mv.visitIincInsn(var, increment);
      return codeIndex + 3;
   }

   @Nonnegative
   private int readMultiANewArray(@Nonnegative int codeIndex) {
      String arrayTypeDesc = readNonnullClass(codeIndex + 1);
      int dims = readByte(codeIndex + 3);
      mv.visitMultiANewArrayInsn(arrayTypeDesc, dims);
      return codeIndex + 4;
   }

   private void visitLabelAndLineNumber(boolean readDebugInfo, @Nonnegative int offset) {
      Label label = labels[offset];

      if (label != null) {
         mv.visitLabel(label);

         if (readDebugInfo) {
            int lineNumber = label.line;

            if (lineNumber > 0) {
               mv.visitLineNumber(lineNumber, label);
            }
         }
      }
   }

   private void readImplicitVarInstruction(int opcode) {
      int opcodeBase;

      if (opcode > ISTORE) {
         opcode -= ISTORE_0;
         opcodeBase = ISTORE;
      }
      else {
         opcode -= ILOAD_0;
         opcodeBase = ILOAD;
      }

      int localVarOpcode = opcodeBase + (opcode >> 2);
      int varIndex = opcode & 0x3;

      mv.visitVarInsn(localVarOpcode, varIndex);
   }

   @Nonnegative
   private int readWideInstruction(@Nonnegative int codeIndex) {
      int opcode = readByte(codeIndex + 1);
      int var = readUnsignedShort(codeIndex + 2);
      int offset;

      if (opcode == IINC) {
         int increment = readShort(codeIndex + 4);
         mv.visitIincInsn(var, increment);
         offset = 6;
      }
      else {
         mv.visitVarInsn(opcode, var);
         offset = 4;
      }

      return codeIndex + offset;
   }

   @Nonnegative
   private int readTableSwitchInstruction(@Nonnegative int codeIndex, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      codeIndex = codeIndex + 4 - (offset & 3);

      // Reads instruction.
      int dfltLabelOffset = offset + readInt(codeIndex);
      int min = readInt(codeIndex + 4);
      int max = readInt(codeIndex + 8);
      Label[] table = new Label[max - min + 1];
      codeIndex += 12;

      for (int i = 0; i < table.length; i++) {
         int handlerLabelOffset = offset + readInt(codeIndex);
         table[i] = labels[handlerLabelOffset];
         codeIndex += 4;
      }

      Label dfltLabel = labels[dfltLabelOffset];
      mv.visitTableSwitchInsn(min, max, dfltLabel, table);
      return codeIndex;
   }

   @Nonnegative
   private int readLookupSwitchInstruction(@Nonnegative int codeIndex, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      codeIndex = codeIndex + 4 - (offset & 3);

      // Reads the instruction.
      int dfltLabelOffset = offset + readInt(codeIndex);
      int len = readInt(codeIndex + 4);
      int[] keys = new int[len];
      Label[] values = new Label[len];
      codeIndex += 8;

      for (int i = 0; i < len; i++) {
         keys[i] = readInt(codeIndex);
         int handlerLabelOffset = offset + readInt(codeIndex + 4);
         values[i] = labels[handlerLabelOffset];
         codeIndex += 8;
      }

      Label dfltLabel = labels[dfltLabelOffset];
      mv.visitLookupSwitchInsn(dfltLabel, keys, values);
      return codeIndex;
   }

   @Nonnegative
   private int readFieldOrInvokeInstruction(@Nonnegative int codeIndex, int opcode) {
      int cpIndex1 = readItem(codeIndex + 1);
      String owner = readNonnullClass(cpIndex1);
      int cpIndex2 = readItem(cpIndex1 + 2);
      String name = readNonnullUTF8(cpIndex2);
      String desc = readNonnullUTF8(cpIndex2 + 2);

      if (opcode < INVOKEVIRTUAL) {
         mv.visitFieldInsn(opcode, owner, name, desc);
      }
      else {
         boolean itf = code[cpIndex1 - 1] == Type.IMETH;
         mv.visitMethodInsn(opcode, owner, name, desc, itf);
      }

      int offset = opcode == INVOKEINTERFACE ? 5 : 3;
      return codeIndex + offset;
   }

   private void readEndLabel(@Nonnegative int codeLength) {
      Label label = labels[codeLength];

      if (label != null) {
         mv.visitLabel(label);
      }
   }

   private void readLocalVariableTables(@Nonnegative int varTableCodeIndex, @Nullable int[] typeTable) {
      if (varTableCodeIndex > 0) {
         codeIndex = varTableCodeIndex;

         for (int localVarCount = readUnsignedShort(); localVarCount > 0; localVarCount--) {
            int start  = readUnsignedShort();
            int length = readUnsignedShort();
            String name = readNonnullUTF8();
            String desc = readNonnullUTF8();
            int index  = readUnsignedShort();
            String signature = typeTable == null ? null : getLocalVariableSignature(typeTable, start, index);

            mv.visitLocalVariable(name, desc, signature, labels[start], labels[start + length], index);
         }
      }
   }

   @Nullable
   private String getLocalVariableSignature(@Nonnull int[] typeTable, @Nonnegative int start, @Nonnegative int index) {
      for (int i = 0, n = typeTable.length; i < n; i += 3) {
         if (typeTable[i] == start && typeTable[i + 1] == index) {
            String signature = readNonnullUTF8(typeTable[i + 2]);
            return signature;
         }
      }

      return null;
   }
}
