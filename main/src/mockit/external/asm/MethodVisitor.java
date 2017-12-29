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

/**
 * A visitor to visit a Java method. The methods of this class are called by {@link ClassReader} in the following order:
 * <p/>
 * ({@link #visitAnnotation})* ({@link #visitParameterAnnotation})*
 * [(<tt>visit<i>X</i>Insn</tt> | {@link #visitLabel} | {@link #visitTryCatchBlock} | {@link #visitLocalVariable} |
 * {@link #visitLineNumber})* {@link #visitMaxStack}] {@link #visitEnd}.
 * <p/>
 * In addition, the <tt>visit<i>X</i>Insn</tt> and <tt>visitLabel</tt> methods are called in the sequential order of the
 * bytecode instructions of the visited code, <tt>visitTryCatchBlock</tt> is called <i>before</i> the labels passed as
 * arguments have been visited, and the <tt>visitLocalVariable</tt> and <tt>visitLineNumber</tt> methods are called
 * <i>after</i> the labels passed as arguments have been visited.
 */
public class MethodVisitor extends BaseWriter
{
   protected MethodVisitor() {}

   /**
    * Visits an annotation on a parameter of the method being visited.
    *
    * @param parameter the parameter index.
    * @param desc      the descriptor of the annotation type.
    * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting
    * this annotation.
    */
   @Nullable
   public AnnotationVisitor visitParameterAnnotation(@Nonnegative int parameter, @Nonnull String desc) { return null; }

   /**
    * Visits a zero operand instruction.
    *
    * @param opcode the opcode of the instruction to be visited: {@link Opcodes#NOP NOP},
    * {@link Opcodes#ACONST_NULL ACONST_NULL}, {@link Opcodes#ICONST_M1 ICONST_M1}, {@link Opcodes#ICONST_0 ICONST_0} to
    * {@link Opcodes#ICONST_5 ICONST_5}, {@link Opcodes#LCONST_0 LCONST_0}, {@link Opcodes#LCONST_1},
    * {@link Opcodes#FCONST_0 FCONST_0} to {@link Opcodes#FCONST_2 FCONST_2}, {@link Opcodes#DCONST_0 DCONST_0},
    * {@link Opcodes#DCONST_1 DCONST_1}, {@link Opcodes#IALOAD IALOAD}, {@link Opcodes#LALOAD LALOAD},
    * {@link Opcodes#FALOAD FALOAD}, {@link Opcodes#DALOAD DALOAD}, {@link Opcodes#AALOAD AALOAD},
    * {@link Opcodes#BALOAD BALOAD}, {@link Opcodes#CALOAD CALOAD}, {@link Opcodes#SALOAD SALOAD},
    * {@link Opcodes#IASTORE IASTORE}, LASTORE, FASTORE, DASTORE, {@link Opcodes#AASTORE AASTORE},
    * {@link Opcodes#BASTORE BASTORE}, {@link Opcodes#CASTORE CASTORE}, {@link Opcodes#SASTORE SASTORE},
    * {@link Opcodes#POP POP}, POP2, {@link Opcodes#DUP DUP}, {@link Opcodes#DUP_X1 DUP_X1},
    * {@link Opcodes#DUP_X2 DUP_X2}, {@link Opcodes#DUP2 DUP2}, DUP2_X1, DUP2_X2, {@link Opcodes#SWAP SWAP},
    * {@link Opcodes#IADD IADD}, LADD, FADD, DADD, {@link Opcodes#ISUB ISUB}, LSUB, FSUB, {@link Opcodes#DSUB DSUB},
    * {@link Opcodes#IMUL IMUL}, LMUL, {@link Opcodes#FMUL FMUL}, DMUL, {@link Opcodes#IDIV IDIV}, LDIV, FDIV, DDIV,
    * {@link Opcodes#IREM IREM}, LREM, FREM, DREM, {@link Opcodes#INEG INEG}, LNEG, FNEG, {@link Opcodes#DNEG DNEG},
    * {@link Opcodes#ISHL ISHL}, LSHL, {@link Opcodes#ISHR ISHR}, LSHR, IUSHR, LUSHR, {@link Opcodes#IAND IAND}, LAND,
    * {@link Opcodes#IOR IOR}, LOR, {@link Opcodes#IXOR IXOR}, LXOR, {@link Opcodes#I2L I2L}, I2F, I2D, L2I, L2F, L2D,
    * {@link Opcodes#F2I F2I}, F2L, {@link Opcodes#F2D F2D}, {@link Opcodes#D2I D2I}, D2L, D2F, I2B, I2C, I2S,
    * {@link Opcodes#LCMP LCMP}, FCMPL, FCMPG, DCMPL, DCMPG, {@link Opcodes#IRETURN IRETURN}, LRETURN, FRETURN,
    * {@link Opcodes#DRETURN DRETURN}, {@link Opcodes#ARETURN ARETURN}, {@link Opcodes#RETURN RETURN},
    * {@link Opcodes#ARRAYLENGTH ARRAYLENGTH}, {@link Opcodes#ATHROW ATHROW}, {@link Opcodes#MONITORENTER MONITORENTER},
    * or {@link Opcodes#MONITOREXIT MONITOREXIT}.
    */
   public void visitInsn(int opcode) {}

   /**
    * Visits an instruction with a single <tt>int</tt> operand.
    *
    * @param opcode  the opcode of the instruction to be visited. This opcode is either {@link Opcodes#BIPUSH BIPUSH},
    *                {@link Opcodes#SIPUSH SIPUSH} or {@link Opcodes#NEWARRAY NEWARRAY}.
    * @param operand the operand of the instruction to be visited.<br>
    *                When opcode is BIPUSH, it's between {@link Byte#MIN_VALUE} and {@link Byte#MAX_VALUE}.<br>
    *                When opcode is SIPUSH, it's between {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE}.<br>
    *                When opcode is NEWARRAY, the operand value is one of the {@link ArrayElementType} values.
    */
   public void visitIntInsn(int opcode, int operand) {}

   /**
    * Visits a local variable instruction, which loads or stores the value of a local variable.
    *
    * @param opcode the opcode of the local variable instruction to be visited. This opcode is either
    * {@link Opcodes#ILOAD ILOAD}, {@link Opcodes#LLOAD LLOAD}, {@link Opcodes#FLOAD FLOAD},
    * {@link Opcodes#DLOAD DLOAD}, {@link Opcodes#ALOAD ALOAD}, {@link Opcodes#ISTORE ISTORE},
    * {@link Opcodes#LSTORE LSTORE}, {@link Opcodes#FSTORE FSTORE}, {@link Opcodes#DSTORE DSTORE},
    * {@link Opcodes#ASTORE ASTORE} or {@link Opcodes#RET RET}.
    * @param var the operand of the instruction to be visited, which is the index of a local variable.
    */
   public void visitVarInsn(int opcode, @Nonnegative int var) {}

   /**
    * Visits a type instruction, which takes the internal name of a class as parameter.
    *
    * @param opcode the opcode of the instruction to be visited: {@link Opcodes#NEW NEW},
    * {@link Opcodes#ANEWARRAY ANEWARRAY}, {@link Opcodes#CHECKCAST CHECKCAST}, or
    * {@link Opcodes#INSTANCEOF INSTANCEOF}.
    * @param type the operand of the instruction, which is the internal name of an object or array class.
    */
   public void visitTypeInsn(int opcode, @Nonnull String type) {}

   /**
    * Visits a field access instruction, which loads or stores the value of a field of an object or a class.
    *
    * @param opcode the opcode of the instruction to be visited: GETSTATIC, PUTSTATIC, GETFIELD, or PUTFIELD.
    * @param owner  the internal name of the field's owner class.
    * @param name   the field's name.
    * @param desc   the field's descriptor (see {@link JavaType}).
    */
   public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {}

   /**
    * Visits a method invocation instruction, which invokes a method or constructor.
    *
    * @param opcode the opcode of the instruction: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, or INVOKEINTERFACE.
    * @param owner  the internal name of the method's owner class.
    * @param name   the method's name.
    * @param desc   the method's descriptor (see {@link JavaType}).
    * @param itf    whether the method's owner class is an interface or not.
    */
   public void visitMethodInsn(
      int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {}

   /**
    * Visits an {@link Opcodes#INVOKEDYNAMIC INVOKEDYNAMIC} instruction.
    *
    * @param name    the method's name.
    * @param desc    the method's descriptor (see {@link JavaType}).
    * @param bsm     the bootstrap method.
    * @param bsmArgs the bootstrap method constant arguments. Each argument must be an {@link Integer}, {@link Float},
    *                {@link Long}, {@link Double}, {@link String}, {@link JavaType}, or {@link MethodHandle} value.
    */
   public void visitInvokeDynamicInsn(
      @Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs) {}

   /**
    * Visits a jump instruction. A jump instruction is an instruction that may jump to another instruction.
    *
    * @param opcode the opcode of the type instruction to be visited. This opcode is either IFEQ, IFNE, IFLT, IFGE,
    *               IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
    *               GOTO, JSR, IFNULL or IFNONNULL.
    * @param label  the operand of the instruction to be visited. This operand is a label that designates the
    *               instruction to which the jump instruction may jump.
    */
   public void visitJumpInsn(int opcode, @Nonnull Label label) {}

   /**
    * Visits a label. A label designates the instruction that will be visited just after it.
    */
   public void visitLabel(@Nonnull Label label) {}

   /**
    * Visits a {@link Opcodes#LDC LDC} instruction.
    *
    * @param cst the constant to be loaded on the stack, which must be a non null
    * {@link Integer}/{@link Float}/{@link Long}/{@link Double}/{@link String}, an {@link ObjectType} or
    * {@link ArrayType} for <tt>.class</tt> constants, a {@link MethodType}, or a {@link MethodHandle}.
    */
   public void visitLdcInsn(@Nonnull Object cst) {}

   /**
    * Visits an {@link Opcodes#IINC IINC} instruction.
    *
    * @param var       index of the local variable to be incremented.
    * @param increment amount to increment the local variable by.
    */
   public void visitIincInsn(@Nonnegative int var, @Nonnegative int increment) {}

   /**
    * Visits a {@link Opcodes#TABLESWITCH TABLESWITCH} instruction.
    *
    * @param min    the minimum key value.
    * @param max    the maximum key value.
    * @param dflt   beginning of the default handler block.
    * @param labels beginnings of the handler blocks. <tt>labels[i]</tt> is the beginning of the handler block for the
    *               <tt>min + i</tt> key.
    */
   public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {}

   /**
    * Visits a {@link Opcodes#LOOKUPSWITCH LOOKUPSWITCH} instruction.
    *
    * @param dflt   beginning of the default handler block.
    * @param keys   the values of the keys.
    * @param labels beginnings of the handler blocks. <tt>labels[i]</tt> is the beginning of the handler block for the
    *               <tt>keys[i]</tt>.
    */
   public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {}

   /**
    * Visits a {@link Opcodes#MULTIANEWARRAY MULTIANEWARRAY} instruction.
    *
    * @param desc an array type descriptor (see {@link ArrayType}).
    * @param dims number of dimensions of the array to allocate.
    */
   public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {}

   /**
    * Visits a <tt>try..catch</tt> block.
    *
    * @param start   beginning of the exception handler's scope (inclusive).
    * @param end     end of the exception handler's scope (exclusive).
    * @param handler beginning of the exception handler's code.
    * @param type    internal name of the type of exceptions handled by the handler, or <tt>null</tt> to catch any
    *                exceptions (for "finally" blocks).
    */
   public void visitTryCatchBlock(
      @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {}

   /**
    * Visits a local variable declaration.
    *
    * @param name      the name of a local variable.
    * @param desc      the type descriptor of the local variable.
    * @param signature the type signature of the local variable.
    *                  May be <tt>null</tt> if the local variable type does not use generic types.
    * @param start     the first instruction corresponding to the scope of this local variable (inclusive).
    * @param end       the last instruction corresponding to the scope of this local variable (exclusive).
    * @param index     the local variable's index.
    */
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index) {}

   /**
    * Visits a line number within the body of the method.
    *
    * @param line  a line number. This number refers to the source file from which the class was compiled.
    * @param start the first instruction corresponding to this line number.
    */
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {}

   /**
    * Visits the maximum stack size of the method.
    */
   public void visitMaxStack(@Nonnegative int maxStack) {}
}
