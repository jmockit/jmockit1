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
   @Nullable private String[] exceptions;

   MethodReader(@Nonnull ClassReader cr, @Nonnull ClassVisitor cv) {
      super(cr);
      this.cv = cv;
   }

   /**
    * Reads a method and makes the given visitor visit it.
    *
    * @param context information about the class being parsed.
    * @param u       the start offset of the method in the class file.
    * @return the offset of the first byte following the method in the class.
    */
   @Nonnegative
   int readMethod(@Nonnull Context context, @Nonnegative int u) {
      u = readMethodDeclaration(context, u);

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
            exception = readExceptionsInThrowsClause(u, c);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(u + 8, c);
         }
         else if ("Deprecated".equals(attrName)) {
            context.access = Access.asDeprecated(context.access);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            anns = u + 8;
         }
         else if ("AnnotationDefault".equals(attrName)) {
            annDefault = u + 8;
         }
         else if ("Synthetic".equals(attrName)) {
            context.access = Access.asSynthetic(context.access);
         }
         else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
            paramAnns = u + 8;
         }

         u += 6 + readInt(u + 4);
      }

      u += 2;
      readMethodBody(context, u0, u, code, exception, signature, anns, annDefault, paramAnns);
      return u;
   }

   @Nonnegative
   private int readMethodDeclaration(@Nonnull Context context, @Nonnegative int u) {
      char[] c = buf;
      context.access = readUnsignedShort(u);
      context.name = readUTF8(u + 2, c);
      context.desc = readUTF8(u + 4, c);
      return u + 6;
   }

   @Nonnegative
   private int readExceptionsInThrowsClause(@Nonnegative int u, @Nonnull char[] c) {
      int n = readUnsignedShort(u + 8);
      exceptions = new String[n];
      int exception = u + 10;

      for (int j = 0; j < n; j++) {
         exceptions[j] = readClass(exception, c);
         exception += 2;
      }

      return exception;
   }

   private void readMethodBody(
      @Nonnull Context context, @Nonnegative int u0, @Nonnegative int u, int code, int exception,
      @Nullable String signature, @Nonnegative int anns, @Nonnegative int annDefault, @Nonnegative int paramAnns
   ) {
      MethodVisitor mv = cv.visitMethod(context.access, context.name, context.desc, signature, exceptions);

      if (mv == null || mv instanceof MethodWriter && copyMethodBody((MethodWriter) mv, u, exception, signature, u0)) {
         return;
      }

      char[] c = buf;
      readAnnotationDefaultValue(mv, c, annDefault);
      readAnnotationValues(mv, c, anns);
      readParameterAnnotations(mv, context, paramAnns);
      readMethodCode(mv, context, code);
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
      @Nonnull MethodWriter mw, @Nonnegative int u, int exception, @Nullable String signature,
      @Nonnegative int firstAttribute
   ) {
      boolean sameExceptions = false;
      ThrowsClause throwsClause = mw.throwsClause;
      int exceptionCount = throwsClause.getExceptionCount();

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

   private void readAnnotationDefaultValue(
      @Nonnull MethodVisitor mv, @Nonnull char[] c, @Nonnegative int annotationDefault
   ) {
      if (annotationDefault != 0) {
         AnnotationVisitor dv = mv.visitAnnotationDefault();
         annotationReader.readAnnotationValue(annotationDefault, c, null, dv);

         if (dv != null) {
            dv.visitEnd();
         }
      }
   }

   private void readAnnotationValues(@Nonnull MethodVisitor mv, @Nonnull char[] c, @Nonnegative int anns) {
      if (anns != 0) {
         for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; i--) {
            String desc = readUTF8(v, c);
            @SuppressWarnings("ConstantConditions") AnnotationVisitor av = mv.visitAnnotation(desc);
            v = annotationReader.readAnnotationValues(v + 2, c, true, av);
         }
      }
   }

   /**
    * Reads parameter annotations and makes the given visitor visit them.
    *
    * @param mv      the visitor that must visit the annotations.
    * @param context information about the class being parsed.
    * @param v       start offset in {@link #code} of the annotations to be read.
    */
   private void readParameterAnnotations(@Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int v) {
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

   private void readMethodCode(@Nonnull MethodVisitor mv, @Nonnull Context context, int code) {
      if (code != 0 && context.readCode()) {
         mv.visitCode();
         readCode(mv, context, code);
      }
   }

   /**
    * Reads the bytecode of a method and makes the given visitor visit it.
    *
    * @param mv      the visitor that must visit the method's code.
    * @param context information about the class being parsed.
    * @param u       the start offset of the code attribute in the class file.
    */
   private void readCode(@Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int u) {
      // Reads the header.
      int maxStack = readUnsignedShort(u);
      int codeLength = readInt(u + 4);
      u += 8;

      // Reads the bytecode to find the labels.
      int codeStart = u;
      int codeEnd = u + codeLength;

      Label[] labels = new Label[codeLength + 2];
      context.labels = labels;

      u = readAllLabelsInCodeBlock(context, u, codeLength, codeStart, codeEnd);

      // Reads the try catch entries to find the labels, and also visits them.
      u = readTryCatchBlocks(mv, context, u);

      u += 2;

      // Reads the code attributes.
      int varTable = 0;
      int varTypeTable = 0;

      for (int attributeCount = readUnsignedShort(u); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(u + 2, buf);

         if ("LocalVariableTable".equals(attrName)) {
            varTable = readLocalVariableTable(context, u, varTable);
         }
         else if ("LocalVariableTypeTable".equals(attrName)) {
            varTypeTable = u + 8;
         }
         else if ("LineNumberTable".equals(attrName)) {
            readLineNumberTable(context, u);
         }

         u += 6 + readInt(u + 4);
      }

      readBytecodeInstructionsInCodeBlock(mv, context, codeStart, codeEnd);

      Label label = labels[codeLength];

      if (label != null) {
         mv.visitLabel(label);
      }

      if (varTable != 0 && context.readDebugInfo()) {
         readLocalVariableTables(mv, context, varTable, varTypeTable);
      }

      mv.visitMaxStack(maxStack);
   }

   @Nonnegative
   private int readAllLabelsInCodeBlock(
      @Nonnull Context context, @Nonnegative int u, @Nonnegative int codeLength,
      @Nonnegative int codeStart, @Nonnegative int codeEnd
   ) {
      byte[] b = this.code;
      Label[] labels = context.labels;

      readLabel(codeLength + 1, labels);

      while (u < codeEnd) {
         int offset = u - codeStart;
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
            case IMPLVAR:
               u++;
               break;
            case LABEL:
               readLabel(offset + readShort(u + 1), labels);
               u += 3;
               break;
            case LABELW:
               readLabel(offset + readInt(u + 1), labels);
               u += 5;
               break;
            case InstructionType.WIDE:
               opcode = b[u + 1] & 0xFF;
               u += opcode == IINC ? 6 : 4;
               break;
            case TABL:
               u = readTableSwitchInstruction(labels, u, offset);
               break;
            case LOOK:
               u = readLookupSwitchInstruction(labels, u, offset);
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
    * @param labels the already created labels, indexed by their offset.
    * @return a non null Label, which must be equal to labels[offset].
    */
   @Nonnull
   private static Label readLabel(@Nonnegative int offset, @Nonnull Label[] labels) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         labels[offset] = label;
      }

      return label;
   }

   // Reads the try catch entries to find the labels, and also visits them.
   @Nonnegative
   private int readTryCatchBlocks(@Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int u) {
      Label[] labels = context.labels;

      for (int blockCount = readUnsignedShort(u); blockCount > 0; blockCount--) {
         Label start = readLabel(readUnsignedShort(u + 2), labels);
         Label end = readLabel(readUnsignedShort(u + 4), labels);
         Label handler = readLabel(readUnsignedShort(u + 6), labels);
         String type = readUTF8(items[readUnsignedShort(u + 8)], buf);

         mv.visitTryCatchBlock(start, end, handler, type);
         u += 8;
      }

      return u;
   }

   @Nonnegative
   private int readLocalVariableTable(@Nonnull Context context, @Nonnegative int u, @Nonnegative int varTable) {
      if (context.readDebugInfo()) {
         Label[] labels = context.labels;
         varTable = u + 8;

         for (int j = readUnsignedShort(u + 8), v = u; j > 0; j--) {
            int label = readUnsignedShort(v + 10);

            if (labels[label] == null) {
               readDebugLabel(label, labels);
            }

            label += readUnsignedShort(v + 12);

            if (labels[label] == null) {
               readDebugLabel(label, labels);
            }

            v += 10;
         }
      }

      return varTable;
   }

   private static void readDebugLabel(@Nonnegative int index, @Nonnull Label[] labels) {
      Label label = readLabel(index, labels);
      label.markAsDebug();
   }

   private void readLineNumberTable(@Nonnull Context context, @Nonnegative int u) {
      if (context.readDebugInfo()) {
         Label[] labels = context.labels;

         for (int j = readUnsignedShort(u + 8), v = u; j > 0; j--) {
            int label = readUnsignedShort(v + 10);

            if (labels[label] == null) {
               readDebugLabel(label, labels);
            }

            labels[label].line = readUnsignedShort(v + 12);
            v += 4;
         }
      }
   }

   private void readBytecodeInstructionsInCodeBlock(
      @Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int codeStart, @Nonnegative int codeEnd
   ) {
      Label[] labels = context.labels;
      boolean readDebugInfo = context.readDebugInfo();
      char[] c = buf;
      byte[] b = this.code;
      int u = codeStart;

      while (u < codeEnd) {
         int offset = u - codeStart;
         readLabelAndLineNumber(mv, labels[offset], readDebugInfo);

         // Visits the instruction at this offset.
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
               mv.visitInsn(opcode);
               u++;
               break;
            case IMPLVAR:
               readImplicitVarInstruction(mv, opcode);
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
               u = readWideInstruction(mv, u);
               break;
            case TABL:
               u = readTableSwitchInstruction(mv, labels, u, offset);
               break;
            case LOOK:
               u = readLookupSwitchInstruction(mv, labels, u, offset);
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
               readFieldOrInvokeInstruction(mv, c, u, opcode);
               u += opcode == INVOKEINTERFACE ? 5 : 3;
               break;
            case INDYMETH:
               u = readInvokeDynamicInstruction(mv, context, u);
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
   private void readLabelAndLineNumber(@Nonnull MethodVisitor mv, @Nullable Label label, boolean readDebugInfo) {
      if (label != null) {
         mv.visitLabel(label);

         if (readDebugInfo && label.line > 0) {
            mv.visitLineNumber(label.line, label);
         }
      }
   }

   private void readImplicitVarInstruction(@Nonnull MethodVisitor mv, int opcode) {
      int opcodeBase;

      if (opcode > ISTORE) {
         opcode -= ISTORE_0;
         opcodeBase = ISTORE;
      }
      else {
         opcode -= ILOAD_0;
         opcodeBase = ILOAD;
      }

      mv.visitVarInsn(opcodeBase + (opcode >> 2), opcode & 0x3);
   }

   @Nonnegative
   private int readWideInstruction(@Nonnull MethodVisitor mv, @Nonnegative int u) {
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
   private int readTableSwitchInstruction(
      @Nonnull MethodVisitor mv, @Nonnull Label[] labels, @Nonnegative int u, @Nonnegative int offset
   ) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      int dfltLabelIndex = offset + readInt(u);
      int min = readInt(u + 4);
      int max = readInt(u + 8);
      Label[] table = new Label[max - min + 1];
      u += 12;

      for (int i = 0; i < table.length; i++) {
         int handlerLabelIndex = offset + readInt(u);
         table[i] = labels[handlerLabelIndex];
         u += 4;
      }

      Label dfltLabel = labels[dfltLabelIndex];
      mv.visitTableSwitchInsn(min, max, dfltLabel, table);
      return u;
   }

   @Nonnegative
   private int readLookupSwitchInstruction(
      @Nonnull MethodVisitor mv, @Nonnull Label[] labels, @Nonnegative int u, @Nonnegative int offset
   ) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads the instruction.
      int dfltLabelIndex = offset + readInt(u);
      int len = readInt(u + 4);
      int[] keys = new int[len];
      Label[] values = new Label[len];
      u += 8;

      for (int i = 0; i < len; i++) {
         keys[i] = readInt(u);
         int handlerLabelIndex = offset + readInt(u + 4);
         values[i] = labels[handlerLabelIndex];
         u += 8;
      }

      Label dfltLabel = labels[dfltLabelIndex];
      mv.visitLookupSwitchInsn(dfltLabel, keys, values);
      return u;
   }

   private void readFieldOrInvokeInstruction(
      @Nonnull MethodVisitor mv, @Nonnull char[] c, @Nonnegative int u, int opcode
   ) {
      int cpIndex1 = items[readUnsignedShort(u + 1)];

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

   private void readLocalVariableTables(
      @Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int varTable, int varTypeTable
   ) {
      char[] c = buf;
      Label[] labels = context.labels;
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
         //noinspection ConstantConditions
         mv.visitLocalVariable(name, desc, signature, labels[start], labels[start + length], index);
         u += 10;
      }
   }

   @Nonnegative
   private int readInvokeDynamicInstruction(@Nonnull MethodVisitor mv, @Nonnull Context context, @Nonnegative int u) {
      int cpIndex = items[readUnsignedShort(u + 1)];
      int bsmIndex = context.bootstrapMethods[readUnsignedShort(cpIndex)];
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
   private int readTableSwitchInstruction(@Nonnull Label[] labels, @Nonnegative int u, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(u), labels);

      for (int i = readInt(u + 8) - readInt(u + 4) + 1; i > 0; i--) {
         readLabel(offset + readInt(u + 12), labels);
         u += 4;
      }

      return u + 12;
   }

   @Nonnegative
   private int readLookupSwitchInstruction(@Nonnull Label[] labels, @Nonnegative int u, @Nonnegative int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(u), labels);

      for (int i = readInt(u + 4); i > 0; i--) {
         readLabel(offset + readInt(u + 12), labels);
         u += 8;
      }

      return u + 8;
   }
}
