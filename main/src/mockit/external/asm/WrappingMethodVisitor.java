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
   @Nonnull private final MethodVisitor mw;

   /**
    * Constructs a new {@link WrappingMethodVisitor}.
    *
    * @param mw the method visitor to which this visitor must delegate method calls.
    */
   protected WrappingMethodVisitor(@Nonnull MethodWriter mw) { this.mw = mw; }

   @Override
   public final AnnotationVisitor visitAnnotationDefault() {
      return mw.visitAnnotationDefault();
   }

   @Override
   public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return mw.visitAnnotation(desc, visible);
   }

   @Override
   public final AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      return mw.visitParameterAnnotation(parameter, desc, visible);
   }

   @Override
   public final void visitAttribute(Attribute attr) {
      mw.visitAttribute(attr);
   }

   @Override
   public final void visitCode() {
      mw.visitCode();
   }

   @Override
   public final void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
      mw.visitFrame(type, nLocal, local, nStack, stack);
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
   public void visitTypeInsn(int opcode, String type) {
      mw.visitTypeInsn(opcode, type);
   }

   @Override
   public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      mw.visitFieldInsn(opcode, owner, name, desc);
   }

   @Override
   public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      mw.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   @Override
   public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      mw.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
   }

   @Override
   public void visitJumpInsn(int opcode, Label label) {
      mw.visitJumpInsn(opcode, label);
   }

   @Override
   public void visitLabel(Label label) {
      mw.visitLabel(label);
   }

   @Override
   public void visitLdcInsn(Object cst) {
      mw.visitLdcInsn(cst);
   }

   @Override
   public void visitIincInsn(int var, int increment) {
      mw.visitIincInsn(var, increment);
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      mw.visitLookupSwitchInsn(dflt, keys, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(String desc, int dims) {
      mw.visitMultiANewArrayInsn(desc, dims);
   }

   @Override
   public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
      mw.visitTryCatchBlock(start, end, handler, type);
   }

   @Override
   public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      mw.visitLocalVariable(name, desc, signature, start, end, index);
   }

   @Override
   public void visitLineNumber(int line, Label start) {
      mw.visitLineNumber(line, start);
   }

   @Override
   public final void visitMaxs(int maxStack, int maxLocals) {
      mw.visitMaxs(maxStack, maxLocals);
   }

   @Override
   public void visitEnd() {
      mw.visitEnd();
   }
}
