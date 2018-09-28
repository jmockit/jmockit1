package mockit.asm;

import javax.annotation.*;

import mockit.asm.types.*;

/**
 * A visitor to visit a Java annotation, in the following order:
 * (<tt>visit</tt> | <tt>visitEnum</tt> | <tt>visitAnnotation</tt> | <tt>visitArray</tt>)* <tt>visitEnd</tt>.
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
    * @param name the value name
    * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link Character}, {@link Short}, {@link Integer},
    * {@link Long}, {@link Float}, {@link Double}, {@link String}, or {@link JavaType} of OBJECT or ARRAY sort;
    * this value can also be an array of byte, boolean, short, char, int, long, float or double values (this is equivalent to using
    * {@link #visitArray} and visiting each array element in turn, but is more convenient)
    */
   abstract void visit(@Nullable String name, @Nonnull Object value);

   /**
    * Visits an enumeration value of the annotation.
    *
    * @param name the value name
    * @param desc the class descriptor of the enumeration class
    * @param value the actual enumeration value
    */
   abstract void visitEnum(@Nullable String name, @Nonnull String desc, @Nonnull String value);

   /**
    * Visits a nested annotation value of the annotation.
    *
    * @param name the value name
    * @param desc the class descriptor of the nested annotation class
    * @return a visitor to visit the actual nested annotation value, or <tt>null</tt> if this visitor is not interested in visiting this
    * nested annotation
    */
   @Nullable
   AnnotationVisitor visitAnnotation(@Nullable String name, @Nonnull String desc) { return null; }

   /**
    * Visits an array value of the annotation.
    * Note that arrays of primitive types can be passed as value to {@link #visit(String, Object)}. This is what {@link ClassReader} does.
    *
    * @param name the value name
    * @return a visitor to visit the actual array value elements, or <tt>null</tt> if this visitor is not interested in visiting these
    * values; the 'name' parameters passed to the methods of this visitor are ignored
    */
   @Nullable
   AnnotationVisitor visitArray(@Nullable String name) { return null; }

   /**
    * Visits the end of the annotation.
    */
   abstract void visitEnd();

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
