package mockit.external.asm;

import javax.annotation.*;

/**
 * Same as {@link ClassVisitor}, except it wraps another {@code ClassVisitor}.
 */
public class WrappingClassVisitor extends ClassVisitor
{
    /**
     * The class visitor to which this visitor must delegate method calls.
     */
    @Nonnull protected final ClassVisitor cv;

    /**
     * Constructs a new {@link WrappingClassVisitor}.
     *
     * @param cv the class visitor to which this visitor must delegate method calls.
     */
    protected WrappingClassVisitor(@Nonnull ClassVisitor cv) { this.cv = cv; }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        cv.visitSource(source, debug);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        cv.visitOuterClass(owner, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return cv.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        cv.visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cv.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return cv.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        cv.visitEnd();
    }

    @Override
    public final byte[] toByteArray() { return cv.toByteArray(); }
}
