package mockit.asm.methods;

import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.annotations.*;
import mockit.asm.constantPool.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.types.*;
import mockit.asm.util.*;

/**
 * A visitor to visit a Java method, in the following order:<br>
 * ({@link #visitAnnotation})* ({@link #visitParameterAnnotation})*
 * [(<tt>visit<i>X</i>Insn</tt> | {@link #visitLabel} | {@link #visitTryCatchBlock} | {@link #visitLocalVariable} |
 * {@link #visitLineNumber})* {@link #visitMaxStack}] {@link #visitEnd}.
 * <p/>
 * In addition, the <tt>visit<i>X</i>Insn</tt> and <tt>visitLabel</tt> methods are called in the sequential order of the bytecode
 * instructions of the visited code, <tt>visitTryCatchBlock</tt> is called <i>before</i> the labels passed as arguments have been visited,
 * and the <tt>visitLocalVariable</tt> and <tt>visitLineNumber</tt> methods are called <i>after</i> the labels passed as arguments have been
 * visited.
 */
public class MethodVisitor extends BaseWriter
{
   protected MethodVisitor() {}
   protected MethodVisitor(@Nonnull ConstantPoolGeneration cp, int methodAccess) { super(cp, methodAccess); }

   /**
    * Visits an annotation on a parameter of the method being visited.
    *
    * @param parameter the parameter index
    * @param desc      the descriptor of the annotation type
    * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting this annotation
    */
   @Nullable
   public AnnotationVisitor visitParameterAnnotation(@Nonnegative int parameter, @Nonnull String desc) { return null; }

   /**
    * Visits a zero operand instruction.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the instruction to be visited:
    * NOP, ACONST_NULL, ICONST_M1, ICONST_0 ICONST_0 to ICONST_5, LCONST_0, LCONST_1, FCONST_0 to FCONST_2, DCONST_0, DCONST_1, IALOAD,
    * LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, POP,
    * POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV,
    * LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR,
    * LXOR, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN,
    * FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER, or MONITOREXIT
    */
   public void visitInsn(int opcode) {}

   /**
    * Visits an instruction with a single <tt>int</tt> operand.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the instruction to be visited: BIPUSH, SIPUSH, or NEWARRAY
    * @param operand the operand of the instruction to be visited:
    *                when opcode is BIPUSH, it's between {@link Byte#MIN_VALUE} and {@link Byte#MAX_VALUE};
    *                when opcode is SIPUSH, it's between {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE};
    *                when opcode is NEWARRAY, the operand value is one of the {@link ArrayElementType} values
    */
   public void visitIntInsn(int opcode, int operand) {}

   /**
    * Visits a local variable instruction, which loads or stores the value of a local variable.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the local variable instruction to be visited:
    * ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET
    * @param varIndex the operand of the instruction to be visited, which is the index of a local variable
    */
   public void visitVarInsn(int opcode, @Nonnegative int varIndex) {}

   /**
    * Visits a type instruction, which takes the internal name of a class as parameter.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the instruction to be visited: NEW, ANEWARRAY, CHECKCAST, or INSTANCEOF
    * @param typeDesc the operand of the instruction, which is the internal name of an object or array class
    */
   public void visitTypeInsn(int opcode, @Nonnull String typeDesc) {}

   /**
    * Visits a field access instruction, which loads or stores the value of a field of an object or a class.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the instruction to be visited: GETSTATIC, PUTSTATIC, GETFIELD, or PUTFIELD
    * @param owner the internal name of the field's owner class
    * @param name the field's name
    * @param desc the field's descriptor (see {@link JavaType})
    */
   public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {}

   /**
    * Visits a method invocation instruction, which invokes a method or constructor.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the instruction: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, or INVOKEINTERFACE
    * @param owner the internal name of the method's owner class
    * @param name the method's name
    * @param desc the method's descriptor (see {@link JavaType})
    * @param itf whether the method's owner class is an interface or not
    */
   public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {}

   /**
    * Visits an {@link Opcodes#INVOKEDYNAMIC INVOKEDYNAMIC} instruction.
    *
    * @param name    the method's name
    * @param desc    the method's descriptor (see {@link JavaType})
    * @param bsm     the bootstrap method
    * @param bsmArgs the bootstrap method constant arguments, where each argument must be an {@link Integer}, {@link Float}, {@link Long},
    * {@link Double}, {@link String}, {@link JavaType}, or {@link MethodHandle} value
    */
   public void visitInvokeDynamicInsn(@Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs) {}

   /**
    * Visits a jump instruction.
    *
    * @param opcode the {@linkplain Opcodes opcode} of the jump instruction to be visited:
    * IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR,
    * IFNULL, or IFNONNULL
    * @param label the operand of the instruction to be visited, which is a label that designates the instruction to which the jump
    *              instruction may jump
    */
   public void visitJumpInsn(int opcode, @Nonnull Label label) {}

   /**
    * Visits a label, which designates the instruction that will be visited just after it.
    */
   public void visitLabel(@Nonnull Label label) {}

   /**
    * Visits a {@link Opcodes#LDC LDC} instruction.
    *
    * @param cst the constant to be loaded on the stack, which must be a non null
    * {@link Integer}/{@link Float}/{@link Long}/{@link Double}/{@link String}, an {@link ObjectType} or {@link ArrayType} for
    * <tt>.class</tt> constants, a {@link MethodType}, or a {@link MethodHandle}
    */
   public void visitLdcInsn(@Nonnull Object cst) {}

   /**
    * Visits an {@link Opcodes#IINC IINC} instruction.
    *
    * @param varIndex  index of the local variable to be incremented
    * @param increment amount to increment the local variable by
    */
   public void visitIincInsn(@Nonnegative int varIndex, int increment) {}

   /**
    * Visits a {@link Opcodes#TABLESWITCH TABLESWITCH} instruction.
    *
    * @param min    the minimum key value
    * @param max    the maximum key value
    * @param dflt   beginning of the default handler block
    * @param labels beginnings of the handler blocks; <tt>labels[i]</tt> is the beginning of the handler block for the <tt>min + i</tt> key
    */
   public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {}

   /**
    * Visits a {@link Opcodes#LOOKUPSWITCH LOOKUPSWITCH} instruction.
    *
    * @param dflt   beginning of the default handler block
    * @param keys   the values of the keys
    * @param labels beginnings of the handler blocks; <tt>labels[i]</tt> is the beginning of the handler block for the <tt>keys[i]</tt>
    */
   public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {}

   /**
    * Visits a {@link Opcodes#MULTIANEWARRAY MULTIANEWARRAY} instruction.
    *
    * @param desc an array type descriptor (see {@link ArrayType})
    * @param dims number of dimensions of the array to allocate
    */
   public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {}

   /**
    * Visits a <tt>try..catch</tt> block.
    *
    * @param start   beginning of the exception handler's scope (inclusive)
    * @param end     end of the exception handler's scope (exclusive)
    * @param handler beginning of the exception handler's code
    * @param type    internal name of the type of exceptions handled by the handler, or <tt>null</tt> to catch any exceptions (for "finally"
    *                blocks)
    */
   public void visitTryCatchBlock(
      @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {}

   /**
    * Visits a local variable declaration.
    *
    * @param name      the name of the local variable
    * @param desc      the type descriptor of the local variable
    * @param signature the type signature of the local variable; <tt>null</tt> when the local variable type does not use generic types
    * @param start     the first instruction corresponding to the scope of this local variable (inclusive)
    * @param end       the last instruction corresponding to the scope of this local variable (exclusive)
    * @param index     the local variable's index
    */
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index) {}

   /**
    * Visits a line number within the body of the method.
    *
    * @param line a line number, which refers to the source file from which the class was compiled
    * @param start the first instruction corresponding to this line number
    */
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {}

   /**
    * Visits the maximum stack size of the method.
    */
   public void visitMaxStack(@Nonnegative int maxStack) {}
}
