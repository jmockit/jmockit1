package mockit.external.asm;

import java.io.*;

class BytecodeReader
{
   /**
    * The class to be parsed. <em>The content of this array must not be modified.</em>
    */
   public final byte[] b;

   /**
    * The start index of each constant pool item in {@link #b}, plus one.
    * The one byte offset skips the constant pool item tag that indicates its type.
    */
   final int[] items;

   /**
    * The String objects corresponding to the CONSTANT_Utf8 items. This cache avoids multiple parsing of a given
    * CONSTANT_Utf8 constant pool item, which GREATLY improves performances (by a factor 2 to 3). This caching
    * strategy could be extended to all constant pool items, but its benefit would not be so great for these items
    * (because they are much less expensive to parse than CONSTANT_Utf8 items).
    */
   final String[] strings;

   /**
    * Maximum length of the strings contained in the constant pool of the class.
    */
   final int maxStringLength;

   /**
    * Start index of the class header information (access, name...) in {@link #b}.
    */
   final int header;

   BytecodeReader(byte[] bytecode) {
      b = bytecode;

      // Parses the constant pool.
      int n = readUnsignedShort(8);
      items = new int[n];
      strings = new String[n];
      int maxSize = 0;
      int index = 10;

      for (int i = 1; i < n; ++i) {
         items[i] = index + 1;
         int size;

         switch (bytecode[index]) {
            case ConstantPoolItemType.FIELD:
            case ConstantPoolItemType.METH:
            case ConstantPoolItemType.IMETH:
            case ConstantPoolItemType.INT:
            case ConstantPoolItemType.FLOAT:
            case ConstantPoolItemType.NAME_TYPE:
            case ConstantPoolItemType.INDY:
               size = 5;
               break;
            case ConstantPoolItemType.LONG:
            case ConstantPoolItemType.DOUBLE:
               size = 9;
               ++i;
               break;
            case ConstantPoolItemType.UTF8:
               size = 3 + readUnsignedShort(index + 1);

               if (size > maxSize) {
                  maxSize = size;
               }

               break;
            case ConstantPoolItemType.HANDLE:
               size = 4;
               break;
            // case ConstantPoolItemType.CLASS|STR|MTYPE
            default:
               size = 3;
               break;
         }

         index += size;
      }

      maxStringLength = maxSize;
      header = index; // the class header information starts just after the constant pool
   }

   BytecodeReader(BytecodeReader another) {
      b = another.b;
      items = another.items;
      strings = another.strings;
      maxStringLength = another.maxStringLength;
      header = another.header;
   }

   /**
    * Reads a byte value in {@link #b b}.
    *
    * @param index the start index of the value to be read in {@link #b b}.
    * @return the read value.
    */
   final int readByte(int index) { return b[index] & 0xFF; }

   /**
    * Reads an unsigned short value in {@link #b b}.
    *
    * @param index the start index of the value to be read in {@link #b b}.
    * @return the read value.
    */
   final int readUnsignedShort(int index) {
      byte[] b = this.b;
      return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
   }

   /**
    * Reads a signed short value in {@link #b b}.
    *
    * @param index the start index of the value to be read in {@link #b b}.
    * @return the read value.
    */
   final short readShort(int index) {
      return (short) readUnsignedShort(index);
   }

   /**
    * Reads a signed int value in {@link #b b}.
    *
    * @param index the start index of the value to be read in {@link #b b}.
    * @return the read value.
    */
   final int readInt(int index) {
      byte[] b = this.b;
      return
         ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) |
         ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
   }

   /**
    * Reads a signed long value in {@link #b b}.
    *
    * @param index the start index of the value to be read in {@link #b b}.
    * @return the read value.
    */
   final long readLong(int index) {
      long l1 = readInt(index);
      long l0 = readInt(index + 4) & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
   }

   /**
    * Reads UTF8 string in {@link #b b}.
    *
    * @param index  start offset of the UTF8 string to be read.
    * @param utfLen length of the UTF8 string to be read.
    * @param buf    buffer to be used to read the string. This buffer must be sufficiently large.
    *               It is not automatically resized.
    * @return the String corresponding to the specified UTF8 string.
    */
   final String readUTF(int index, int utfLen, char[] buf) {
      int endIndex = index + utfLen;
      byte[] b = this.b;
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
    * Reads an UTF8 string constant pool item in {@link #b b}.
    *
    * @param index the start index of an unsigned short value in {@link #b b}, whose value is the index of an UTF8
    *              constant pool item.
    * @param buf   buffer to be used to read the item. This buffer must be sufficiently large. It is not automatically
    *              resized.
    * @return the String corresponding to the specified UTF8 item.
    */
   final String readUTF8(int index, char[] buf) {
      int item = readUnsignedShort(index);

      if (index == 0 || item == 0) {
         return null;
      }

      String s = strings[item];

      if (s != null) {
         return s;
      }

      int startIndex = items[item];
      int utfLen = readUnsignedShort(startIndex);
      String utf = readUTF(startIndex + 2, utfLen, buf);
      strings[item] = utf;
      return utf;
   }

   /**
    * Reads a numeric or string constant pool item in {@link #b b}.
    *
    * @param item the index of a constant pool item.
    * @param buf  buffer to be used to read the item. This buffer must be sufficiently large. It is not automatically
    *             resized.
    * @return the {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link Type} or
    * {@link Handle} corresponding to the given constant pool item.
    */
   final Object readConst(int item, char[] buf) {
      int index = items[item];

      switch (b[index - 1]) {
         case ConstantPoolItemType.INT:
            return readInt(index);
         case ConstantPoolItemType.FLOAT:
            return Float.intBitsToFloat(readInt(index));
         case ConstantPoolItemType.LONG:
            return readLong(index);
         case ConstantPoolItemType.DOUBLE:
            return Double.longBitsToDouble(readLong(index));
         case ConstantPoolItemType.CLASS:
            return Type.getObjectType(readUTF8(index, buf));
         case ConstantPoolItemType.STR:
            return readUTF8(index, buf);
         case ConstantPoolItemType.MTYPE:
            return Type.getMethodType(readUTF8(index, buf));
         default: // case ConstantPoolItemType.HANDLE_BASE + [1..9]:
            int tag = readByte(index);
            int[] items = this.items;
            int cpIndex = items[readUnsignedShort(index + 1)];
            String owner = readClass(cpIndex, buf);
            cpIndex = items[readUnsignedShort(cpIndex + 2)];
            String name = readUTF8(cpIndex, buf);
            String desc = readUTF8(cpIndex + 2, buf);
            return new Handle(tag, owner, name, desc);
      }
   }

   /**
    * Reads a class constant pool item in {@link #b}.
    *
    * @param index the start index of an unsigned short value in {@link #b}, whose value is the index of a class
    *              constant pool item.
    * @return the String corresponding to the specified class item.
    */
   final String readClass(int index) {
      return readClass(index, new char[maxStringLength]);
   }

   /**
    * Reads a class constant pool item in {@link #b}.
    *
    * @param index the start index of an unsigned short value in {@link #b}, whose value is the index of a class
    *              constant pool item.
    * @param buf   buffer to be used to read the item. This buffer must be sufficiently large. It is not automatically
    *              resized.
    * @return the String corresponding to the specified class item.
    */
   final String readClass(int index, char[] buf) {
      // Computes the start index of the CONSTANT_Class item in b and reads the CONSTANT_Utf8 item designated by the
      // first two bytes of this CONSTANT_Class item.
      int itemIndex = readUnsignedShort(index);
      int item = items[itemIndex];
      return readUTF8(item, buf);
   }

   /**
    * Reads the bytecode of a class.
    *
    * @param is an input stream from which to read the class.
    * @return the bytecode read from the given input stream.
    * @throws IOException if a problem occurs during reading.
    */
   static byte[] readClass(InputStream is) throws IOException {
      if (is == null) {
         throw new IOException("Class not found");
      }

      try {
         byte[] b = new byte[is.available()];
         int len = 0;

         while (true) {
            int n = is.read(b, len, b.length - len);

            if (n == -1) {
               if (len < b.length) {
                  byte[] c = new byte[len];
                  System.arraycopy(b, 0, c, 0, len);
                  b = c;
               }

               return b;
            }

            len += n;

            if (len == b.length) {
               int last = is.read();

               if (last < 0) {
                  return b;
               }

               byte[] c = new byte[b.length + 1000];
               System.arraycopy(b, 0, c, 0, len);
               c[len++] = (byte) last;
               b = c;
            }
         }
      }
      finally {
         is.close();
      }
   }

   /**
    * Returns the label corresponding to the given offset. The default implementation of this method creates a label
    * for the given offset if it has not been already created.
    *
    * @param offset a bytecode offset in a method.
    * @param labels the already created labels, indexed by their offset. If a label already exists for offset this
    *               method must not create a new one. Otherwise it must store the new label in this array.
    * @return a non null Label, which must be equal to labels[offset].
    */
   static Label readLabel(int offset, Label[] labels) {
      Label label = labels[offset];

      if (label == null) {
         label = new Label();
         labels[offset] = label;
      }

      return label;
   }

   static void readDebugLabel(int index, Label[] labels) {
      Label label = readLabel(index, labels);
      label.markAsDebug();
   }
}
