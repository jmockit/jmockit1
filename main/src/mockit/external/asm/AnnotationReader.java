package mockit.external.asm;

import java.lang.reflect.*;
import javax.annotation.*;

final class AnnotationReader extends BytecodeReader
{
   AnnotationReader(@Nonnull BytecodeReader br) { super(br); }

   /**
    * Reads the values of a named annotation and makes the given visitor visit them.
    *
    * @param codeIndex the start offset in {@link #code} of the values to be read (including the unsigned short that
    *                  gives the number of values).
    * @param av the visitor that must visit the values.
    * @return the end offset of the annotation values.
    */
   @Nonnegative
   int readNamedAnnotationValues(@Nonnegative int codeIndex, @Nullable AnnotationVisitor av) {
      return readAnnotationValues(codeIndex, true, av);
   }

   /**
    * Reads the values of an unnamed annotation and makes the given visitor visit them.
    *
    * @param codeIndex the start offset in {@link #code} of the values to be read (including the unsigned short that
    *                  gives the number of values).
    * @param av the visitor that must visit the values.
    * @return the end offset of the annotation values.
    */
   @Nonnegative
   private int readUnnamedAnnotationValues(@Nonnegative int codeIndex, @Nullable AnnotationVisitor av) {
      return readAnnotationValues(codeIndex, false, av);
   }

   @Nonnegative
   private int readAnnotationValues(@Nonnegative int codeIndex, boolean named, @Nullable AnnotationVisitor av) {
      int valueCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      while (valueCount > 0) {
         String name = null;

         if (named) {
            name = readUTF8(codeIndex);
            codeIndex += 2;
         }

         codeIndex = readAnnotationValue(codeIndex, name, av);
         valueCount--;
      }

      if (av != null) {
         av.visitEnd();
      }

      return codeIndex;
   }

   /**
    * Reads the default value of an annotation and makes the given visitor visit it.
    *
    * @param codeIndex the start offset in {@link #code} of the value to be read (<i>not including the value name
    *                  constant pool index</i>).
    * @param av the visitor that must visit the value.
    */
   @Nonnegative
   void readDefaultAnnotationValue(@Nonnegative int codeIndex, @Nullable AnnotationVisitor av) {
      readAnnotationValue(codeIndex, null, av);
   }

   /**
    * Reads a value of an annotation and makes the given visitor visit it.
    *
    * @param codeIndex the start offset in {@link #code} of the value to be read (<i>not including the value name
    *                  constant pool index</i>).
    * @param name the name of the value to be read.
    * @return the end offset of the annotation value.
    */
   @Nonnegative
   private int readAnnotationValue(@Nonnegative int codeIndex, @Nullable String name, @Nullable AnnotationVisitor av) {
      int typeCode = code[codeIndex++] & 0xFF;

      if (av == null) {
         return readAnnotationValue(codeIndex, typeCode);
      }

      Object value = readAnnotationValueIfPrimitiveOrString(codeIndex, typeCode);

      if (value != null) {
         av.visit(name, value);
         codeIndex += 2;
         return codeIndex;
      }

      switch (typeCode) {
         case 'e': return readEnumConstValue(codeIndex, name, av);   // enum_const_value
         case 'c': return readClassInfo(codeIndex, name, av);        // class_info
         case '@': return readAnnotationValue2(codeIndex, name, av); // annotation_value
         case '[': return readArrayValue(codeIndex, name, av);       // array_value
         default: return codeIndex;
      }
   }

   @Nonnegative
   private int readAnnotationValue(@Nonnegative int codeIndex, int typeCode) {
      switch (typeCode) {
         case 'e': return codeIndex + 4;                                  // enum_const_value
         case '@': return readNamedAnnotationValues(codeIndex + 2, null); // annotation_value
         case '[': return readUnnamedAnnotationValues(codeIndex, null);   // array_value
         default:  return codeIndex + 2;
      }
   }

   @Nullable
   private Object readAnnotationValueIfPrimitiveOrString(@Nonnegative int codeIndex, int typeCode) {
      switch (typeCode) {
         case 'I': case 'J': case 'F': case 'D': return readConstItem(codeIndex); // CONSTANT_Integer/Long/Float/Double
         case 'B': return (byte) readValueOfOneOrTwoBytes(codeIndex);             // CONSTANT_Byte
         case 'Z': return readValueOfOneOrTwoBytes(codeIndex) != 0;               // CONSTANT_Boolean
         case 'S': return (short) readValueOfOneOrTwoBytes(codeIndex);            // CONSTANT_Short
         case 'C': return (char)  readValueOfOneOrTwoBytes(codeIndex);            // CONSTANT_Char
         case 's': return readUTF8(codeIndex);                                    // CONSTANT_Utf8
         default:  return null;
      }
   }

   private int readValueOfOneOrTwoBytes(@Nonnegative int codeIndex) {
      int itemIndex = readUnsignedShort(codeIndex);
      int item = items[itemIndex];
      return readInt(item);
   }

   @Nonnegative
   private int readEnumConstValue(@Nonnegative int codeIndex, @Nullable String name, @Nullable AnnotationVisitor av) {
      String enumDesc = readUTF8(codeIndex);
      String enumValue = readUTF8(codeIndex + 2);

      //noinspection ConstantConditions
      av.visitEnum(name, enumDesc, enumValue);

      return codeIndex + 4;
   }

   @Nonnegative @SuppressWarnings("ConstantConditions")
   private int readClassInfo(@Nonnegative int codeIndex, @Nullable String name, @Nullable AnnotationVisitor av) {
      String typeDesc = readUTF8(codeIndex);
      ReferenceType value = ReferenceType.createFromTypeDescriptor(typeDesc);
      av.visit(name, value);
      return codeIndex + 2;
   }

   @Nonnegative
   private int readAnnotationValue2(@Nonnegative int codeIndex, @Nullable String name, @Nullable AnnotationVisitor av) {
      String desc = readUTF8(codeIndex);
      //noinspection ConstantConditions
      AnnotationVisitor nestedVisitor = av.visitAnnotation(name, desc);

      return readNamedAnnotationValues(codeIndex + 2, nestedVisitor);
   }

   @Nonnegative
   private int readArrayValue(@Nonnegative int codeIndex, @Nullable String name, @Nonnull AnnotationVisitor av) {
      int size = readUnsignedShort(codeIndex);

      if (size == 0) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readUnnamedAnnotationValues(codeIndex, arrayVisitor);
      }

      codeIndex += 2;
      int typeCode = code[codeIndex] & 0xFF;
      PrimitiveType primitiveElementType = PrimitiveType.getPrimitiveType(typeCode);

      if (primitiveElementType == null) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readUnnamedAnnotationValues(codeIndex - 2, arrayVisitor);
      }

      Class<?> elementType = primitiveElementType.getType();
      Object array = Array.newInstance(elementType, size);
      codeIndex++;

      codeIndex = fillArrayElements(codeIndex, size, typeCode, array);

      av.visit(name, array);
      codeIndex--;
      return codeIndex;
   }

   @Nonnegative
   private int fillArrayElements(
      @Nonnegative int codeIndex, @Nonnegative int length, int typeCode, @Nonnull Object array
   ) {
      for (int i = 0; i < length; i++) {
         int index = items[readUnsignedShort(codeIndex)];
         Object value = getArrayElementValue(typeCode, index);
         Array.set(array, i, value);
         codeIndex += 3;
      }

      return codeIndex;
   }

   @Nonnull
   private Object getArrayElementValue(int typeCode, @Nonnegative int codeIndex) {
      switch (typeCode) {
         case 'Z': return readBoolean(codeIndex);
         case 'C': return readChar(codeIndex);
         case 'B': return readByte(codeIndex);
         case 'S': return readShort(codeIndex);
         case 'F': return readFloat(codeIndex);
         case 'D': return readDouble(codeIndex);
         case 'J': return readLong(codeIndex);
         default:  return readInt(codeIndex);
      }
   }
}
