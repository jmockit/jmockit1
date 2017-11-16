package mockit.external.asm;

import java.lang.reflect.*;

final class AnnotationReader extends BytecodeReader
{
   AnnotationReader(BytecodeReader br) { super(br); }

   /**
    * Reads the values of an annotation and makes the given visitor visit them.
    *
    * @param v     the start offset in {@link #b b} of the values to be read
    *              (including the unsigned short that gives the number of values).
    * @param buf   buffer to be used to call {@link #readUTF8 readUTF8},
    *              {@link #readClass(int, char[]) readClass} or {@link #readConst readConst}.
    * @param named if the annotation values are named or not.
    * @param av    the visitor that must visit the values.
    * @return the end offset of the annotation values.
    */
   int readAnnotationValues(int v, char[] buf, boolean named, AnnotationVisitor av) {
      int i = readUnsignedShort(v);
      v += 2;

      if (named) {
         for (; i > 0; i--) {
            String name = readUTF8(v, buf);
            v = readAnnotationValue(v + 2, buf, name, av);
         }
      }
      else {
         for (; i > 0; --i) {
            v = readAnnotationValue(v, buf, null, av);
         }
      }

      if (av != null) {
         av.visitEnd();
      }

      return v;
   }

   /**
    * Reads a value of an annotation and makes the given visitor visit it.
    *
    * @param v    the start offset in {@link #b} of the value to be read
    *             (<i>not including the value name constant pool index</i>).
    * @param buf  buffer to be used to call {@link #readUTF8 readUTF8},
    *             {@link #readClass(int, char[]) readClass} or {@link #readConst readConst}.
    * @param name the name of the value to be read.
    * @param av   the visitor that must visit the value.
    * @return the end offset of the annotation value.
    */
   int readAnnotationValue(int v, char[] buf, String name, AnnotationVisitor av) {
      if (av == null) {
         switch (b[v] & 0xFF) {
            case 'e': // enum_const_value
               return v + 5;
            case '@': // annotation_value
               return readAnnotationValues(v + 3, buf, true, null);
            case '[': // array_value
               return readAnnotationValues(v + 1, buf, false, null);
            default:
               return v + 3;
         }
      }

      Object value;

      switch (b[v++] & 0xFF) {
         case 'I': // pointer to CONSTANT_Integer
         case 'J': // pointer to CONSTANT_Long
         case 'F': // pointer to CONSTANT_Float
         case 'D': // pointer to CONSTANT_Double
            value = readConst(readUnsignedShort(v), buf);
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
            value = readUTF8(v, buf);
            av.visit(name, value);
            v += 2;
            break;
         case 'e': // enum_const_value
            String enumDesc = readUTF8(v, buf);
            String enumValue = readUTF8(v + 2, buf);
            av.visitEnum(name, enumDesc, enumValue);
            v += 4;
            break;
         case 'c': // class_info
            value = Type.getType(readUTF8(v, buf));
            av.visit(name, value);
            v += 2;
            break;
         case '@': // annotation_value
            String desc = readUTF8(v, buf);
            AnnotationVisitor nestedVisitor = av.visitAnnotation(name, desc);
            v = readAnnotationValues(v + 2, buf, true, nestedVisitor);
            break;
         case '[': // array_value
            int size = readUnsignedShort(v);
            v += 2;

            if (size == 0) {
               AnnotationVisitor arrayVisitor = av.visitArray(name);
               return readAnnotationValues(v - 2, buf, false, arrayVisitor);
            }

            v = readAnnotationArrayValue(v, buf, name, av, size);
      }

      return v;
   }

   private int readAnnotationArrayValue(int v, char[] buf, String name, AnnotationVisitor av, int size) {
      int typeCode = b[v++] & 0xFF;

      if ("BZSCIJFD".indexOf(typeCode) < 0) {
         AnnotationVisitor arrayVisitor = av.visitArray(name);
         return readAnnotationValues(v - 3, buf, false, arrayVisitor);
      }

      Class<?> elementType = Type.getPrimitiveType(typeCode);
      Object array = Array.newInstance(elementType, size);

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
