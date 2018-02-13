package mockit.external.asm;

import javax.annotation.*;

/**
 * A visitor to visit a Java annotation. The methods of this class must be called in the following order:
 * ( <tt>visit</tt> | <tt>visitEnum</tt> | <tt>visitAnnotation</tt> | <tt>visitArray</tt> )* <tt>visitEnd</tt>.
 */
public abstract class AnnotationVisitor
{
   /**
    * Next annotation visitor. This field is used to store annotation lists.
    */
   @SuppressWarnings("ClassReferencesSubclass") @Nullable AnnotationWriter next;

   /**
    * Previous annotation visitor. This field is used to store annotation lists.
    */
   @SuppressWarnings("ClassReferencesSubclass") @Nullable AnnotationWriter prev;

   /**
    * Constructs a new AnnotationVisitor.
    */
   protected AnnotationVisitor() {}

   @Nonnegative
   protected abstract int getByteLength();

   /**
    * Visits a primitive, String, Class, or array value of the annotation.
    *
    * @param name  the value name.
    * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link Character},
    *              {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link String} or
    *              {@link JavaType} of OBJECT or ARRAY sort. This value can also be an array of byte, boolean, short,
    *              char, int, long, float or double values (this is equivalent to using {@link #visitArray} and visiting
    *              each array element in turn, but is more convenient).
    */
   public void visit(@Nullable String name, @Nonnull Object value) {}

   /**
    * Visits an enumeration value of the annotation.
    *
    * @param name  the value name.
    * @param desc  the class descriptor of the enumeration class.
    * @param value the actual enumeration value.
    */
   public void visitEnum(@Nullable String name, @Nonnull String desc, @Nonnull String value) {}

   /**
    * Visits a nested annotation value of the annotation.
    *
    * @param name the value name.
    * @param desc the class descriptor of the nested annotation class.
    * @return a visitor to visit the actual nested annotation value, or <tt>null</tt> if this visitor is not interested
    * in visiting this nested annotation. <i>The nested annotation value must be fully visited before calling
    * other methods on this annotation visitor</i>.
    */
   @Nullable
   public AnnotationVisitor visitAnnotation(@Nullable String name, @Nonnull String desc) { return null; }

   /**
    * Visits an array value of the annotation. Note that arrays of primitive types can be passed as value to
    * {@link #visit(String, Object)}. This is what {@link ClassReader} does.
    *
    * @param name the value name.
    * @return a visitor to visit the actual array value elements, or <tt>null</tt> if this visitor is not interested in
    * visiting these values. The 'name' parameters passed to the methods of this visitor are ignored.
    * <i>All the array values must be visited before calling other methods on this annotation visitor</i>.
    */
   @Nullable
   public AnnotationVisitor visitArray(@Nullable String name) { return null; }

   /**
    * Visits the end of the annotation.
    */
   public void visitEnd() {}

   /**
    * Returns the size of this annotation list.
    */
   @Nonnegative
   final int getSize() {
      int size = 0;
      AnnotationVisitor annotation = this;

      while (annotation != null) {
         size += annotation.getByteLength();
         annotation = annotation.next;
      }

      return size;
   }
}
