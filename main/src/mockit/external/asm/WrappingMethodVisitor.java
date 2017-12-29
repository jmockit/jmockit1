package mockit.external.asm;

import javax.annotation.*;

/**
 * Same as {@link MethodVisitor}, except it always wraps a {@link MethodWriter}.
 */
public class WrappingMethodVisitor extends MethodVisitor
{
   /**
    * The method writer to which this visitor must delegate method calls.
    */
   @Nonnull protected final MethodWriter mw;

   /**
    * Constructs a new wrapping Method Visitor.
    *
    * @param mw the method visitor to which this visitor must delegate method calls.
    */
   protected WrappingMethodVisitor(@Nonnull MethodWriter mw) { this.mw = mw; }

   @Nullable @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return mw.visitAnnotation(desc);
   }

   @Override
   public final AnnotationVisitor visitParameterAnnotation(@Nonnegative int parameter, @Nonnull String desc) {
      return mw.visitParameterAnnotation(parameter, desc);
   }

   @Override
   public void visitInsn(int opcode) {
      mw.visitInsn(opcode);
   }

   @Override
   public void visitIntInsn(int opcode, int operand) {
      mw.visitIntInsn(opcode, operand);
   }

   @Override
   public void visitVarInsn(int opcode, int var) {
      mw.visitVarInsn(opcode, var);
   }

   @Override
   public void visitTypeInsn(int opcode, @Nonnull String type) {
      mw.visitTypeInsn(opcode, type);
   }

   @Override
   public void visitFieldInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      mw.visitFieldInsn(opcode, owner, name, desc);
   }

   @Override
   public void visitMethodInsn(
      int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf
   ) {
      mw.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   @Override
   public final void visitInvokeDynamicInsn(
      @Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs
   ) {
      mw.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
   }

   @Override
   public void visitJumpInsn(int opcode, @Nonnull Label label) {
      mw.visitJumpInsn(opcode, label);
   }

   @Override
   public void visitLabel(@Nonnull Label label) {
      mw.visitLabel(label);
   }

   @Override
   public void visitLdcInsn(@Nonnull Object cst) {
      mw.visitLdcInsn(cst);
   }

   @Override
   public void visitIincInsn(int var, int increment) {
      mw.visitIincInsn(var, increment);
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, @Nonnull Label dflt, @Nonnull Label... labels) {
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(@Nonnull Label dflt, @Nonnull int[] keys, @Nonnull Label[] labels) {
      mw.visitLookupSwitchInsn(dflt, keys, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(@Nonnull String desc, @Nonnegative int dims) {
      mw.visitMultiANewArrayInsn(desc, dims);
   }

   @Override
   public void visitTryCatchBlock(
      @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type
   ) {
      mw.visitTryCatchBlock(start, end, handler, type);
   }

   @Override
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      mw.visitLocalVariable(name, desc, signature, start, end, index);
   }

   @Override
   public void visitLineNumber(@Nonnegative int line, @Nonnull Label start) {
      mw.visitLineNumber(line, start);
   }

   @Override
   public final void visitMaxStack(@Nonnegative int maxStack) {
      mw.visitMaxStack(maxStack);
   }
}
