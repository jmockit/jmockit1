package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.MethodReader.InstructionType.*;
import static mockit.external.asm.MethodReader.InstructionType.IINC;
import static mockit.external.asm.Opcodes.*;

final class MethodReader extends AnnotatedReader
{
   interface InstructionType
   {
      int NOARG       = 0; // instructions without any argument
      int SBYTE       = 1; // instructions with a signed byte argument
      int SHORT       = 2; // instructions with a signed short argument
      int VAR         = 3; // instructions with a local variable index argument
      int IMPLVAR     = 4; // instructions with an implicit local variable index argument
      int TYPE        = 5; // instructions with a type descriptor argument
      int FIELDORMETH = 6; // field and method invocations instructions
      int ITFMETH     = 7; // INVOKEINTERFACE/INVOKEDYNAMIC instruction
      int INDYMETH    = 8; // INVOKEDYNAMIC instruction
      int LABEL       = 9; // instructions with a 2 bytes bytecode offset label
      int LABELW     = 10; // instructions with a 4 bytes bytecode offset label
      int LDC        = 11; // the LDC instruction
      int LDCW       = 12; // the LDC_W and LDC2_W instructions
      int IINC       = 13; // the IINC instruction
      int TABL       = 14; // the TABLESWITCH instruction
      int LOOK       = 15; // the LOOKUPSWITCH instruction
      @SuppressWarnings("unused") int MANA_INSN  = 16; // the MULTIANEWARRAY instruction
      int WIDE       = 17; // the WIDE instruction
   }

   /**
    * The instruction types of all JVM opcodes.
    */
   private static final byte[] TYPE = new byte[220];
   static {
      String s =
         "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD" +
         "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
         "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA" +
         "AAAAGGGGGGGHIFBFAAFFAARQJJKKJJJJJJJJJJJJJJJJJJ";
      byte[] b = TYPE;

      for (int i = 0; i < b.length; i++) {
         b[i] = (byte) (s.charAt(i) - 'A');
      }
   }

   @Nonnull private final ClassVisitor cv;

   /**
    * The start index of each bootstrap method.
    */
   @Nullable private final int[] bootstrapMethods;

   private final boolean readCode;
   private final boolean readDebugInfo;

   @Nullable private String[] exceptions;

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
    * @param u the start offset of the method in the class file.
    * @return the offset of the first byte following the method in the class.
    */
   @Nonnegative
   private int readMethod(@Nonnegative int u) {
      u = readMethodDeclaration(u);

      int u0 = u;
      int code = 0;
      int exception = 0;
      exceptions = null;
      String signature = null;
      int anns = 0;
      int annDefault = 0;
      int paramAnns = 0;
      char[] c = buf;

      for (int i = readUnsignedShort(u); i > 0; i--) {
         String attrName = readUTF8(u + 2, c);

         if ("Code".equals(attrName)) {
            code = u + 8;
         }
         else if ("Exceptions".equals(attrName)) {
            exception = readExceptionsInThrowsClause(u);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(u + 8, c);
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            anns = u + 8;
         }
         else if ("AnnotationDefault".equals(attrName)) {
            annDefault = u + 8;
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
            paramAnns = u + 8;
         }

         u += 6 + readInt(u + 4);
      }

      u += 2;
      readMethodBody(u0, u, code, exception, signature, anns, annDefault, paramAnns);
      return u;
   }

   @Nonnegative
   private int readMethodDeclaration(@Nonnegative int u) {
      char[] c = buf;
      access = readUnsignedShort(u);
      name = readUTF8(u + 2, c);
      desc = readUTF8(u + 4, c);
      return u + 6;
   }

   @Nonnegative
   private int readExceptionsInThrowsClause(@Nonnegative int u) {
      int n = readUnsignedShort(u + 8);
      exceptions = new String[n];
      int exception = u + 10;

      for (int i = 0; i < n; i++) {
         exceptions[i] = readClass(exception, buf);
         exception += 2;
      }

      return exception;
   }

   private void readMethodBody(
      @Nonnegative int u0, @Nonnegative int u, @Nonnegative int code, @Nonnegative int exception,
      @Nullable String signature, @Nonnegative int anns, @Nonnegative int annDefault, @Nonnegative int paramAnns
   ) {
      mv = cv.visitMethod(access, name, desc, signature, exceptions);

      if (mv == null || mv instanceof MethodWriter && copyMethodBody(u, exception, signature, u0)) {
         return;
      }

      readAnnotationDefaultValue(annDefault);
      readAnnotationValues(anns);
      readParameterAnnotations(paramAnns);
      readMethodCode(code);
      mv.visitEnd();
   }

   /**
    * If the returned MethodVisitor is in fact a MethodWriter, it means there is no method adapter between the reader
    * and the writer.
    * In addition, it's assumed that the writer's constant pool was copied from this reader (mw.cw.cr == this.cr), and
    * the signature of the method has not been changed; then, we skip all visit events and just copy the original code
    * of the method to the writer (the access, name and descriptor can have been changed, this is not important since
    * they are not copied as is from the reader).
    *
    * @return <tt>true</tt> if the method body can be copied, or <tt>false</tt> if it can't because the method's
    * <tt>throws</tt> clause was changed
    */
   private boolean copyMethodBody(
      @Nonnegative int u, @Nonnegative int exception, @Nullable String signature, @Nonnegative int firstAttribute
   ) {
      MethodWriter mw = (MethodWriter) mv;
      ThrowsClause throwsClause = mw.throwsClause;
      int exceptionCount = throwsClause.getExceptionCount();
      boolean sameExceptions = false;

      if (exceptions == null) {
         sameExceptions = exceptionCount == 0;
      }
      else if (exceptions.length == exceptionCount) {
         sameExceptions = true;

         for (int j = exceptionCount - 1; j >= 0; j--) {
            exception -= 2;
            int exceptionIndex = readUnsignedShort(exception);

            if (throwsClause.getExceptionIndex(j) != exceptionIndex) {
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
         mw.classReaderOffset = firstAttribute;
         mw.classReaderLength = u - firstAttribute;
         return true;
      }

      return false;
   }

   private void readAnnotationDefaultValue(@Nonnegative int annotationDefault) {
      if (annotationDefault != 0) {
         AnnotationVisitor dv = mv.visitAnnotationDefault();
         annotationReader.readAnnotationValue(annotationDefault, buf, null, dv);

         if (dv != null) {
            dv.visitEnd();
         }
      }
   }

   private void readAnnotationValues(@Nonnegative int anns) {
      if (anns != 0) {
         for (int valueCount = readUnsignedShort(anns), v = anns + 2; valueCount > 0; valueCount--) {
            String desc = readUTF8(v, buf);
            @SuppressWarnings("ConstantConditions") AnnotationVisitor av = mv.visitAnnotation(desc);
            v = annotationReader.readAnnotationValues(v + 2, buf, true, av);
         }
      }
   }

   /**
    * Reads parameter annotations and makes the given visitor visit them.
    *
    * @param v start offset in {@link #code} of the annotations to be read.
    */
   private void readParameterAnnotations(@Nonnegative int v) {
      if (v != 0) {
         char[] c = buf;
         int parameters = code[v++] & 0xFF;
         AnnotationVisitor av;

         for (int i = 0; i < parameters; i++) {
            int j = readUnsignedShort(v);
            v += 2;

            for (; j > 0; j--) {
               String desc = readUTF8(v, c);
               //noinspection ConstantConditions
               av = mv.visitParameterAnnotation(i, desc);
               v = annotationReader.readAnnotationValues(v + 2, c, true, av);
            }
         }
      }
   }

   private void readMethodCode(@Nonnegative int code) {
      if (code != 0 && readCode) {
         mv.visitCode();
         readCode(code);
      }
   }

   /**
    * Reads the bytecode of a method and makes the given visitor visit it.
    *
    * @param u the start offset of the code attribute in the class file.
    */
   private void readCode(@Nonnegative int u) {
      // Reads the header.
      int maxStack = readUnsignedShort(u);
      int codeLength = readInt(u + 4);
      u += 8;

      // Reads the bytecode to find the labels.
      int codeStart = u;
      int codeEnd = u + codeLength;

      labels = new Label[codeLength + 2];

      u = readAllLabelsInCodeBlock(u, codeLength, codeStart, codeEnd);

      // Reads the try catch entries to find the labels, and also visits them.
      u = readTryCatchBlocks(u);

      u += 2;

      // Reads the code attributes.
      int varTable = 0;
      int varTypeTable = 0;

      for (int attributeCount = readUnsignedShort(u); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(u + 2, buf);

         if ("LocalVariableTable".equals(attrName)) {
            varTable = readLocalVariableTable(u, varTable);
         }
         else if ("LocalVariableTypeTable".equals(attrName)) {
            varTypeTable = u + 8;
         }
         else if ("LineNumberTable".equals(attrName)) {
            readLineNumberTable(u);
         }

         u += 6 + readInt(u + 4);
      }

      readBytecodeInstructionsInCodeBlock(codeStart, codeEnd);

      Label label = labels[codeLength];

      if (label != null) {
         mv.visitLabel(label);
      }

      if (varTable != 0 && readDebugInfo) {
         readLocalVariableTables(varTable, varTypeTable);
      }

      mv.visitMaxStack(maxStack);
   }

   @Nonnegative
   private int readAllLabelsInCodeBlock(
      @Nonnegative int u, @Nonnegative int codeLength, @Nonnegative int codeStart, @Nonnegative int codeEnd
   ) {
      byte[] b = code;

      readLabel(codeLength + 1);

      while (u < codeEnd) {
         int offset = u - codeStart;
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
            case IMPLVAR:
               u++;
               break;
            case LABEL:
               readLabel(offset + readShort(u + 1));
               u += 3;
               break;
            case LABELW:
               readLabel(offset + readInt(u + 1));
               u += 5;
               break;
            case InstructionType.WIDE:
               opcode = b[u + 1] & 0xFF;
               u += opcode == IINC ? 6 : 4;
               break;
            case TABL:
               u = readSwitchInstruction(u, offset, true);
               break;
            case LOOK:
               u = readSwitchInstruction(u, offset, false);
               break;
            case VAR:
            case SBYTE:
            case InstructionType.LDC:
               u += 2;
               break;
            case SHORT:
            case LDCW:
            case FIELDORMETH:
            case InstructionType.TYPE:
            case IINC:
               u += 3;
               break;
            case ITFMETH:
            case INDYMETH:
               u += 5;
               break;
            // case MANA_INSN:
            default:
               u += 4;
               break;
         }
      }

      return u;
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

   // Reads the try catch entries to find the labels, and also visits them.
   @Nonnegative
   private int readTryCatchBlocks(@Nonnegative int u) {
      for (int blockCount = readUnsignedShort(u); blockCount > 0; blockCount--) {
         Label start = readLabel(readUnsignedShort(u + 2));
         Label end = readLabel(readUnsignedShort(u + 4));
         Label handler = readLabel(readUnsignedShort(u + 6));
         String type = readUTF8(items[readUnsignedShort(u + 8)], buf);

         mv.visitTryCatchBlock(start, end, handler, type);
         u += 8;
      }

      return u;
   }

   @Nonnegative
   private int readLocalVariableTable(@Nonnegative int u, @Nonnegative int varTable) {
      if (readDebugInfo) {
         varTable = u + 8;

         for (int localVarCount = readUnsignedShort(u + 8), v = u; localVarCount > 0; localVarCount--) {
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
         label = readLabel(offset);
         label.markAsDebug();
      }

      return label;
   }

   private void readLineNumberTable(@Nonnegative int u) {
      if (readDebugInfo) {
         for (int lineCount = readUnsignedShort(u + 8), v = u; lineCount > 0; lineCount--) {
            int labelOffset = readUnsignedShort(v + 10);
            Label debugLabel = readDebugLabel(labelOffset);

            debugLabel.line = readUnsignedShort(v + 12);
            v += 4;
         }
      }
   }

   private void readBytecodeInstructionsInCodeBlock(@Nonnegative int codeStart, @Nonnegative int codeEnd) {
      char[] c = buf;
      byte[] b = code;
      int u = codeStart;

      while (u < codeEnd) {
         int offset = u - codeStart;
         readLabelAndLineNumber(offset);

         // Visits the instruction at this offset.
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
               mv.visitInsn(opcode);
               u++;
               break;
            case IMPLVAR:
               readImplicitVarInstruction(opcode);
               u++;
               break;
            case LABEL:
               Label targetLabel = labels[offset + readShort(u + 1)];
               mv.visitJumpInsn(opcode, targetLabel);
               u += 3;
               break;
            case LABELW:
               Label targetLabelW = labels[offset + readInt(u + 1)];
               mv.visitJumpInsn(opcode - 33, targetLabelW);
               u += 5;
               break;
            case InstructionType.WIDE:
               u = readWideInstruction(u);
               break;
            case TABL:
               u = readTableSwitchInstruction(u, offset);
               break;
            case LOOK:
               u = readLookupSwitchInstruction(u, offset);
               break;
            case VAR:
               int var = b[u + 1] & 0xFF;
               mv.visitVarInsn(opcode, var);
               u += 2;
               break;
            case SBYTE:
               byte byteOperand = b[u + 1];
               mv.visitIntInsn(opcode, byteOperand);
               u += 2;
               break;
            case SHORT:
               int shortOperand = readShort(u + 1);
               mv.visitIntInsn(opcode, shortOperand);
               u += 3;
               break;
            case InstructionType.LDC:
               Object cst = readConst(b[u + 1] & 0xFF, c);
               mv.visitLdcInsn(cst);
               u += 2;
               break;
            case LDCW:
               Object cstWide = readConst(readUnsignedShort(u + 1), c);
               mv.visitLdcInsn(cstWide);
               u += 3;
               break;
            case FIELDORMETH:
            case ITFMETH:
               readFieldOrInvokeInstruction(u, opcode);
               u += opcode == INVOKEINTERFACE ? 5 : 3;
               break;
            case INDYMETH:
               //noinspection ConstantConditions
               u = readInvokeDynamicInstruction(u);
               break;
            case InstructionType.TYPE:
               String typeDesc = readClass(u + 1, c);
               //noinspection ConstantConditions
               mv.visitTypeInsn(opcode, typeDesc);
               u += 3;
               break;
            case IINC:
               int incCar = b[u + 1] & 0xFF;
               byte increment = b[u + 2];
               mv.visitIincInsn(incCar, increment);
               u += 3;
               break;
            // case MANA_INSN:
            default:
               String arrayTypeDesc = readClass(u + 1, c);
               int dims = b[u + 3] & 0xFF;
               //noinspection ConstantConditions
               mv.visitMultiANewArrayInsn(arrayTypeDesc, dims);
               u += 4;
               break;
         }
      }
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
   private int readWideInstruction(@Nonnegative int u) {
      int opcode = code[u + 1] & 0xFF;
      int var = readUnsignedShort(u + 2);

      if (opcode == IINC) {
         int increment = readShort(u + 4);
         mv.visitIincInsn(var, increment);
         u += 6;
      }
      else {
         mv.visitVarInsn(opcode, var);
         u += 4;
      }

      return u;
   }

   @Nonnegative
   private int readTableSwitchInstruction(@Nonnegative int u, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      int dfltLabelOffset = offset + readInt(u);
      int min = readInt(u + 4);
      int max = readInt(u + 8);
      Label[] table = new Label[max - min + 1];
      u += 12;

      for (int i = 0; i < table.length; i++) {
         int handlerLabelOffset = offset + readInt(u);
         table[i] = labels[handlerLabelOffset];
         u += 4;
      }

      Label dfltLabel = labels[dfltLabelOffset];
      mv.visitTableSwitchInsn(min, max, dfltLabel, table);
      return u;
   }

   @Nonnegative
   private int readLookupSwitchInstruction(@Nonnegative int u, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads the instruction.
      int dfltLabelOffset = offset + readInt(u);
      int len = readInt(u + 4);
      int[] keys = new int[len];
      Label[] values = new Label[len];
      u += 8;

      for (int i = 0; i < len; i++) {
         keys[i] = readInt(u);
         int handlerLabelOffset = offset + readInt(u + 4);
         values[i] = labels[handlerLabelOffset];
         u += 8;
      }

      Label dfltLabel = labels[dfltLabelOffset];
      mv.visitLookupSwitchInsn(dfltLabel, keys, values);
      return u;
   }

   private void readFieldOrInvokeInstruction(@Nonnegative int u, int opcode) {
      int cpIndex1 = items[readUnsignedShort(u + 1)];
      @Nonnull char[] c = buf;
      String owner = readClass(cpIndex1, c);
      int cpIndex2 = items[readUnsignedShort(cpIndex1 + 2)];
      String name = readUTF8(cpIndex2, c);
      String desc = readUTF8(cpIndex2 + 2, c);

      if (opcode < INVOKEVIRTUAL) {
         //noinspection ConstantConditions
         mv.visitFieldInsn(opcode, owner, name, desc);
      }
      else {
         boolean itf = code[cpIndex1 - 1] == ConstantPoolItemType.IMETH;
         //noinspection ConstantConditions
         mv.visitMethodInsn(opcode, owner, name, desc, itf);
      }
   }

   private void readLocalVariableTables(@Nonnegative int varTable, @Nonnegative int varTypeTable) {
      char[] c = buf;
      int[] typeTable = null;
      int u;

      if (varTypeTable != 0) {
         u = varTypeTable + 2;
         typeTable = new int[readUnsignedShort(varTypeTable) * 3];

         for (int i = typeTable.length; i > 0; ) {
            typeTable[--i] = u + 6; // signature
            typeTable[--i] = readUnsignedShort(u + 8); // index
            typeTable[--i] = readUnsignedShort(u); // start
            u += 10;
         }
      }

      u = varTable + 2;

      for (int i = readUnsignedShort(varTable); i > 0; i--) {
         int start = readUnsignedShort(u);
         int length = readUnsignedShort(u + 2);
         int index = readUnsignedShort(u + 8);
         String signature = null;

         if (typeTable != null) {
            for (int j = 0; j < typeTable.length; j += 3) {
               if (typeTable[j] == start && typeTable[j + 1] == index) {
                  signature = readUTF8(typeTable[j + 2], c);
                  break;
               }
            }
         }

         String name = readUTF8(u + 4, c);
         String desc = readUTF8(u + 6, c);
         Label startLabel = labels[start];
         Label endLabel = labels[start + length];
         u += 10;

         //noinspection ConstantConditions
         mv.visitLocalVariable(name, desc, signature, startLabel, endLabel, index);
      }
   }

   @Nonnegative
   private int readInvokeDynamicInstruction(@Nonnegative int u) {
      int cpIndex = items[readUnsignedShort(u + 1)];
      int bsmStartIndex = readUnsignedShort(cpIndex);
      @SuppressWarnings("ConstantConditions") int bsmIndex = bootstrapMethods[bsmStartIndex];
      char[] c = buf;
      Handle bsm = (Handle) readConst(readUnsignedShort(bsmIndex), c);
      int bsmArgCount = readUnsignedShort(bsmIndex + 2);
      Object[] bsmArgs = new Object[bsmArgCount];
      bsmIndex += 4;

      for (int i = 0; i < bsmArgCount; i++) {
         bsmArgs[i] = readConst(readUnsignedShort(bsmIndex), c);
         bsmIndex += 2;
      }

      cpIndex = items[readUnsignedShort(cpIndex + 2)];
      String name = readUTF8(cpIndex, c);
      String desc = readUTF8(cpIndex + 2, c);

      //noinspection ConstantConditions
      mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      u += 5;

      return u;
   }

   @Nonnegative
   private int readSwitchInstruction(@Nonnegative int u, @Nonnegative int offset, boolean tableNotLookup) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(u));

      int caseCount = tableNotLookup ? readInt(u + 8) - readInt(u + 4) + 1 : readInt(u + 4);
      int offsetStep = tableNotLookup ? 4 : 8;

      while (caseCount > 0) {
         int caseOffset = readInt(u + 12);
         readLabel(offset + caseOffset);
         u += offsetStep;
         caseCount--;
      }

      return u + 8;
   }
}
