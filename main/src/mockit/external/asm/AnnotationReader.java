package mockit.external.asm;

import java.lang.reflect.*;
import javax.annotation.*;

final class AnnotationReader extends BytecodeReader
{
   AnnotationReader(@Nonnull BytecodeReader br) { super(br); }

   /**
    * Reads the values of a named annotation and makes the given visitor visit them.
    *
    * @param v  the start offset in {@link #code} of the values to be read (including the unsigned short that gives the
    *           number of values).
    * @param av the visitor that must visit the values.
    * @return the end offset of the annotation values.
    */
   @Nonnegative
   int readNamedAnnotationValues(@Nonnegative int v, @Nullable AnnotationVisitor av) {
      return readAnnotationValues(v, true, av);
   }

   /**
    * Reads the values of an unnamed annotation and makes the given visitor visit them.
    *
    * @param v  the start offset in {@link #code} of the values to be read (including the unsigned short that gives the
    *           number of values).
    * @param av the visitor that must visit the values.
    * @return the end offset of the annotation values.
    */
   @Nonnegative
   private int readUnnamedAnnotationValues(@Nonnegative int v, @Nullable AnnotationVisitor av) {
      return readAnnotationValues(v, false, av);
   }

   @Nonnegative
   private int readAnnotationValues(@Nonnegative int v, boolean named, @Nullable AnnotationVisitor av) {
      int valueCount = readUnsignedShort(v);
      v += 2;

      while (valueCount > 0) {
         String name = null;

         if (named) {
            name = readUTF8(v);
            v += 2;
         }

         v = readAnnotationValue(v, name, av);
         valueCount--;
      }

      if (av != null) {
         av.visitEnd();
      }

      return v;
   }

   /**
    * Reads the default value of an annotation and makes the given visitor visit it.
    *
    * @param v  the start offset in {@link #code} of the value to be read (<i>not including the value name constant pool
    *           index</i>).
    * @param av the visitor that must visit the value.
    */
   @Nonnegative
   void readDefaultAnnotationValue(@Nonnegative int v, @Nullable AnnotationVisitor av) {
      readAnnotationValue(v, null, av);
   }

   /**
    * Reads a value of an annotation and makes the given visitor visit it.
    *
    * @param v    the start offset in {@link #code} of the value to be read (<i>not including the value name constant
    *             pool index</i>).
    * @param name the name of the value to be read.
    * @return the end offset of the annotation value.
    */
   @Nonnegative
   private int readAnnotationValue(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      int typeCode = code[v++] & 0xFF;

      if (av == null) {
         return readAnnotationValue(v, typeCode);
      }

      Object value = readAnnotationValueIfPrimitiveOrString(v, typeCode);

      if (value != null) {
         av.visit(name, value);
         v += 2;
         return v;
      }

      switch (typeCode) {
         case 'e': return readEnumConstValue(v, name, av);   // enum_const_value
         case 'c': return readClassInfo(v, name, av);        // class_info
         case '@': return readAnnotationValue2(v, name, av); // annotation_value
         case '[': return readArrayValue(v, name, av);       // array_value
         default: return v;
      }
   }

   @Nonnegative
   private int readAnnotationValue(@Nonnegative int v, int typeCode) {
      switch (typeCode) {
         case 'e': return v + 4;                                  // enum_const_value
         case '@': return readNamedAnnotationValues(v + 2, null); // annotation_value
         case '[': return readUnnamedAnnotationValues(v, null);   // array_value
         default:  return v + 2;
      }
   }

   @Nullable
   private Object readAnnotationValueIfPrimitiveOrString(@Nonnegative int v, int typeCode) {
      switch (typeCode) {
         case 'I': case 'J': case 'F': case 'D':
            return readConst(readUnsignedShort(v));            // CONSTANT_Integer/Long/Float/Double
         case 'B': return (byte) readValueOfOneOrTwoBytes(v);  // CONSTANT_Byte
         case 'Z': return readValueOfOneOrTwoBytes(v) != 0;    // CONSTANT_Boolean
         case 'S': return (short) readValueOfOneOrTwoBytes(v); // CONSTANT_Short
         case 'C': return (char)  readValueOfOneOrTwoBytes(v); // CONSTANT_Char
         case 's': return readUTF8(v);                         // CONSTANT_Utf8
         default:  return null;
      }
   }

   private int readValueOfOneOrTwoBytes(@Nonnegative int v) {
      int itemIndex = readUnsignedShort(v);
      int item = items[itemIndex];
      return readInt(item);
   }

   @Nonnegative
   private int readEnumConstValue(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      String enumDesc = readUTF8(v);
      String enumValue = readUTF8(v + 2);

      //noinspection ConstantConditions
      av.visitEnum(name, enumDesc, enumValue);

      return v + 4;
   }

   @Nonnegative @SuppressWarnings("ConstantConditions")
   private int readClassInfo(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      String typeDesc = readUTF8(v);
      ReferenceType value = ReferenceType.createFromTypeDescriptor(typeDesc);
      av.visit(name, value);
      return v + 2;
   }

   @Nonnegative
   private int readAnnotationValue2(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      String desc = readUTF8(v);
      //noinspection ConstantConditions
      AnnotationVisitor nestedVisitor = av.visitAnnotation(name, desc);

      return readNamedAnnotationValues(v + 2, nestedVisitor);
   }

   @Nonnegative
   private int readArrayValue(@Nonnegative int v, @Nullable String name, @Nonnull AnnotationVisitor av) {
      int size = readUnsignedShort(v);

      if (size == 0) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readUnnamedAnnotationValues(v, arrayVisitor);
      }

      v += 2;
      int typeCode = code[v] & 0xFF;
      PrimitiveType primitiveElementType = PrimitiveType.getPrimitiveType(typeCode);

      if (primitiveElementType == null) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readUnnamedAnnotationValues(v - 2, arrayVisitor);
      }

      Class<?> elementType = primitiveElementType.getType();
      Object array = Array.newInstance(elementType, size);
      v++;

      v = fillArrayElements(v, size, typeCode, array);

      av.visit(name, array);
      v--;
      return v;
   }

   @Nonnegative
   private int fillArrayElements(@Nonnegative int v, @Nonnegative int length, int typeCode, @Nonnull Object array) {
      for (int i = 0; i < length; i++) {
         int index = items[readUnsignedShort(v)];
         Object value = getArrayElementValue(typeCode, index);
         Array.set(array, i, value);
         v += 3;
      }

      return v;
   }

   @Nonnull
   private Object getArrayElementValue(int typeCode, @Nonnegative int index) {
      switch (typeCode) {
         case 'Z': return readBoolean(index);
         case 'C': return readChar(index);
         case 'B': return readByte(index);
         case 'S': return readShort(index);
         case 'F': return readFloat(index);
         case 'D': return readDouble(index);
         case 'J': return readLong(index);
         default:  return readInt(index);
      }
   }
}
