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

      for (; valueCount > 0; valueCount--) {
         String name = null;

         if (named) {
            name = readUTF8(v);
            v += 2;
         }

         v = readAnnotationValue(v, name, av);
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
      if (av == null) {
         return readAnnotationValue(v);
      }

      int typeCode = code[v++] & 0xFF;
      Object value;

      switch (typeCode) {
         case 'I': // pointer to CONSTANT_Integer
         case 'J': // pointer to CONSTANT_Long
         case 'F': // pointer to CONSTANT_Float
         case 'D': // pointer to CONSTANT_Double
            value = readConst(readUnsignedShort(v));
            av.visit(name, value);
            v += 2;
            break;
         case 'B': // pointer to CONSTANT_Byte
            value = (byte) readInt(items[readUnsignedShort(v)]);
            av.visit(name, value);
            v += 2;
            break;
         case 'Z': // pointer to CONSTANT_Boolean
            value = readInt(items[readUnsignedShort(v)]) == 0 ? Boolean.FALSE : Boolean.TRUE;
            av.visit(name, value);
            v += 2;
            break;
         case 'S': // pointer to CONSTANT_Short
            value = (short) readInt(items[readUnsignedShort(v)]);
            av.visit(name, value);
            v += 2;
            break;
         case 'C': // pointer to CONSTANT_Char
            value = (char) readInt(items[readUnsignedShort(v)]);
            av.visit(name, value);
            v += 2;
            break;
         case 's': // pointer to CONSTANT_Utf8
            value = readUTF8(v);
            //noinspection ConstantConditions
            av.visit(name, value);
            v += 2;
            break;
         case 'e': // enum_const_value
            v = readEnumConstValue(v, name, av);
            break;
         case 'c': // class_info
            v = readClassInfo(v, name, av);
            break;
         case '@': // annotation_value
            v = readAnnotationValue2(v, name, av);
            break;
         case '[': // array_value
            v = readArrayValue(v, name, av);
      }

      return v;
   }

   @Nonnegative
   private int readAnnotationValue(@Nonnegative int v) {
      int typeCode = code[v] & 0xFF;

      switch (typeCode) {
         case 'e': // enum_const_value
            return v + 5;
         case '@': // annotation_value
            return readNamedAnnotationValues(v + 3, null);
         case '[': // array_value
            return readUnnamedAnnotationValues(v + 1, null);
         default:
            return v + 3;
      }
   }

   @Nonnegative
   private int readEnumConstValue(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      String enumDesc = readUTF8(v);
      String enumValue = readUTF8(v + 2);

      //noinspection ConstantConditions
      av.visitEnum(name, enumDesc, enumValue);

      return v + 4;
   }

   @Nonnegative
   private int readClassInfo(@Nonnegative int v, @Nullable String name, @Nullable AnnotationVisitor av) {
      String typeDesc = readUTF8(v);
      @SuppressWarnings("ConstantConditions") Object value = JavaType.getType(typeDesc);

      //noinspection ConstantConditions
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

      if ("BZSCIJFD".indexOf(typeCode) < 0) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readUnnamedAnnotationValues(v - 2, arrayVisitor);
      }

      Class<?> elementType = PrimitiveType.getType(typeCode);
      Object array = Array.newInstance(elementType, size);
      v++;

      for (int i = 0; i < size; i++) {
         int index = items[readUnsignedShort(v)];
         Object value;

         switch (typeCode) {
            case 'B':
               value = (byte) readInt(index);
               break;
            case 'Z':
               value = readInt(index) != 0;
               break;
            case 'S':
               value = (short) readInt(index);
               break;
            case 'C':
               value = (char) readInt(index);
               break;
            case 'I':
               value = readInt(index);
               break;
            case 'J':
               value = readLong(index);
               break;
            case 'F':
               int floatBits = readInt(index);
               value = Float.intBitsToFloat(floatBits);
               break;
            default: // 'D'
               long doubleBits = readLong(index);
               value = Double.longBitsToDouble(doubleBits);
         }

         Array.set(array, i, value);
         v += 3;
      }

      av.visit(name, array);
      v--;
      return v;
   }
}
