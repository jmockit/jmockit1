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
   @Nonnegative private int parameterAnnotationsCodeIndex;

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
    *
    * @return the offset of the first byte following the last method in the class.
    */
   int readMethods() {
      for (int methodCount = readUnsignedShort(); methodCount > 0; methodCount--) {
         readMethod();
      }

      return codeIndex;
   }

   private void readMethod() {
      readMethodDeclaration();
      parameterAnnotationsCodeIndex = 0;

      readAttributes();

      int codeIndex = this.codeIndex;
      readMethodBody();
      this.codeIndex = codeIndex;
   }

   private void readMethodDeclaration() {
      access = readUnsignedShort();
      name = readNonnullUTF8();
      desc = readNonnullUTF8();

      methodStartCodeIndex = codeIndex;
      bodyStartCodeIndex = 0;
      throwsClauseTypes = null;
   }

   @Nullable @Override
   Boolean readAttribute(@Nonnull String attributeName) {
      if ("Code".equals(attributeName)) {
         bodyStartCodeIndex = codeIndex;
         return false;
      }

      if ("Exceptions".equals(attributeName)) {
         readExceptionsInThrowsClause();
         return true;
      }

      if ("RuntimeVisibleParameterAnnotations".equals(attributeName)) {
         parameterAnnotationsCodeIndex = codeIndex;
         return false;
      }

      return null;
   }

   private void readExceptionsInThrowsClause() {
      int n = readUnsignedShort();
      String[] typeDescs = new String[n];

      for (int i = 0; i < n; i++) {
         typeDescs[i] = readNonnullClass();
      }

      throwsClauseTypes = typeDescs;
   }

   private void readMethodBody() {
      mv = cr.cv.visitMethod(access, name, desc, signature, throwsClauseTypes);

      if (mv == null) {
         return;
      }

      if (mv instanceof MethodWriter) {
         copyMethodBody();
         return;
      }

      readAnnotations(mv);
      readAnnotationsOnAllParameters();

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

   private void readAnnotationsOnAllParameters() {
      if (parameterAnnotationsCodeIndex > 0) {
         codeIndex = parameterAnnotationsCodeIndex;
         int parameters = readUnsignedByte();

         for (int i = 0; i < parameters; i++) {
            readParameterAnnotations(i);
         }
      }
   }

   private void readParameterAnnotations(@Nonnegative int parameterIndex) {
      for (int annotationCount = readUnsignedShort(); annotationCount > 0; annotationCount--) {
         String annotationTypeDesc = readNonnullUTF8();
         AnnotationVisitor av = mv.visitParameterAnnotation(parameterIndex, annotationTypeDesc);
         readAnnotationValues(av);
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
      visitEndLabel(codeLength);
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
      int opcode = readUnsignedByte();
      byte instructionType = INSTRUCTION_TYPE[opcode];
      boolean tablInsn = instructionType == TABL_INSN;

      if (tablInsn || instructionType == LOOK_INSN) {
         readLabelsForSwitchInstruction(offset, tablInsn);
      }
      else {
         readLabelsForNonSwitchInstruction(offset, instructionType);
      }
   }

   private void readLabelsForSwitchInstruction(@Nonnegative int offset, boolean tableNotLookup) {
      readSwitchDefaultLabel(offset);

      int caseCount;

      if (tableNotLookup) {
         int min = readInt();
         int max = readInt();
         caseCount = max - min + 1;
      }
      else {
         caseCount = readInt();
      }

      while (caseCount > 0) {
         if (!tableNotLookup) {
            codeIndex += 4;
         }

         int caseOffset = offset + readInt();
         getOrCreateLabel(caseOffset);
         caseCount--;
      }
   }

   @Nonnull
   private Label readSwitchDefaultLabel(@Nonnegative int offset) {
      codeIndex += 3 - (offset & 3); // skips 0 to 3 padding bytes

      int defaultLabelOffset = readInt();
      return getOrCreateLabel(offset + defaultLabelOffset);
   }

   @SuppressWarnings("OverlyLongMethod")
   private void readLabelsForNonSwitchInstruction(@Nonnegative int offset, byte instructionType) {
      int codeIndexSize = 0;

      switch (instructionType) {
         case NOARG: case IMPLVAR:
            return;
         case LABEL:
            int labelOffset = offset + readShort();
            getOrCreateLabel(labelOffset);
            return;
         case LABELW:
            int labelOffsetW = offset + readInt();
            getOrCreateLabel(labelOffsetW);
            return;
         case WIDE_INSN:
            int opcode = readUnsignedByte();
            codeIndexSize = opcode == IINC ? 4 : 2;
            break;
         case VAR: case SBYTE: case LDC_INSN:
            codeIndexSize = 1;
            break;
         case SHORT: case LDCW_INSN: case TYPE_INSN: case FIELDORMETH: case IINC_INSN:
            codeIndexSize = 2;
            break;
         case ITFMETH: case INDYMETH:
            codeIndexSize = 4;
            break;
         case MANA_INSN:
            codeIndexSize = 3;
      }

      codeIndex += codeIndexSize;
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
      boolean readDebugInfo, @Nonnegative int codeStartIndex, @Nonnegative int codeEndIndex
   ) {
      codeIndex = codeStartIndex;

      while (codeIndex < codeEndIndex) {
         int offset = codeIndex - codeStartIndex;
         visitLabelAndLineNumber(readDebugInfo, offset);

         int opcode = readUnsignedByte();

         switch (INSTRUCTION_TYPE[opcode]) {
            case NOARG:       mv.visitInsn(opcode); break;
            case VAR:         readVariableAccessInstruction(opcode); break;
            case IMPLVAR:     readInstructionWithImplicitVariable(opcode); break;
            case TYPE_INSN:   readTypeInsn(opcode); break;
            case LABEL:       readJump(opcode, offset); break;
            case LABELW:      readWideJump(opcode, offset); break;
            case LDC_INSN:    readLDC(); break;
            case LDCW_INSN:   readLDCW(); break;
            case IINC_INSN:   readIInc(); break;
            case SBYTE:       readInstructionTakingASignedByte(opcode); break;
            case SHORT:       readInstructionTakingASignedShort(opcode); break;
            case TABL_INSN:   readSwitchInstruction(offset, true); break;
            case LOOK_INSN:   readSwitchInstruction(offset, false); break;
            case MANA_INSN:   readMultiANewArray(); break;
            case WIDE_INSN:   readWideInstruction(); break;
            case FIELDORMETH:
            case ITFMETH:     readFieldOrInvokeInstruction(opcode); break;
            case INDYMETH:    readInvokeDynamicInstruction(); break;
         }
      }
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

   private void readVariableAccessInstruction(int opcode) {
      int var = readUnsignedByte();
      mv.visitVarInsn(opcode, var);
   }

   private void readInstructionWithImplicitVariable(int opcode) {
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
      int varIndex = opcode & 3;

      mv.visitVarInsn(localVarOpcode, varIndex);
   }

   private void readTypeInsn(int opcode) {
      String typeDesc = readNonnullClass();
      mv.visitTypeInsn(opcode, typeDesc);
   }

   private void readJump(int opcode, @Nonnegative int offset) {
      short targetIndex = readShort();
      Label targetLabel = labels[offset + targetIndex];
      mv.visitJumpInsn(opcode, targetLabel);
   }

   private void readWideJump(int opcode, @Nonnegative int offset) {
      int targetIndex = readInt();
      Label targetLabel = labels[offset + targetIndex];
      mv.visitJumpInsn(opcode - 33, targetLabel);
   }

   private void readLDC() {
      int constIndex = readUnsignedByte();
      Object cst = readConst(constIndex);
      mv.visitLdcInsn(cst);
   }

   private void readLDCW() {
      Object cst = readConstItem();
      mv.visitLdcInsn(cst);
   }

   private void readIInc() {
      int var = readUnsignedByte();
      int increment = readSignedByte();
      mv.visitIincInsn(var, increment);
   }

   private void readInstructionTakingASignedByte(int opcode) {
      int operand = readSignedByte();
      mv.visitIntInsn(opcode, operand);
   }

   private void readInstructionTakingASignedShort(int opcode) {
      int operand = readShort();
      mv.visitIntInsn(opcode, operand);
   }

   private void readSwitchInstruction(@Nonnegative int offset, boolean tableNotLookup) {
      Label dfltLabel = readSwitchDefaultLabel(offset);
      int min;
      int max;
      int caseCount;
      int[] keys;

      if (tableNotLookup) {
         min = readInt();
         max = readInt();
         caseCount = max - min + 1;
         keys = null;
      }
      else {
         min = max = 0;
         caseCount = readInt();
         keys = new int[caseCount];
      }

      Label[] handlerLabels = readSwitchCaseLabels(offset, caseCount, keys);

      if (tableNotLookup) {
         mv.visitTableSwitchInsn(min, max, dfltLabel, handlerLabels);
      }
      else {
         mv.visitLookupSwitchInsn(dfltLabel, keys, handlerLabels);
      }
   }

   @Nonnull
   private Label[] readSwitchCaseLabels(@Nonnegative int offset, @Nonnegative int caseCount, @Nullable int[] keys) {
      Label[] caseLabels = new Label[caseCount];

      for (int i = 0; i < caseCount; i++) {
         if (keys != null) {
            keys[i] = readInt();
         }

         int labelOffset = offset + readInt();
         caseLabels[i] = labels[labelOffset];
      }

      return caseLabels;
   }

   private void readMultiANewArray() {
      String arrayTypeDesc = readNonnullClass();
      int dims = readUnsignedByte();
      mv.visitMultiANewArrayInsn(arrayTypeDesc, dims);
   }

   private void readWideInstruction() {
      int opcode = readUnsignedByte();
      int var = readUnsignedShort();

      if (opcode == IINC) {
         int increment = readShort();
         mv.visitIincInsn(var, increment);
      }
      else {
         mv.visitVarInsn(opcode, var);
         codeIndex += 2;
      }
   }

   private void readFieldOrInvokeInstruction(int opcode) {
      int ownerCodeIndex = readItem();
      String owner = readNonnullClass(ownerCodeIndex);
      int nameCodeIndex = readItem(ownerCodeIndex + 2);
      String name = readNonnullUTF8(nameCodeIndex);
      String desc = readNonnullUTF8(nameCodeIndex + 2);

      if (opcode < INVOKEVIRTUAL) {
         mv.visitFieldInsn(opcode, owner, name, desc);
      }
      else {
         boolean itf = code[ownerCodeIndex - 1] == Type.IMETH;
         mv.visitMethodInsn(opcode, owner, name, desc, itf);

         if (opcode == INVOKEINTERFACE) {
            codeIndex += 2;
         }
      }
   }

   private void readInvokeDynamicInstruction() {
      int cpIndex = readItem();
      int bsmStartIndex = readUnsignedShort(cpIndex);
      int nameCodeIndex = readItem(cpIndex + 2);

      String bsmName = readNonnullUTF8(nameCodeIndex);
      String bsmDesc = readNonnullUTF8(nameCodeIndex + 2);

      @SuppressWarnings("ConstantConditions") int bsmCodeIndex = cr.bootstrapMethods[bsmStartIndex];
      MethodHandle bsmHandle = readMethodHandleItem(bsmCodeIndex);
      int bsmArgCount = readUnsignedShort(bsmCodeIndex + 2);
      bsmCodeIndex += 4;
      Object[] bsmArgs = new Object[bsmArgCount];

      for (int i = 0; i < bsmArgCount; i++) {
         bsmArgs[i] = readConstItem(bsmCodeIndex);
         bsmCodeIndex += 2;
      }

      mv.visitInvokeDynamicInsn(bsmName, bsmDesc, bsmHandle, bsmArgs);
      codeIndex += 2;
   }

   private void visitEndLabel(@Nonnegative int codeLength) {
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
