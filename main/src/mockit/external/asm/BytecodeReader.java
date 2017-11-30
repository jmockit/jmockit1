package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

class BytecodeReader
{
   /**
    * The class to be parsed. <em>The content of this array must not be modified.</em>
    */
   @Nonnull final byte[] code;

   /**
    * The start index of each constant pool item in {@link #code}, plus one.
    * The one byte offset skips the constant pool item tag that indicates its type.
    */
   @Nonnull final int[] items;

   /**
    * The String objects corresponding to the CONSTANT_Utf8 items. This cache avoids multiple parsing of a given
    * CONSTANT_Utf8 constant pool item, which GREATLY improves performances (by a factor 2 to 3). This caching
    * strategy could be extended to all constant pool items, but its benefit would not be so great for these items
    * (because they are much less expensive to parse than CONSTANT_Utf8 items).
    */
   @Nonnull final String[] strings;

   /**
    * Maximum length of the strings contained in the constant pool of the class.
    */
   @Nonnegative final int maxStringLength;

   /**
    * The buffer used to read strings.
    */
   @Nonnull final char[] buf;

   /**
    * Start index of the class header information (access, name...) in {@link #code}.
    */
   @Nonnegative final int header;

   BytecodeReader(@Nonnull byte[] code) {
      this.code = code;

      // Parses the constant pool.
      int n = readUnsignedShort(8);
      items = new int[n];
      strings = new String[n];

      int maxSize = 0;
      int index = 10;

      for (int i = 1; i < n; i++) {
         items[i] = index + 1;
         int size;

         switch (code[index]) {
            case FIELD:
            case METH:
            case IMETH:
            case INT:
            case FLOAT:
            case NAME_TYPE:
            case INDY:
               size = 5;
               break;
            case LONG:
            case DOUBLE:
               size = 9;
               i++;
               break;
            case UTF8:
               size = 3 + readUnsignedShort(index + 1);

               if (size > maxSize) {
                  maxSize = size;
               }

               break;
            case HANDLE:
               size = 4;
               break;
            // case CLASS|STR|MTYPE
            default:
               size = 3;
               break;
         }

         index += size;
      }

      maxStringLength = maxSize;
      buf = new char[maxSize];
      header = index; // the class header information starts just after the constant pool
   }

   BytecodeReader(@Nonnull BytecodeReader another) {
      code = another.code;
      items = another.items;
      strings = another.strings;
      maxStringLength = another.maxStringLength;
      buf = another.buf;
      header = another.header;
   }

   /**
    * Reads a byte value in {@link #code}.
    *
    * @param index the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final int readByte(@Nonnegative int index) { return code[index] & 0xFF; }

   /**
    * Reads an unsigned short value in {@link #code}.
    *
    * @param index the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   @Nonnegative
   final int readUnsignedShort(@Nonnegative int index) {
      byte[] b = code;
      return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
   }

   /**
    * Reads a signed short value in {@link #code}.
    *
    * @param index the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final short readShort(@Nonnegative int index) {
      return (short) readUnsignedShort(index);
   }

   /**
    * Reads a signed int value in {@link #code}.
    *
    * @param index the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final int readInt(@Nonnegative int index) {
      byte[] b = code;
      return
         ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) |
         ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
   }

   /**
    * Reads a signed long value in {@link #code}.
    *
    * @param index the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final long readLong(@Nonnegative int index) {
      long l1 = readInt(index);
      long l0 = readInt(index + 4) & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
   }

   /**
    * Reads UTF8 string in {@link #code}.
    *
    * @param index  start offset of the UTF8 string to be read.
    * @param utfLen length of the UTF8 string to be read.
    * @return the String corresponding to the specified UTF8 string.
    */
   @Nonnull
   final String readUTF(@Nonnegative int index, @Nonnegative int utfLen) {
      int endIndex = index + utfLen;
      //noinspection UnnecessaryLocalVariable
      byte[] b = code;
      int strLen = 0;
      int st = 0;
      char cc = 0;
      int c;

      while (index < endIndex) {
         c = b[index++];

         switch (st) {
            case 0:
               c = c & 0xFF;

               if (c < 0x80) { // 0xxxxxxx
                  buf[strLen++] = (char) c;
               }
               else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                  cc = (char) (c & 0x1F);
                  st = 1;
               }
               else { // 1110 xxxx 10xx xxxx 10xx xxxx
                  cc = (char) (c & 0x0F);
                  st = 2;
               }

               break;
            case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
               buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
               st = 0;
               break;
            case 2: // byte 2 of 3-byte char
               cc = (char) ((cc << 6) | (c & 0x3F));
               st = 1;
               break;
         }
      }

      return new String(buf, 0, strLen);
   }

   /**
    * Reads an UTF8 string constant pool item in {@link #code}.
    *
    * @param index the start index of an unsigned short value in {@link #code}, whose value is the index of an UTF8
    *              constant pool item.
    * @return the String corresponding to the specified UTF8 item, or <tt>null</tt> if index is zero or points to an
    * item whose value is zero.
    */
   @Nullable
   final String readUTF8(@Nonnegative int index) {
      if (index == 0) {
         return null;
      }

      int item = readUnsignedShort(index);

      if (item == 0) {
         return null;
      }

      String s = strings[item];

      if (s != null) {
         return s;
      }

      int startIndex = items[item];
      int utfLen = readUnsignedShort(startIndex);
      String utf = readUTF(startIndex + 2, utfLen);
      strings[item] = utf;
      return utf;
   }

   /**
    * Reads a numeric or string constant pool item in {@link #code}.
    *
    * @param itemIndex the index of a constant pool item.
    * @return the {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link JavaType} or
    * {@link Handle} corresponding to the given constant pool item.
    */
   @Nonnull
   final Object readConst(@Nonnegative int itemIndex) {
      int startIndex = items[itemIndex];
      byte itemType = code[startIndex - 1];

      switch (itemType) {
         case INT:
            return readInt(startIndex);
         case FLOAT:
            int bits = readInt(startIndex);
            return Float.intBitsToFloat(bits);
         case LONG:
            return readLong(startIndex);
         case DOUBLE:
            long longBits = readLong(startIndex);
            return Double.longBitsToDouble(longBits);
         case CLASS:
            String typeDesc = readUTF8(startIndex);
            //noinspection ConstantConditions
            return JavaType.getObjectType(typeDesc);
         case STR:
            String string = readUTF8(startIndex);
            //noinspection ConstantConditions
            return string;
         case MTYPE:
            String methodDesc = readUTF8(startIndex);
            //noinspection ConstantConditions
            return JavaType.getMethodType(methodDesc);
         default: // case HANDLE_BASE + [1..9]:
            return readHandle(startIndex);
      }
   }

   @Nonnull
   private Object readHandle(@Nonnegative int startIndex) {
      int tag = readByte(startIndex);

      int i = readUnsignedShort(startIndex + 1);
      int classIndex = items[i];
      String owner = readClass(classIndex);

      i = readUnsignedShort(classIndex + 2);
      int nameIndex = items[i];
      String name = readUTF8(nameIndex);
      String desc = readUTF8(nameIndex + 2);

      //noinspection ConstantConditions
      return new Handle(tag, owner, name, desc);
   }

   /**
    * Reads a class constant pool item in {@link #code}.
    *
    * @param index the start index of an unsigned short value in {@link #code}, whose value is the index of a class
    *              constant pool item.
    * @return the String corresponding to the specified class item.
    */
   @Nullable
   final String readClass(@Nonnegative int index) {
      // Computes the start index of the CONSTANT_Class item in code and reads the CONSTANT_Utf8 item designated by the
      // first two bytes of this CONSTANT_Class item.
      int itemIndex = readUnsignedShort(index);
      String classDesc = readUTF8(items[itemIndex]);
      return classDesc;
   }
}
