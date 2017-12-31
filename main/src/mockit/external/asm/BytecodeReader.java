package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

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
    * The next index at {@link #code} to be read.
    */
   @Nonnegative int codeIndex;

   BytecodeReader(@Nonnull byte[] code) {
      this.code = code;
      codeIndex = 8;

      int itemCount = readUnsignedShort();
      items = new int[itemCount];
      strings = new String[itemCount];

      int maxStringSize = readConstantPoolItems();
      buf = new char[maxStringSize];
   }

   @Nonnegative
   private int readConstantPoolItems() {
      int maxStringSize = 0;

      for (int itemIndex = 1; itemIndex < items.length; itemIndex++) {
         int itemType = readSignedByte();
         items[itemIndex] = codeIndex;
         int itemSize = getItemSize(itemType);

         if (itemType == LONG || itemType == DOUBLE) {
            itemIndex++;
         }
         else if (itemType == UTF8 && itemSize > maxStringSize) {
            maxStringSize = itemSize;
         }

         codeIndex += itemSize - 1;
      }

      return maxStringSize;
   }

   @Nonnegative
   private int getItemSize(int itemType) {
      switch (itemType) {
         case FIELD: case METH: case IMETH: case INT: case FLOAT: case NAME_TYPE: case INDY: return 5;
         case LONG: case DOUBLE: return 9;
         case UTF8: return 3 + readUnsignedShort(codeIndex);
         case HANDLE: return 4;
         default: return 3; // CLASS|STR|MTYPE
      }
   }

   BytecodeReader(@Nonnull BytecodeReader another) {
      code = another.code;
      items = another.items;
      strings = another.strings;
      buf = another.buf;
      codeIndex = another.codeIndex;
   }

   /**
    * Reads an unsigned <tt>byte</tt> value in {@link #code}, incrementing {@link #codeIndex} by 1.
    */
   final int readUnsignedByte() {
      return code[codeIndex++] & 0xFF;
   }

   /**
    * Reads an unsigned byte value in {@link #code}.
    *
    * @param codeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   final int readUnsignedByte(@Nonnegative int codeIndex) {
      return code[codeIndex] & 0xFF;
   }

   /**
    * Reads a signed <tt>byte</tt> value in {@link #code}, incrementing {@link #codeIndex} by 1.
    */
   final int readSignedByte() {
      return code[codeIndex++];
   }

   final char readChar(@Nonnegative int codeIndex) {
      return (char) readInt(codeIndex);
   }

   final boolean readBoolean(@Nonnegative int codeIndex) {
      return readInt(codeIndex) != 0;
   }

   /**
    * Reads an unsigned short value in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   @Nonnegative
   final int readUnsignedShort() {
      byte[] b = code;
      int i = codeIndex;
      int byte0 = (b[i++] & 0xFF) << 8;
      int byte1 =  b[i++] & 0xFF;
      codeIndex = i;
      return byte0 | byte1;
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
    * Reads a signed <tt>short</tt> value in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   final short readShort() {
      return (short) readUnsignedShort();
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
    * Reads a signed <tt>int</tt> value in {@link #code}, incrementing {@link #codeIndex} by 4.
    */
   final int readInt() {
      byte[] b = code;
      int i = codeIndex;
      int byte0 = (b[i++] & 0xFF) << 24;
      int byte1 = (b[i++] & 0xFF) << 16;
      int byte2 = (b[i++] & 0xFF) << 8;
      int byte3 =  b[i++] & 0xFF;
      codeIndex = i;
      return byte0 | byte1 | byte2 | byte3;
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
    * Reads a signed long value in {@link #code}, incrementing {@link #codeIndex} by 8.
    */
   final long readLong() {
      long l1 = readInt();
      long l0 = readInt() & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
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

   final double readDouble() {
      long bits = readLong();
      return Double.longBitsToDouble(bits);
   }

   final double readDouble(@Nonnegative int codeIndex) {
      long bits = readLong(codeIndex);
      return Double.longBitsToDouble(bits);
   }

   final float readFloat() {
      int bits = readInt();
      return Float.intBitsToFloat(bits);
   }

   final float readFloat(@Nonnegative int codeIndex) {
      int bits = readInt(codeIndex);
      return Float.intBitsToFloat(bits);
   }

   /**
    * Reads an UTF8 string in {@link #code}.
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
    * Reads an UTF8 string constant pool item in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the String corresponding to the specified UTF8 item, or <tt>null</tt> if {@link #codeIndex} points to an
    * item whose value is zero.
    */
   @Nullable
   final String readUTF8() {
      int itemIndex = readUnsignedShort();

      if (itemIndex == 0) {
         return null;
      }

      return readString(itemIndex);
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

   /**
    * Reads the index of an UTF8 item in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the UTF8 string found in {@link #strings} at that index
    */
   @Nonnull
   final String readNonnullUTF8() {
      int itemIndex = readUnsignedShort();
      return readString(itemIndex);
   }

   /**
    * Reads the index of an UTF8 item in {@link #code}.
    *
    * @return the UTF8 string found in {@link #strings} at that index
    */
   @Nonnull
   final String readNonnullUTF8(@Nonnegative int codeIndex) {
      int itemIndex = readUnsignedShort(codeIndex);
      return readString(itemIndex);
   }

   /**
    * Reads a string in {@link #strings} at the given index.
    */
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

   /**
    * Reads the index of a constant item in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the UTF8 string found in {@link #strings} at that index
    */
   @Nonnull
   final Object readConstItem() {
      int constIndex = readUnsignedShort();
      Object cst = readConst(constIndex);
      return cst;
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
    * {@link MethodHandle} corresponding to the given constant pool item.
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
         case STR:    return readNonnullUTF8(codeIndex);
         case CLASS:
            String typeDesc = readNonnullUTF8(codeIndex);
            return ReferenceType.createFromInternalName(typeDesc);
         case MTYPE:
            String methodDesc = readNonnullUTF8(codeIndex);
            return MethodType.create(methodDesc);
      // case HANDLE_BASE + [1..9]:
         default:
            return readMethodHandle(codeIndex);
      }
   }

   @Nonnull
   final MethodHandle readMethodHandle() {
      int itemIndex = readUnsignedShort();
      int codeIndex = items[itemIndex];
      return readMethodHandle(codeIndex);
   }

   @Nonnull
   final MethodHandle readMethodHandleItem(@Nonnegative int codeIndex) {
      int itemIndex = readUnsignedShort(codeIndex);
      codeIndex = items[itemIndex];
      return readMethodHandle(codeIndex);
   }

   @Nonnull
   private MethodHandle readMethodHandle(@Nonnegative int codeIndex) {
      int tag = readUnsignedByte(codeIndex);

      int classIndex = readItem(codeIndex + 1);
      String owner = readNonnullClass(classIndex);

      int nameIndex = readItem(classIndex + 2);
      String name = readNonnullUTF8(nameIndex);
      String desc = readNonnullUTF8(nameIndex + 2);

      return new MethodHandle(tag, owner, name, desc);
   }

   /**
    * Reads the class name from the constant pool, incrementing {@link #codeIndex} by 2.
    */
   @Nullable
   final String readClass() {
      int itemCodeIndex = readItem();
      String classDesc = readUTF8(itemCodeIndex);
      return classDesc;
   }

   /**
    * Reads a class name constant pool item in {@link #code}.
    *
    * @param codeIndex the start index of an unsigned short value in {@link #code}, whose value is the index of a class
    *                  constant pool item.
    * @return the String corresponding to the specified class item.
    */
   @Nullable
   final String readClass(@Nonnegative int codeIndex) {
      // Computes the start index of the CONSTANT_Class item in code and reads the CONSTANT_Utf8 item designated by the
      // first two bytes of this CONSTANT_Class item.
      int itemCodeIndex = readItem(codeIndex);
      String classDesc = readUTF8(itemCodeIndex);
      return classDesc;
   }

   /**
    * Reads a class descriptor in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   @Nonnull
   final String readNonnullClass() {
      int itemCodeIndex = readItem();
      String classDesc = readNonnullUTF8(itemCodeIndex);
      return classDesc;
   }

   @Nonnull
   final String readNonnullClass(@Nonnegative int codeIndex) {
      int itemCodeIndex = readItem(codeIndex);
      String classDesc = readNonnullUTF8(itemCodeIndex);
      return classDesc;
   }

   /**
    * Reads an item index in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the item at that index in {@link #items}
    */
   @Nonnegative
   final int readItem() {
      int itemIndex = readUnsignedShort();
      return items[itemIndex];
   }

   @Nonnegative
   final int readItem(@Nonnegative int codeIndex) {
      int itemIndex = readUnsignedShort(codeIndex);
      return items[itemIndex];
   }
}
