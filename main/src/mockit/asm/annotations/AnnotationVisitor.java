package mockit.asm.annotations;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.types.*;
import mockit.asm.util.*;

/**
 * A visitor to visit a Java annotation, in the following order:
 * (<tt>visit</tt> | <tt>visitEnum</tt> | <tt>visitAnnotation</tt> | <tt>visitArray</tt>)* <tt>visitEnd</tt>.
 */
public final class AnnotationVisitor
{
   /**
    * The constant pool to which this annotation must be added.
    */
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * The number of attribute values in this annotation.
    */
   @Nonnegative private int attributeCount;

   /**
    * <tt>true<tt> if values are named, <tt>false</tt> otherwise.
    * Annotation writers used for annotation default and annotation arrays use unnamed values.
    */
   private final boolean named;

   /**
    * The annotation values in bytecode form. This byte vector only contains the values themselves, i.e. the number of values must be
    * stored as an unsigned short just before these bytes.
    */
   @Nonnull private final ByteVector bv;

   /**
    * Where the number of values of this annotation must be stored in {@link #bv}.
    */
   @Nonnegative private final int offset;

   /**
    * Next annotation visitor. This field is used to store annotation lists.
    */
   @Nullable private AnnotationVisitor next;

   /**
    * Previous annotation visitor. This field is used to store annotation lists.
    */
   @Nullable private AnnotationVisitor prev;

   public AnnotationVisitor(@Nonnull ConstantPoolGeneration cp, @Nonnull String typeDesc) {
      this.cp = cp;
      named = true;
      bv = new ByteVector();
      bv.putShort(cp.newUTF8(typeDesc));
      bv.putShort(0); // reserve space for value count
      offset = 2;
   }

   private AnnotationVisitor(@Nonnull AnnotationVisitor parent, boolean named) {
      cp = parent.cp;
      this.named = named;
      bv = parent.bv;
      offset = getByteLength() - 2;
   }

   @Nonnegative
   private int getByteLength() { return bv.getLength(); }

   /**
    * Sets the visitor to the {@link #next} annotation.
    */
   public void setNext(@Nullable AnnotationVisitor next) { this.next = next; }

   /**
    * Visits a primitive, String, Class, or array value of the annotation.
    *
    * @param name the value name
    * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link Character}, {@link Short}, {@link Integer},
    * {@link Long}, {@link Float}, {@link Double}, {@link String}, or {@link JavaType} of OBJECT or ARRAY sort;
    * this value can also be an array of byte, boolean, short, char, int, long, float or double values (this is equivalent to using
    * {@link #visitArray} and visiting each array element in turn, but is more convenient)
    */
   void visit(@Nullable String name, @Nonnull Object value) {
      putName(name);

      if (value instanceof String) {
         putString('s', (String) value);
      }
      else if (putValueWhenPrimitive(value)) {
         // OK
      }
      else if (value instanceof JavaType) {
         putType((JavaType) value);
      }
      else {
         putElementValuesWhenArray(value);
      }
   }

   private void putName(@Nullable String name) {
      attributeCount++;

      if (named) {
         //noinspection ConstantConditions
         putString(name);
      }
   }

   private boolean putValueWhenPrimitive(@Nonnull Object value) {
      if (value instanceof Boolean) {
         putBoolean((Boolean) value);
      }
      else if (value instanceof Integer) {
         putInteger('I', (Integer) value);
      }
      else if (value instanceof Double) {
         putDouble((Double) value);
      }
      else if (value instanceof Float) {
         putFloat((Float) value);
      }
      else if (value instanceof Long) {
         putLong((Long) value);
      }
      else if (value instanceof Byte) {
         putInteger('B', (Byte) value);
      }
      else if (value instanceof Character) {
         putInteger('C', (Character) value);
      }
      else if (value instanceof Short) {
         putInteger('S', (Short) value);
      }
      else {
         return false;
      }

      return true;
   }

   private void putItem(int typeCode, @Nonnull Item item) {
      bv.put12(typeCode, item.index);
   }

   private void putBoolean(boolean value) {
      putInteger('Z', value ? 1 : 0);
   }

   private void putInteger(int typeCode, int value) {
      Item item = cp.newInteger(value);
      putItem(typeCode, item);
   }

   private void putDouble(double value) {
      Item item = cp.newDouble(value);
      putItem('D', item);
   }

   private void putFloat(float value) {
      Item item = cp.newFloat(value);
      putItem('F', item);
   }

   private void putLong(long value) {
      Item item = cp.newLong(value);
      putItem('J', item);
   }

   private void putType(@Nonnull JavaType type) {
      String typeDescriptor = type.getDescriptor();
      putString('c', typeDescriptor);
   }

   private void putString(int b, @Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.put12(b, itemIndex);
   }

   private void putString(@Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.putShort(itemIndex);
   }

   private void putArrayLength(@Nonnegative int length) {
      bv.put12('[', length);
   }

   private void putElementValuesWhenArray(@Nonnull Object value) {
      if (value instanceof byte[]) {
         putArrayElementValues('B', value);
      }
      else if (value instanceof boolean[]) {
         putArrayElementValues('Z', value);
      }
      else if (value instanceof short[]) {
         putArrayElementValues('S', value);
      }
      else if (value instanceof char[]) {
         putArrayElementValues('C', value);
      }
      else if (value instanceof int[]) {
         putArrayElementValues('I', value);
      }
      else if (value instanceof long[]) {
         putArrayElementValues('J', value);
      }
      else if (value instanceof float[]) {
         putArrayElementValues('F', value);
      }
      else if (value instanceof double[]) {
         putArrayElementValues('D', value);
      }
   }

   private void putArrayElementValues(char elementType, @Nonnull Object array) {
      int length = Array.getLength(array);
      putArrayLength(length);

      for (int i = 0; i < length; i++) {
         if (elementType == 'J') {
            long value = Array.getLong(array, i);
            putLong(value);
         }
         else if (elementType == 'F') {
            float value = Array.getFloat(array, i);
            putFloat(value);
         }
         else if (elementType == 'D') {
            double value = Array.getDouble(array, i);
            putDouble(value);
         }
         else if (elementType == 'Z') {
            boolean value = Array.getBoolean(array, i);
            putBoolean(value);
         }
         else {
            int value = Array.getInt(array, i);
            putInteger(elementType, value);
         }
      }
   }

   /**
    * Visits an enumeration value of the annotation.
    *
    * @param name the value name
    * @param desc the class descriptor of the enumeration class
    * @param value the actual enumeration value
    */
   void visitEnum(@Nullable String name, @Nonnull String desc, @Nonnull String value) {
      putName(name);
      putString('e', desc);
      putString(value);
   }

   /**
    * Visits a nested annotation value of the annotation.
    *
    * @param name the value name
    * @param desc the class descriptor of the nested annotation class
    * @return a visitor to visit the actual nested annotation value
    */
   @Nonnull
   AnnotationVisitor visitAnnotation(@Nullable String name, @Nonnull String desc) {
      putName(name);

      // Write tag and type, and reserve space for value count.
      putString('@', desc);
      bv.putShort(0);

      return new AnnotationVisitor(this, true);
   }

   /**
    * Visits an array value of the annotation.
    * Note that arrays of primitive types can be passed as value to {@link #visit(String, Object)}.
    *
    * @param name the value name
    * @return a visitor to visit the actual array value elements; the 'name' parameters passed to the methods of this visitor are ignored
    */
   @Nonnull
   AnnotationVisitor visitArray(@Nullable String name) {
      putName(name);

      // Write tag, and reserve space for array size.
      putArrayLength(0);

      return new AnnotationVisitor(this, false);
   }

   /**
    * Visits the end of the annotation.
    */
   @SuppressWarnings("NumericCastThatLosesPrecision")
   void visitEnd() {
      byte[] data = bv.getData();
      data[offset] = (byte) (attributeCount >>> 8);
      data[offset + 1] = (byte) attributeCount;
   }

   /**
    * Returns the size of this annotation list.
    */
   @Nonnegative
   public int getSize() {
      int size = 0;
      AnnotationVisitor annotation = this;

      while (annotation != null) {
         size += annotation.getByteLength();
         annotation = annotation.next;
      }

      return size;
   }

   /**
    * Puts the annotations of this annotation writer list into the given byte vector.
    */
   public void put(@Nonnull ByteVector out) {
      AnnotationVisitor aw = this;
      AnnotationVisitor last = null;
      int n = 0;
      int size = 2;

      while (aw != null) {
         n++;
         size += aw.getByteLength();
         aw.prev = last;
         last = aw;
         aw = aw.next;
      }

      out.putInt(size);
      out.putShort(n);
      putFromLastToFirst(out, last);
   }

   private static void putFromLastToFirst(@Nonnull ByteVector out, @Nullable AnnotationVisitor aw) {
      while (aw != null) {
         out.putByteVector(aw.bv);
         aw = aw.prev;
      }
   }

   /**
    * Puts the given annotation lists into the given byte vector.
    */
   public static void put(@Nonnull ByteVector out, @Nonnull AnnotationVisitor[] anns) {
      putNumberAndSizeOfAnnotations(out, anns);

      for (AnnotationVisitor ann : anns) {
         AnnotationVisitor last = putNumberOfAnnotations(out, ann);
         putFromLastToFirst(out, last);
      }
   }

   private static void putNumberAndSizeOfAnnotations(@Nonnull ByteVector out, @Nonnull AnnotationVisitor[] anns) {
      int numAnns = anns.length;
      int size = 1 + 2 * numAnns;

      for (AnnotationVisitor aw : anns) {
         if (aw != null) {
            size += aw.getSize();
         }
      }

      out.putInt(size).putByte(numAnns);
   }

   @Nullable
   private static AnnotationVisitor putNumberOfAnnotations(@Nonnull ByteVector out, @Nullable AnnotationVisitor aw) {
      AnnotationVisitor last = null;
      int n = 0;

      while (aw != null) {
         n++;
         aw.prev = last;
         last = aw;
         aw = aw.next;
      }

      out.putShort(n);
      return last;
   }
}
