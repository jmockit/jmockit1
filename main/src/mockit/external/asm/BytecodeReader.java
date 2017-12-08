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
   @Nonnull private final String[] strings;

   /**
    * The buffer used to read strings.
    */
   @Nonnull private final char[] buf;

   /**
    * Start index of the class header information (access, name...) in {@link #code}.
    */
   @Nonnegative final int header;

   @Nonnegative int currentCodeIndex;

   BytecodeReader(@Nonnull byte[] code) {
      this.code = code;

      // Parses the constant pool.
      int itemCount = readUnsignedShort(8);
      items = new int[itemCount];
      strings = new String[itemCount];

      int maxStringSize = 0;
      int codeIndex = 10;

      for (int itemIndex = 1; itemIndex < itemCount; itemIndex++) {
         items[itemIndex] = codeIndex + 1;

         byte itemType = code[codeIndex];
         int itemSize = computeItemSize(codeIndex, itemType);

         if (itemType == LONG || itemType == DOUBLE) {
            itemIndex++;
         }
         else if (itemType == UTF8 && itemSize > maxStringSize) {
            maxStringSize = itemSize;
         }

         codeIndex += itemSize;
      }

      buf = new char[maxStringSize];
      header = codeIndex; // the class header information starts just after the constant pool
   }

   private int computeItemSize(@Nonnegative int codeIndex, byte itemType) {
      switch (itemType) {
         case FIELD: case METH: case IMETH: case INT: case FLOAT: case NAME_TYPE: case INDY: return 5;
         case LONG: case DOUBLE: return 9;
         case UTF8: return 3 + readUnsignedShort(codeIndex + 1);
         case HANDLE: return 4;
         default: return 3; // CLASS|STR|MTYPE
      }
   }

   BytecodeReader(@Nonnull BytecodeReader another) {
      code = another.code;
      items = another.items;
      strings = another.strings;
      buf = another.buf;
      header = 0;
   }

   /**
    * Reads a byte value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final int readByte(@Nonnegative int codeIndex) {
      return code[codeIndex] & 0xFF;
   }

   final char readChar(@Nonnegative int codeIndex) {
      return (char) readInt(codeIndex);
   }

   final boolean readBoolean(@Nonnegative int codeIndex) {
      return readInt(codeIndex) != 0;
   }

   /**
    * Reads an unsigned short value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   @Nonnegative
   final int readUnsignedShort(@Nonnegative int codeIndex) {
      byte[] b = code;
      return ((b[codeIndex] & 0xFF) << 8) | (b[codeIndex + 1] & 0xFF);
   }

   /**
    * Reads a signed short value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final short readShort(@Nonnegative int codeIndex) {
      return (short) readUnsignedShort(codeIndex);
   }

   /**
    * Reads a signed int value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final int readInt(@Nonnegative int codeIndex) {
      byte[] b = code;
      return
         ((b[codeIndex] & 0xFF) << 24) | ((b[codeIndex + 1] & 0xFF) << 16) |
         ((b[codeIndex + 2] & 0xFF) << 8) | (b[codeIndex + 3] & 0xFF);
   }

   /**
    * Reads a signed long value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final long readLong(@Nonnegative int codeIndex) {
      long l1 = readInt(codeIndex);
      long l0 = readInt(codeIndex + 4) & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
   }

   final double readDouble(@Nonnegative int codeIndex) {
      long bits = readLong(codeIndex);
      return Double.longBitsToDouble(bits);
   }

   final float readFloat(@Nonnegative int codeIndex) {
      int bits = readInt(codeIndex);
      return Float.intBitsToFloat(bits);
   }

   /**
    * Reads UTF8 string in {@link #code}.
    *
    * @param itemIndex index in {@link #items} for the UTF8 string to be read.
    * @return the String corresponding to the specified UTF8 string.
    */
   @Nonnull
   private String readUTF(@Nonnegative int itemIndex) {
      int startIndex = items[itemIndex];
      int utfLen = readUnsignedShort(startIndex);
      startIndex += 2;
      int endIndex = startIndex + utfLen;
      int strLen = 0;
      int st = 0;
      char cc = 0;

      while (startIndex < endIndex) {
         int c = code[startIndex++];

         if (st == 0) {
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
         }
         else if (st == 1) { // byte 2 of 2-byte char or byte 3 of 3-byte char
            buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
            st = 0;
         }
         else { // byte 2 of 3-byte char
            cc = (char) ((cc << 6) | (c & 0x3F));
            st = 1;
         }
      }

      return new String(buf, 0, strLen);
   }

   /**
    * Reads an UTF8 string constant pool item in {@link #code}.
    *
    * @param codeIndex the start index of an unsigned short value in {@link #code}, whose value is the index of an UTF8
    *                  constant pool item.
    * @return the String corresponding to the specified UTF8 item, or <tt>null</tt> if index is zero or points to an
    * item whose value is zero.
    */
   @Nullable
   final String readUTF8(@Nonnegative int codeIndex) {
      if (codeIndex == 0) {
         return null;
      }

      int itemIndex = readUnsignedShort(codeIndex);

      if (itemIndex == 0) {
         return null;
      }

      return readString(itemIndex);
   }

   @Nonnull
   final String readString(@Nonnegative int itemIndex) {
      String string = strings[itemIndex];

      if (string != null) {
         return string;
      }

      string = readUTF(itemIndex);
      strings[itemIndex] = string;
      return string;
   }

   @Nonnull
   final Object readConstItem(@Nonnegative int codeIndex) {
      int itemIndex = readUnsignedShort(codeIndex);
      return readConst(itemIndex);
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
      int codeIndex = items[itemIndex];
      byte itemType = code[codeIndex - 1];

      switch (itemType) {
         case INT:    return readInt(codeIndex);
         case FLOAT:  return readFloat(codeIndex);
         case LONG:   return readLong(codeIndex);
         case DOUBLE: return readDouble(codeIndex);
         case CLASS:
            String typeDesc = readUTF8(codeIndex);
            //noinspection ConstantConditions
            return ReferenceType.createFromInternalName(typeDesc);
         case STR:
            String string = readUTF8(codeIndex);
            //noinspection ConstantConditions
            return string;
         case MTYPE:
            String methodDesc = readUTF8(codeIndex);
            //noinspection ConstantConditions
            return MethodType.create(methodDesc);
         default: // case HANDLE_BASE + [1..9]:
            return readHandle(codeIndex);
      }
   }

   @Nonnull
   private Object readHandle(@Nonnegative int codeIndex) {
      int tag = readByte(codeIndex);

      int itemIndex = readUnsignedShort(codeIndex + 1);
      int classIndex = items[itemIndex];
      String owner = readClass(classIndex);

      itemIndex = readUnsignedShort(classIndex + 2);
      int nameIndex = items[itemIndex];
      String name = readUTF8(nameIndex);
      String desc = readUTF8(nameIndex + 2);

      //noinspection ConstantConditions
      return new Handle(tag, owner, name, desc);
   }

   /**
    * Reads a class constant pool item in {@link #code}.
    *
    * @param codeIndex the start index of an unsigned short value in {@link #code}, whose value is the index of a class
    *                  constant pool item.
    * @return the String corresponding to the specified class item.
    */
   @Nullable
   final String readClass(@Nonnegative int codeIndex) {
      // Computes the start index of the CONSTANT_Class item in code and reads the CONSTANT_Utf8 item designated by the
      // first two bytes of this CONSTANT_Class item.
      int itemIndex = readUnsignedShort(codeIndex);
      String classDesc = readUTF8(items[itemIndex]);
      return classDesc;
   }
}
