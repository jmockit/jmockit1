package mockit.external.asm;

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
         for (; i > 0; --i) {
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
    * @param v    the start offset in {@link #b b} of the value to be read
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
      int i;

      switch (b[v++] & 0xFF) {
         case 'B':
            byte[] bv = new byte[size];

            for (i = 0; i < size; i++) {
               bv[i] = (byte) readInt(items[readUnsignedShort(v)]);
               v += 3;
            }

            av.visit(name, bv);
            --v;
            break;
         case 'Z':
            boolean[] zv = new boolean[size];

            for (i = 0; i < size; i++) {
               zv[i] = readInt(items[readUnsignedShort(v)]) != 0;
               v += 3;
            }

            av.visit(name, zv);
            --v;
            break;
         case 'S':
            short[] sv = new short[size];

            for (i = 0; i < size; i++) {
               sv[i] = (short) readInt(items[readUnsignedShort(v)]);
               v += 3;
            }

            av.visit(name, sv);
            --v;
            break;
         case 'C':
            char[] cv = new char[size];

            for (i = 0; i < size; i++) {
               cv[i] = (char) readInt(items[readUnsignedShort(v)]);
               v += 3;
            }

            av.visit(name, cv);
            --v;
            break;
         case 'I':
            int[] iv = new int[size];

            for (i = 0; i < size; i++) {
               iv[i] = readInt(items[readUnsignedShort(v)]);
               v += 3;
            }

            av.visit(name, iv);
            --v;
            break;
         case 'J':
            long[] lv = new long[size];

            for (i = 0; i < size; i++) {
               lv[i] = readLong(items[readUnsignedShort(v)]);
               v += 3;
            }

            av.visit(name, lv);
            --v;
            break;
         case 'F':
            float[] fv = new float[size];

            for (i = 0; i < size; i++) {
               fv[i] = Float.intBitsToFloat(readInt(items[readUnsignedShort(v)]));
               v += 3;
            }

            av.visit(name, fv);
            --v;
            break;
         case 'D':
            double[] dv = new double[size];

            for (i = 0; i < size; i++) {
               dv[i] = Double.longBitsToDouble(readLong(items[readUnsignedShort(v)]));
               v += 3;
            }

            av.visit(name, dv);
            --v;
            break;
         default:
            v = readAnnotationValues(v - 3, buf, false, av.visitArray(name));
      }

      return v;
   }
}
