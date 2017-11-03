package mockit.external.asm;

import javax.annotation.*;

/**
 * Same as {@link ClassVisitor}, except it always wraps a {@link ClassWriter}.
 */
public class WrappingClassVisitor extends ClassVisitor
{
   /**
    * The class visitor to which this visitor must delegate method calls.
    */
   @Nonnull protected final ClassWriter cw;

   /**
    * Constructs a new {@link WrappingClassVisitor}.
    *
    * @param cw the class writer to which this visitor must delegate method calls.
    */
   protected WrappingClassVisitor(@Nonnull ClassWriter cw) { this.cw = cw; }

   @Override
   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      cw.visit(version, access, name, signature, superName, interfaces);
   }

   @Override
   public void visitSource(String source, String debug) {
      cw.visitSource(source, debug);
   }

   @Override
   public void visitOuterClass(String owner, String name, String desc) {
      cw.visitOuterClass(owner, name, desc);
   }

   @Override
   public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return cw.visitAnnotation(desc, visible);
   }

   @Override
   public void visitAttribute(Attribute attr) {
      cw.visitAttribute(attr);
   }

   @Override
   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      cw.visitInnerClass(name, outerName, innerName, access);
   }

   @Override
   public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
      return cw.visitField(access, name, desc, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      return cw.visitMethod(access, name, desc, signature, exceptions);
   }

   @Override
   public void visitEnd() {
      cw.visitEnd();
   }

   @Override
   public final byte[] toByteArray() { return cw.toByteArray(); }
}
