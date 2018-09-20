package mockit.asm;

import javax.annotation.*;

/**
 * A visitor to visit a Java class. The methods of this class are called in the following order:
 * <tt>visit</tt> [<tt>visitSource</tt>] (<tt>visitAnnotation</tt>)*
 * (<tt>visitInnerClass</tt> | <tt>visitField</tt> | <tt>visitMethod</tt>)* <tt>visitEnd</tt>.
 */
public class ClassVisitor extends BaseWriter
{
   protected ClassVisitor() {}

   /**
    * Visits the header of the class.
    *
    * @param version    the class version.
    * @param access     the class's access flags (see {@link Access}).
    * @param name       the internal name of the class.
    * @param signature  the signature of this class. May be <tt>null</tt> if the class is not a generic one, and does not extend or
    *                   implement generic classes or interfaces.
    * @param superName  the internal name of the super class. For interfaces, the super class is {@link Object}. May be <tt>null</tt>, but
    *                   only for the {@link Object} class.
    * @param interfaces the internal names of the class's interfaces.
    */
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName, @Nullable String[] interfaces
   ) {}

   /**
    * Visits the source of the class.
    *
    * @param source the name of the source file from which the class was compiled. May be <tt>null</tt>.
    */
   public void visitSource(@Nullable String source) {}

   /**
    * Visits information about an inner class. This inner class is not necessarily a member of the class being visited.
    *
    * @param name      the internal name of an inner class.
    * @param outerName the internal name of the class to which the inner class belongs. May be <tt>null</tt> for not member classes.
    * @param innerName the (simple) name of the inner class inside its enclosing class. May be <tt>null</tt> for anonymous inner classes.
    * @param access    the access flags of the inner class as originally declared in the enclosing class.
    */
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {}

   /**
    * Visits a field of the class.
    *
    * @param access    the field's access flags (see {@link Access}).
    * @param name      the field's name.
    * @param desc      the field's descriptor (see {@link JavaType}).
    * @param signature the field's signature. May be <tt>null</tt> if the field's type does not use generic types.
    * @param value     the field's initial value. This parameter, which may be <tt>null</tt> if the field does not have an initial value,
    *                  must be an {@link Integer}, a {@link Float}, a {@link Long}, a {@link Double} or a {@link String} (for
    *                  <tt>int</tt>, <tt>float</tt>, <tt>long</tt> or <tt>String</tt> fields respectively).
    *                  <i>This parameter is only used for static fields</i>. Its value is ignored for non static fields, which must be
    *                  initialized through bytecode instructions in constructors or methods.
    * @return a visitor to visit field annotations and attributes, or <tt>null</tt> if this class visitor is not interested in visiting
    * these annotations and attributes.
    */
   @Nullable
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value) { return null; }

   /**
    * Visits a method of the class. This method <i>must</i> return a new {@link MethodVisitor} instance (or <tt>null</tt>) each time it is
    * called, i.e., it should not return a previously returned visitor.
    *
    * @param access     the method's access flags (see {@link Opcodes}).
    * @param name       the method's name.
    * @param desc       the method's descriptor (see {@link JavaType}).
    * @param signature  the method's signature. May be <tt>null</tt> if the method parameters, return type and exceptions do not use
    *                   generic types.
    * @param exceptions the internal names of the method's exception classes.
    * @return an object to visit the byte code of the method, or <tt>null</tt> if this class visitor is not interested in visiting the code
    * of this method.
    */
   @Nullable
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) { return null; }

   /**
    * Returns the bytecode of the class that was built with this class visitor.
    */
   public byte[] toByteArray() { return null; }
}
