package mockit.external.asm;

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

      for (int i = 0; i < b.length; ++i) {
         b[i] = (byte) (s.charAt(i) - 'A');
      }
   }

   private final ClassReader cr;
   private String[] exceptions;

   MethodReader(ClassReader cr) { super(cr); this.cr = cr; }

   /**
    * Reads a method and makes the given visitor visit it.
    *
    * @param context information about the class being parsed.
    * @param u       the start offset of the method in the class file.
    * @return the offset of the first byte following the method in the class.
    */
   int readMethod(Context context, int u) {
      u = readMethodDeclaration(context, u);

      int u0 = u;
      int code = 0;
      int exception = 0;
      exceptions = null;
      String signature = null;
      int anns = 0;
      int annDefault = 0;
      int paramAnns = 0;
      char[] c = context.buffer;

      for (int i = readUnsignedShort(u); i > 0; --i) {
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

   private int readMethodDeclaration(Context context, int u) {
      char[] c = context.buffer;
      context.access = readUnsignedShort(u);
      context.name = readUTF8(u + 2, c);
      context.desc = readUTF8(u + 4, c);
      return u + 6;
   }

   private int readExceptionsInThrowsClause(int u, char[] c) {
      int n = readUnsignedShort(u + 8);
      exceptions = new String[n];
      int exception = u + 10;

      for (int j = 0; j < n; ++j) {
         exceptions[j] = readClass(exception, c);
         exception += 2;
      }

      return exception;
   }

   private void readMethodBody(
      Context context, int u0, int u, int code, int exception, String signature, int anns, int annDefault, int paramAnns
   ) {
      MethodVisitor mv = cr.cv.visitMethod(context.access, context.name, context.desc, signature, exceptions);

      if (mv == null || copyMethodBodyIfApplicable(mv, u, exception, signature, u0)) {
         return;
      }

      char[] c = context.buffer;
      readAnnotationDefaultValue(mv, c, annDefault);
      readAnnotationValues(mv, c, anns);
      readParameterAnnotations(mv, context, paramAnns);
      readMethodCode(mv, context, code);
      mv.visitEnd();
   }

   /**
    * If the returned MethodVisitor is in fact a MethodWriter, it means there is no method adapter between the reader
    * and the writer.
    * If, in addition, the writer's constant pool was copied from this reader (mw.cw.cr == this), and the signature
    * and exceptions of the method have not been changed, then it is possible to skip all visit events and just copy
    * the original code of the method to the writer (the access, name and descriptor can have been changed, this is not
    * important since they are not copied as is from the reader).
    */
   private boolean copyMethodBodyIfApplicable(
      MethodVisitor mv, int u, int exception, String signature, int firstAttribute
   ) {
      if (mv instanceof MethodWriter) {
         MethodWriter mw = (MethodWriter) mv;

         //noinspection StringEquality
         if (mw.cw.cr == cr && signature == mw.signature) {
            boolean sameExceptions = false;
            ThrowsClause throwsClause = mw.throwsClause;
            int exceptionCount = throwsClause.getExceptionCount();

            if (exceptions == null) {
               sameExceptions = exceptionCount == 0;
            }
            else if (exceptions.length == exceptionCount) {
               sameExceptions = true;

               for (int j = exceptions.length - 1; j >= 0; --j) {
                  exception -= 2;
                  int exceptionIndex = readUnsignedShort(exception);

                  if (throwsClause.getExceptionIndex(j) != exceptionIndex) {
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
         }
      }

      return false;
   }

   private void readAnnotationDefaultValue(MethodVisitor mv, char[] c, int annotationDefault) {
      if (annotationDefault != 0) {
         AnnotationVisitor dv = mv.visitAnnotationDefault();
         annotationReader.readAnnotationValue(annotationDefault, c, null, dv);

         if (dv != null) {
            dv.visitEnd();
         }
      }
   }

   private void readAnnotationValues(MethodVisitor mv, char[] c, int anns) {
      if (anns != 0) {
         for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; i--) {
            String desc = readUTF8(v, c);
            AnnotationVisitor av = mv.visitAnnotation(desc);
            v = annotationReader.readAnnotationValues(v + 2, c, true, av);
         }
      }
   }

   /**
    * Reads parameter annotations and makes the given visitor visit them.
    *
    * @param mv      the visitor that must visit the annotations.
    * @param context information about the class being parsed.
    * @param v       start offset in {@link #b} of the annotations to be read.
    */
   private void readParameterAnnotations(MethodVisitor mv, Context context, int v) {
      if (v != 0) {
         char[] c = context.buffer;
         int parameters = b[v++] & 0xFF;
         AnnotationVisitor av;

         for (int i = 0; i < parameters; i++) {
            int j = readUnsignedShort(v);
            v += 2;

            for (; j > 0; j--) {
               String desc = readUTF8(v, c);
               av = mv.visitParameterAnnotation(i, desc);
               v = annotationReader.readAnnotationValues(v + 2, c, true, av);
            }
         }
      }
   }

   private void readMethodCode(MethodVisitor mv, Context context, int code) {
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
   private void readCode(MethodVisitor mv, Context context, int u) {
      // Reads the header.
      char[] c = context.buffer;
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

      for (int i = readUnsignedShort(u); i > 0; i--) {
         String attrName = readUTF8(u + 2, c);

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

   private int readAllLabelsInCodeBlock(Context context, int u, int codeLength, int codeStart, int codeEnd) {
      byte[] b = this.b;
      Label[] labels = context.labels;

      readLabel(codeLength + 1, labels);

      while (u < codeEnd) {
         int offset = u - codeStart;
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
            case IMPLVAR:
               u += 1;
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

               if (opcode == IINC) {
                  u += 6;
               }
               else {
                  u += 4;
               }

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

   // Reads the try catch entries to find the labels, and also visits them.
   private int readTryCatchBlocks(MethodVisitor mv, Context context, int u) {
      char[] c = context.buffer;
      Label[] labels = context.labels;

      for (int i = readUnsignedShort(u); i > 0; --i) {
         Label start = readLabel(readUnsignedShort(u + 2), labels);
         Label end = readLabel(readUnsignedShort(u + 4), labels);
         Label handler = readLabel(readUnsignedShort(u + 6), labels);
         String type = readUTF8(items[readUnsignedShort(u + 8)], c);

         mv.visitTryCatchBlock(start, end, handler, type);
         u += 8;
      }

      return u;
   }

   private int readLocalVariableTable(Context context, int u, int varTable) {
      if (context.readDebugInfo()) {
         Label[] labels = context.labels;
         varTable = u + 8;

         for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
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

   private void readLineNumberTable(Context context, int u) {
      if (context.readDebugInfo()) {
         Label[] labels = context.labels;

         for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
            int label = readUnsignedShort(v + 10);

            if (labels[label] == null) {
               readDebugLabel(label, labels);
            }

            labels[label].line = readUnsignedShort(v + 12);
            v += 4;
         }
      }
   }

   private void readBytecodeInstructionsInCodeBlock(MethodVisitor mv, Context context, int codeStart, int codeEnd) {
      boolean readDebugInfo = context.readDebugInfo();
      byte[] b = this.b;
      char[] c = context.buffer;
      Label[] labels = context.labels;
      int u = codeStart;

      while (u < codeEnd) {
         int offset = u - codeStart;

         // Visits the label and line number for this offset, if any.
         Label l = labels[offset];

         if (l != null) {
            mv.visitLabel(l);

            if (readDebugInfo && l.line > 0) {
               mv.visitLineNumber(l.line, l);
            }
         }

         // Visits the instruction at this offset.
         int opcode = b[u] & 0xFF;

         switch (TYPE[opcode]) {
            case NOARG:
               mv.visitInsn(opcode);
               u += 1;
               break;
            case IMPLVAR:
               if (opcode > ISTORE) {
                  opcode -= 59; // ISTORE_0
                  mv.visitVarInsn(ISTORE + (opcode >> 2), opcode & 0x3);
               }
               else {
                  opcode -= 26; // ILOAD_0
                  mv.visitVarInsn(ILOAD + (opcode >> 2), opcode & 0x3);
               }

               u += 1;
               break;
            case LABEL:
               mv.visitJumpInsn(opcode, labels[offset + readShort(u + 1)]);
               u += 3;
               break;
            case LABELW:
               mv.visitJumpInsn(opcode - 33, labels[offset + readInt(u + 1)]);
               u += 5;
               break;
            case InstructionType.WIDE:
               opcode = b[u + 1] & 0xFF;

               if (opcode == IINC) {
                  mv.visitIincInsn(readUnsignedShort(u + 2), readShort(u + 4));
                  u += 6;
               }
               else {
                  mv.visitVarInsn(opcode, readUnsignedShort(u + 2));
                  u += 4;
               }

               break;
            case TABL: {
               // Skips 0 to 3 padding bytes.
               u = u + 4 - (offset & 3);

               // Reads instruction.
               int label = offset + readInt(u);
               int min = readInt(u + 4);
               int max = readInt(u + 8);
               Label[] table = new Label[max - min + 1];
               u += 12;

               for (int i = 0; i < table.length; ++i) {
                  table[i] = labels[offset + readInt(u)];
                  u += 4;
               }

               mv.visitTableSwitchInsn(min, max, labels[label], table);
               break;
            }
            case LOOK: {
               // Skips 0 to 3 padding bytes.
               u = u + 4 - (offset & 3);

               // Reads instruction.
               int label = offset + readInt(u);
               int len = readInt(u + 4);
               int[] keys = new int[len];
               Label[] values = new Label[len];
               u += 8;

               for (int i = 0; i < len; ++i) {
                  keys[i] = readInt(u);
                  values[i] = labels[offset + readInt(u + 4)];
                  u += 8;
               }

               mv.visitLookupSwitchInsn(labels[label], keys, values);
               break;
            }
            case VAR:
               mv.visitVarInsn(opcode, b[u + 1] & 0xFF);
               u += 2;
               break;
            case SBYTE:
               mv.visitIntInsn(opcode, b[u + 1]);
               u += 2;
               break;
            case SHORT:
               mv.visitIntInsn(opcode, readShort(u + 1));
               u += 3;
               break;
            case InstructionType.LDC:
               mv.visitLdcInsn(readConst(b[u + 1] & 0xFF, c));
               u += 2;
               break;
            case LDCW:
               mv.visitLdcInsn(readConst(readUnsignedShort(u + 1), c));
               u += 3;
               break;
            case FIELDORMETH:
            case ITFMETH: {
               int cpIndex = items[readUnsignedShort(u + 1)];
               boolean itf = b[cpIndex - 1] == ConstantPoolItemType.IMETH;
               String owner = readClass(cpIndex, c);
               cpIndex = items[readUnsignedShort(cpIndex + 2)];
               String name = readUTF8(cpIndex, c);
               String desc = readUTF8(cpIndex + 2, c);

               if (opcode < INVOKEVIRTUAL) {
                  mv.visitFieldInsn(opcode, owner, name, desc);
               }
               else {
                  mv.visitMethodInsn(opcode, owner, name, desc, itf);
               }

               if (opcode == INVOKEINTERFACE) {
                  u += 5;
               }
               else {
                  u += 3;
               }

               break;
            }
            case INDYMETH: {
               u = readInvokeDynamicInstruction(mv, context, u);
               break;
            }
            case InstructionType.TYPE:
               mv.visitTypeInsn(opcode, readClass(u + 1, c));
               u += 3;
               break;
            case IINC:
               mv.visitIincInsn(b[u + 1] & 0xFF, b[u + 2]);
               u += 3;
               break;
            // case MANA_INSN:
            default:
               mv.visitMultiANewArrayInsn(readClass(u + 1, c), b[u + 3] & 0xFF);
               u += 4;
               break;
         }
      }
   }

   private void readLocalVariableTables(MethodVisitor mv, Context context, int varTable, int varTypeTable) {
      char[] c = context.buffer;
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

      for (int i = readUnsignedShort(varTable); i > 0; --i) {
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
         mv.visitLocalVariable(name, desc, signature, labels[start], labels[start + length], index);
         u += 10;
      }
   }

   private int readInvokeDynamicInstruction(MethodVisitor mv, Context context, int u) {
      int cpIndex = items[readUnsignedShort(u + 1)];
      int bsmIndex = context.bootstrapMethods[readUnsignedShort(cpIndex)];
      char[] c = context.buffer;
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
      mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      u += 5;

      return u;
   }

   private int readTableSwitchInstruction(Label[] labels, int u, int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(u), labels);

      for (int i = readInt(u + 8) - readInt(u + 4) + 1; i > 0; --i) {
         readLabel(offset + readInt(u + 12), labels);
         u += 4;
      }

      return u + 12;
   }

   private int readLookupSwitchInstruction(Label[] labels, int u, int offset) {
      // Skips 0 to 3 padding bytes.
      u = u + 4 - (offset & 3);

      // Reads instruction.
      readLabel(offset + readInt(u), labels);

      for (int i = readInt(u + 4); i > 0; --i) {
         readLabel(offset + readInt(u + 12), labels);
         u += 8;
      }

      return u + 8;
   }
}
