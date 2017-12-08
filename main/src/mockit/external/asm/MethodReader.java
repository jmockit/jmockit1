package mockit.external.asm;

import javax.annotation.*;

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

   @Nonnull private final ClassVisitor cv;

   /**
    * The start index of each bootstrap method.
    */
   @Nullable private final int[] bootstrapMethods;

   private final boolean readCode;
   private final boolean readDebugInfo;

   @Nullable private String[] throwsClauseTypes;
   @Nonnegative private int throwsClauseLastCodeIndex;

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
      cv = cr.cv;
      bootstrapMethods = cr.bootstrapMethods;
      readCode = cr.readCode;
      readDebugInfo = cr.readDebugInfo;
   }

   void readMethods(@Nonnegative int codeIndex) {
      int methodCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      for (int i = methodCount; i > 0; i--) {
         codeIndex = readMethod(codeIndex);
      }
   }

   /**
    * Reads a method and makes the given visitor visit it.
    *
    * @param codeIndex the start offset of the method in the class file.
    * @return the offset of the first byte following the method in the class.
    */
   @Nonnegative
   private int readMethod(@Nonnegative int codeIndex) {
      codeIndex = readMethodDeclaration(codeIndex);
      methodStartCodeIndex = codeIndex;
      bodyStartCodeIndex = 0;
      throwsClauseTypes = null;
      signature = null;
      int annDefault = 0;
      int anns = 0;
      int paramAnns = 0;

      for (int attributeCount = readUnsignedShort(codeIndex); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(codeIndex + 2);

         if ("Code".equals(attrName)) {
            bodyStartCodeIndex = codeIndex + 8;
         }
         else if ("Exceptions".equals(attrName)) {
            readExceptionsInThrowsClause(codeIndex);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(codeIndex + 8);
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("AnnotationDefault".equals(attrName)) {
            annDefault = codeIndex + 8;
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            anns = codeIndex + 8;
         }
         else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
            paramAnns = codeIndex + 8;
         }

         codeIndex += 6 + readInt(codeIndex + 4);
      }

      codeIndex += 2;
      readMethodBody(codeIndex, annDefault, anns, paramAnns);
      return codeIndex;
   }

   @Nonnegative
   private int readMethodDeclaration(@Nonnegative int codeIndex) {
      access = readUnsignedShort(codeIndex);
      name = readUTF8(codeIndex + 2);
      desc = readUTF8(codeIndex + 4);
      return codeIndex + 6;
   }

   private void readExceptionsInThrowsClause(@Nonnegative int codeIndex) {
      int n = readUnsignedShort(codeIndex + 8);
      throwsClauseTypes = new String[n];
      int throwsTypeCodeIndex = codeIndex + 10;

      for (int i = 0; i < n; i++) {
         throwsClauseTypes[i] = readClass(throwsTypeCodeIndex);
         throwsTypeCodeIndex += 2;
      }

      throwsClauseLastCodeIndex = throwsTypeCodeIndex;
   }

   private void readMethodBody(
      @Nonnegative int methodEndCodeIndex,
      @Nonnegative int annDefault, @Nonnegative int anns, @Nonnegative int paramAnns
   ) {
      mv = cv.visitMethod(access, name, desc, signature, throwsClauseTypes);

      if (mv == null || mv instanceof MethodWriter && copyMethodBody(methodEndCodeIndex)) {
         return;
      }

      readAnnotationDefaultValue(annDefault);
      readAnnotationValues(anns);
      readParameterAnnotations(paramAnns);
      readMethodCode();
      mv.visitEnd();
   }

   /**
    * If the returned MethodVisitor is in fact a <tt>MethodWriter</tt>, it means there is no method adapter between the
    * reader and the writer.
    * In addition, it's assumed that the writer's constant pool was copied from this reader (mw.cw.cr == this.cr), and
    * the signature of the method has not been changed; then, we skip all visit events and just copy the original code
    * of the method to the writer (the access, name and descriptor can have been changed, this is not important since
    * they are not copied as is from the reader).
    *
    * @return <tt>true</tt> if the method body can be copied, or <tt>false</tt> if it can't because the method's
    * <tt>throws</tt> clause was changed
    */
   private boolean copyMethodBody(@Nonnegative int endCodeIndex) {
      MethodWriter mw = (MethodWriter) mv;
      ThrowsClause throwsClause = mw.throwsClause;
      int throwsClauseCount = throwsClause.getExceptionCount();
      boolean sameExceptions = false;

      if (throwsClauseTypes == null) {
         sameExceptions = throwsClauseCount == 0;
      }
      else if (throwsClauseTypes.length == throwsClauseCount) {
         sameExceptions = true;
         int exception = throwsClauseLastCodeIndex;

         for (int throwsClauseIndex = throwsClauseCount - 1; throwsClauseIndex >= 0; throwsClauseIndex--) {
            exception -= 2;
            int exceptionIndex = readUnsignedShort(exception);

            if (throwsClause.getExceptionIndex(throwsClauseIndex) != exceptionIndex) {
               // TODO: verify why it gets here from SpringIntegrationTest, for method
               // "AbstractAutowireCapableBeanFactory#createBean(Class) throws BeansException"
               sameExceptions = false;
               break;
            }
         }
      }

      if (sameExceptions) {
         // We do not copy directly the code into MethodWriter to save a byte array copy operation.
         // The real copy will be done in ClassWriter.toByteArray().
         mw.classReaderOffset = methodStartCodeIndex;
         mw.classReaderLength = endCodeIndex - methodStartCodeIndex;
         return true;
      }

      return false;
   }

   private void readAnnotationDefaultValue(@Nonnegative int annotationDefault) {
      if (annotationDefault != 0) {
         AnnotationVisitor dv = mv.visitAnnotationDefault();
         annotationReader.readDefaultAnnotationValue(annotationDefault, dv);

         if (dv != null) {
            dv.visitEnd();
         }
      }
   }

   private void readAnnotationValues(@Nonnegative int anns) {
      if (anns != 0) {
         for (int valueCount = readUnsignedShort(anns), v = anns + 2; valueCount > 0; valueCount--) {
            String desc = readUTF8(v);
            @SuppressWarnings("ConstantConditions") AnnotationVisitor av = mv.visitAnnotation(desc);
            v = annotationReader.readNamedAnnotationValues(v + 2, av);
         }
      }
   }

   /**
    * Reads parameter annotations and makes the given visitor visit them.
    *
    * @param codeIndex start offset in {@link #code} of the annotations to be read.
    */
   private void readParameterAnnotations(@Nonnegative int codeIndex) {
      if (codeIndex != 0) {
         int parameters = readByte(codeIndex++);

         for (int i = 0; i < parameters; i++) {
            codeIndex = readParameterAnnotations(codeIndex, i);
         }
      }
   }

   @Nonnegative
   private int readParameterAnnotations(@Nonnegative int codeIndex, @Nonnegative int parameterIndex) {
      int annotationCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      while (annotationCount > 0) {
         String desc = readUTF8(codeIndex);

         //noinspection ConstantConditions
         AnnotationVisitor av = mv.visitParameterAnnotation(parameterIndex, desc);
         codeIndex = annotationReader.readNamedAnnotationValues(codeIndex + 2, av);

         annotationCount--;
      }

      return codeIndex;
   }

   private void readMethodCode() {
      if (bodyStartCodeIndex != 0 && readCode) {
         readCode();
      }
   }

   private void readCode() {
      // Reads the header.
      int codeIndex = bodyStartCodeIndex;
      int maxStack = readUnsignedShort(codeIndex);
      int codeLength = readInt(codeIndex + 4);
      codeIndex += 8;

      // Reads the bytecode to find the labels.
      int codeStart = codeIndex;
      int codeEnd = codeIndex + codeLength;

      labels = new Label[codeLength + 2];

      codeIndex = readAllLabelsInCodeBlock(codeIndex, codeLength, codeStart, codeEnd);

      // Reads the try catch entries to find the labels, and also visits them.
      codeIndex = readTryCatchBlocks(codeIndex);

      codeIndex += 2;

      // Reads the code attributes.
      int varTable = 0;
      int varTypeTable = 0;

      for (int attributeCount = readUnsignedShort(codeIndex); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(codeIndex + 2);

         if ("LocalVariableTable".equals(attrName)) {
            varTable = readLocalVariableTable(codeIndex, varTable);
         }
         else if ("LocalVariableTypeTable".equals(attrName)) {
            varTypeTable = codeIndex + 8;
         }
         else if ("LineNumberTable".equals(attrName)) {
            readLineNumberTable(codeIndex);
         }

         codeIndex += 6 + readInt(codeIndex + 4);
      }

      readBytecodeInstructionsInCodeBlock(codeStart, codeEnd);
      readEndLabel(codeLength);
      readLocalVariableTables(varTable, varTypeTable);
      mv.visitMaxStack(maxStack);
   }

   @Nonnegative
   private int readAllLabelsInCodeBlock(
      @Nonnegative int codeIndex, @Nonnegative int codeLength, @Nonnegative int codeStart, @Nonnegative int codeEnd
   ) {
      readLabel(codeLength + 1);

      while (codeIndex < codeEnd) {
         int offset = codeIndex - codeStart;
         codeIndex = readLabelForInstructionIfAny(codeIndex, offset);
      }

      return codeIndex;
   }

   @Nonnegative
   private int readLabelForInstructionIfAny(@Nonnegative int codeIndex, @Nonnegative int offset) {
      int opcode = readByte(codeIndex);
      byte instructionType = INSTRUCTION_TYPE[opcode];
      boolean tablInsn = instructionType == TABL_INSN;

      if (tablInsn || instructionType == LOOK_INSN) {
         return readSwitchInstruction(codeIndex, offset, tablInsn);
      }

      int codeOffset = readNonSwitchInstruction(codeIndex, offset, instructionType);
      return codeIndex + codeOffset;
   }

   @Nonnegative
   private int readSwitchInstruction(@Nonnegative int codeIndex, @Nonnegative int offset, boolean tableNotLookup) {
      // Skips 0 to 3 padding bytes.
      codeIndex = codeIndex + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(codeIndex));

      int caseCount = tableNotLookup ? readInt(codeIndex + 8) - readInt(codeIndex + 4) + 1 : readInt(codeIndex + 4);
      int offsetStep = tableNotLookup ? 4 : 8;

      while (caseCount > 0) {
         int caseOffset = readInt(codeIndex + 12);
         readLabel(offset + caseOffset);
         codeIndex += offsetStep;
         caseCount--;
      }

      return codeIndex + 8;
   }

   /**
    * Returns the label corresponding to the given offset. Creates a label for the given offset if it has not been
    * already created.
    *
    * @param offset a bytecode offset in a method.
    * @return a <tt>Label</tt>, which must be equal to <tt>labels[offset]</tt>.
    */
   @Nonnull
   private Label readLabel(@Nonnegative int offset) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         labels[offset] = label;
      }

      return label;
   }

   /**
    * @return the offset for codeIndex
    */
   @Nonnegative
   private int readNonSwitchInstruction(@Nonnegative int codeIndex, @Nonnegative int offset, byte instructionType) {
      switch (instructionType) {
         case NOARG: case IMPLVAR: return 1;
         case LABEL:  readLabel(offset + readShort(codeIndex + 1)); return 3;
         case LABELW: readLabel(offset + readInt(codeIndex + 1));   return 5;
         case WIDE_INSN: int opcode = readByte(codeIndex + 1); return opcode == IINC ? 6 : 4;
         case VAR: case SBYTE: case LDC_INSN: return 2;
         case SHORT: case LDCW_INSN: case FIELDORMETH: case TYPE_INSN: case IINC_INSN: return 3; case ITFMETH:
         case INDYMETH: return 5;
         case MANA_INSN: default: return 4;
      }
   }

   // Reads the try catch entries to find the labels, and also visits them.
   @Nonnegative
   private int readTryCatchBlocks(@Nonnegative int codeIndex) {
      for (int blockCount = readUnsignedShort(codeIndex); blockCount > 0; blockCount--) {
         Label start   = readLabel(readUnsignedShort(codeIndex + 2));
         Label end     = readLabel(readUnsignedShort(codeIndex + 4));
         Label handler = readLabel(readUnsignedShort(codeIndex + 6));
         String type = readUTF8(items[readUnsignedShort(codeIndex + 8)]);

         mv.visitTryCatchBlock(start, end, handler, type);
         codeIndex += 8;
      }

      return codeIndex;
   }

   @Nonnegative
   private int readLocalVariableTable(@Nonnegative int codeIndex, @Nonnegative int varTable) {
      if (readDebugInfo) {
         varTable = codeIndex + 8;

         for (int localVarCount = readUnsignedShort(codeIndex + 8), v = codeIndex; localVarCount > 0; localVarCount--) {
            int labelOffset = readUnsignedShort(v + 10);
            readDebugLabel(labelOffset);

            labelOffset += readUnsignedShort(v + 12);
            readDebugLabel(labelOffset);

            v += 10;
         }
      }

      return varTable;
   }

   @Nonnull
   private Label readDebugLabel(@Nonnegative int offset) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         label.markAsDebug();
         labels[offset] = label;
      }

      return label;
   }

   private void readLineNumberTable(@Nonnegative int codeIndex) {
      if (readDebugInfo) {
         for (int lineCount = readUnsignedShort(codeIndex + 8); lineCount > 0; lineCount--) {
            int labelOffset = readUnsignedShort(codeIndex + 10);
            Label debugLabel = readDebugLabel(labelOffset);

            debugLabel.line = readUnsignedShort(codeIndex + 12);
            codeIndex += 4;
         }
      }
   }

   @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   private void readBytecodeInstructionsInCodeBlock(@Nonnegative int codeStart, @Nonnegative int codeEnd) {
      int codeIndex = codeStart;

      while (codeIndex < codeEnd) {
         int offset = codeIndex - codeStart;
         readLabelAndLineNumber(offset);

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
      int cpIndex = items[readUnsignedShort(codeIndex + 1)];
      int bsmStartIndex = readUnsignedShort(cpIndex);
      @SuppressWarnings("ConstantConditions") int bsmIndex = bootstrapMethods[bsmStartIndex];
      Handle bsm = (Handle) readConstItem(bsmIndex);
      int bsmArgCount = readUnsignedShort(bsmIndex + 2);
      Object[] bsmArgs = new Object[bsmArgCount];
      bsmIndex += 4;

      for (int i = 0; i < bsmArgCount; i++) {
         bsmArgs[i] = readConstItem(bsmIndex);
         bsmIndex += 2;
      }

      cpIndex = items[readUnsignedShort(cpIndex + 2)];
      String name = readUTF8(cpIndex);
      String desc = readUTF8(cpIndex + 2);

      //noinspection ConstantConditions
      mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      codeIndex += 5;

      return codeIndex;
   }

   @Nonnegative
   private int readTypeInsn(@Nonnegative int codeIndex, int opcode) {
      String typeDesc = readClass(codeIndex + 1);
      //noinspection ConstantConditions
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
      String arrayTypeDesc = readClass(codeIndex + 1);
      int dims = readByte(codeIndex + 3);

      //noinspection ConstantConditions
      mv.visitMultiANewArrayInsn(arrayTypeDesc, dims);

      return codeIndex + 4;
   }

   // Visits the label and line number for this offset, if any.
   private void readLabelAndLineNumber(@Nonnegative int offset) {
      Label label = labels[offset];

      if (label != null) {
         mv.visitLabel(label);

         if (readDebugInfo && label.line > 0) {
            mv.visitLineNumber(label.line, label);
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
      int cpIndex1 = items[readUnsignedShort(codeIndex + 1)];
      String owner = readClass(cpIndex1);
      int cpIndex2 = items[readUnsignedShort(cpIndex1 + 2)];
      String name = readUTF8(cpIndex2);
      String desc = readUTF8(cpIndex2 + 2);

      if (opcode < INVOKEVIRTUAL) {
         //noinspection ConstantConditions
         mv.visitFieldInsn(opcode, owner, name, desc);
      }
      else {
         boolean itf = code[cpIndex1 - 1] == ConstantPoolItemType.IMETH;
         //noinspection ConstantConditions
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

   private void readLocalVariableTables(@Nonnegative int varTable, @Nonnegative int varTypeTable) {
      if (varTable == 0 || !readDebugInfo) {
         return;
      }

      int[] typeTable = null;

      if (varTypeTable != 0) {
         typeTable = readLocalVariableTypeTable(varTypeTable);
      }

      int codeIndex = varTable + 2;

      for (int localVarCount = readUnsignedShort(varTable); localVarCount > 0; localVarCount--) {
         int start  = readUnsignedShort(codeIndex);
         int length = readUnsignedShort(codeIndex + 2);
         int index  = readUnsignedShort(codeIndex + 8);
         String signature = getLocalVariableSignature(typeTable, start, index);

         String name = readUTF8(codeIndex + 4);
         String desc = readUTF8(codeIndex + 6);
         Label startLabel = labels[start];
         Label endLabel   = labels[start + length];

         //noinspection ConstantConditions
         mv.visitLocalVariable(name, desc, signature, startLabel, endLabel, index);

         codeIndex += 10;
      }
   }

   @Nullable
   private String getLocalVariableSignature(@Nullable int[] typeTable, @Nonnegative int start, @Nonnegative int index) {
      if (typeTable != null) {
         for (int i = 0, n = typeTable.length; i < n; i += 3) {
            if (typeTable[i] == start && typeTable[i + 1] == index) {
               String signature = readUTF8(typeTable[i + 2]);
               return signature;
            }
         }
      }

      return null;
   }

   @Nonnull
   private int[] readLocalVariableTypeTable(@Nonnegative int varTypeTable) {
      int codeIndex = varTypeTable + 2;
      int typeTableSize = readUnsignedShort(varTypeTable) * 3;
      int[] typeTable = new int[typeTableSize];
      int typeTableIndex = typeTableSize;

      while (typeTableIndex > 0) {
         typeTable[--typeTableIndex] = codeIndex + 6;                    // signature
         typeTable[--typeTableIndex] = readUnsignedShort(codeIndex + 8); // index
         typeTable[--typeTableIndex] = readUnsignedShort(codeIndex);     // start
         codeIndex += 10;
      }

      return typeTable;
   }
}
