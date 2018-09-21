package mockit.asm;

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
    * Constructs a new WrappingClassVisitor.
    *
    * @param cw the class writer to which this visitor must delegate method calls.
    */
   protected WrappingClassVisitor(@Nonnull ClassWriter cw) { this.cw = cw; }

   @Override
   public void visit(int version, int access, @Nonnull String name, @Nonnull ClassInfo additionalInfo) {
      cw.visit(version, access, name, additionalInfo);
   }

   @Override
   public void visitSource(@Nullable String fileName) {
      cw.visitSource(fileName);
   }

   @Nullable @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return cw.visitAnnotation(desc);
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      cw.visitInnerClass(name, outerName, innerName, access);
   }

   @Nullable @Override
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) {
      return cw.visitField(access, name, desc, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      return cw.visitMethod(access, name, desc, signature, exceptions);
   }

   @Override
   public void visitEnd() {
      cw.visitEnd();
   }

   @Override
   public final byte[] toByteArray() { return cw.toByteArray(); }
}
