package mockit.asm.util;

import javax.annotation.*;

import mockit.asm.types.*;
import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public class BytecodeReader
{
   /**
    * The class to be parsed. <em>The content of this array must not be modified.</em>
    */
   @Nonnull public final byte[] code;

   /**
    * The start index of each constant pool item in {@link #code}, plus one.
    * The one byte offset skips the constant pool item tag that indicates its type.
    */
   @Nonnull public final int[] items;

   /**
    * The String objects corresponding to the CONSTANT_Utf8 items. This cache avoids multiple parsing of a given CONSTANT_Utf8 constant pool
    * item, which GREATLY improves performances (by a factor 2 to 3). This caching strategy could be extended to all constant pool items,
    * but its benefit would not be so great for these items (because they are much less expensive to parse than CONSTANT_Utf8 items).
    */
   @Nonnull private final String[] strings;

   /**
    * The buffer used to read strings.
    */
   @Nonnull private final char[] buf;

   /**
    * The next index at {@link #code} to be read.
    */
   @Nonnegative public int codeIndex;

   protected BytecodeReader(@Nonnull byte[] code) {
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
         case FIELD: case METH: case IMETH: case INT: case FLOAT: case NAME_TYPE: case CONDY: case INDY: return 5;
         case LONG: case DOUBLE: return 9;
         case UTF8: return 3 + readUnsignedShort(codeIndex);
         case HANDLE: return 4;
         default: return 3; // CLASS|STR|MTYPE
      }
   }

   protected BytecodeReader(@Nonnull BytecodeReader another) {
      code = another.code;
      items = another.items;
      strings = another.strings;
      buf = another.buf;
      codeIndex = another.codeIndex;
   }

   /**
    * Reads an unsigned <tt>byte</tt> value in {@link #code}, incrementing {@link #codeIndex} by 1.
    */
   public final int readUnsignedByte() {
      return code[codeIndex++] & 0xFF;
   }

   /**
    * Reads an unsigned byte value in {@link #code}.
    *
    * @param u1CodeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   protected final int readUnsignedByte(@Nonnegative int u1CodeIndex) {
      return code[u1CodeIndex] & 0xFF;
   }

   /**
    * Reads a signed <tt>byte</tt> value in {@link #code}, incrementing {@link #codeIndex} by 1.
    */
   public final int readSignedByte() {
      return code[codeIndex++];
   }

   protected final char readChar(@Nonnegative int s4CodeIndex) {
      return (char) readInt(s4CodeIndex);
   }

   protected final boolean readBoolean(@Nonnegative int s4CodeIndex) {
      return readInt(s4CodeIndex) != 0;
   }

   /**
    * Reads an unsigned short value in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   @Nonnegative
   public final int readUnsignedShort() {
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
    * @param u2CodeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   @Nonnegative
   protected final int readUnsignedShort(@Nonnegative int u2CodeIndex) {
      byte[] b = code;
      return ((b[u2CodeIndex] & 0xFF) << 8) | (b[u2CodeIndex + 1] & 0xFF);
   }

   /**
    * Reads a signed <tt>short</tt> value in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   protected final short readShort() {
      //noinspection NumericCastThatLosesPrecision
      return (short) readUnsignedShort();
   }

   /**
    * Reads a signed short value in {@link #code}.
    *
    * @param u2CodeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   protected final short readShort(@Nonnegative int u2CodeIndex) {
      //noinspection NumericCastThatLosesPrecision
      return (short) readUnsignedShort(u2CodeIndex);
   }

   /**
    * Reads a signed <tt>int</tt> value in {@link #code}, incrementing {@link #codeIndex} by 4.
    */
   public final int readInt() {
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
    * @param s4CodeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   protected final int readInt(@Nonnegative int s4CodeIndex) {
      byte[] b = code;
      return
         ((b[s4CodeIndex] & 0xFF) << 24) | ((b[s4CodeIndex + 1] & 0xFF) << 16) |
         ((b[s4CodeIndex + 2] & 0xFF) << 8) | (b[s4CodeIndex + 3] & 0xFF);
   }

   /**
    * Reads a signed long value in {@link #code}, incrementing {@link #codeIndex} by 8.
    */
   public final long readLong() {
      long l1 = readInt();
      long l0 = readInt() & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
   }

   /**
    * Reads a signed long value in {@link #code}.
    *
    * @param s8CodeIndex the start index of the value to be read in {@link #code}.
    * @return the read value.
    */
   protected final long readLong(@Nonnegative int s8CodeIndex) {
      long l1 = readInt(s8CodeIndex);
      long l0 = readInt(s8CodeIndex + 4) & 0xFFFFFFFFL;
      return (l1 << 32) | l0;
   }

   public final double readDouble() {
      long bits = readLong();
      return Double.longBitsToDouble(bits);
   }

   protected final double readDouble(@Nonnegative int s8CodeIndex) {
      long bits = readLong(s8CodeIndex);
      return Double.longBitsToDouble(bits);
   }

   public final float readFloat() {
      int bits = readInt();
      return Float.intBitsToFloat(bits);
   }

   protected final float readFloat(@Nonnegative int s4CodeIndex) {
      int bits = readInt(s4CodeIndex);
      return Float.intBitsToFloat(bits);
   }

   /**
    * Reads an UTF8 string in {@link #code}.
    *
    * @param itemIndex index in {@link #items} for the UTF8 string to be read.
    * @return the String corresponding to the specified UTF8 string.
    */
   @Nonnull @SuppressWarnings("CharUsedInArithmeticContext")
   private String readUTF(@Nonnegative int itemIndex) {
      int startIndex = items[itemIndex];
      int utfLen = readUnsignedShort(startIndex);
      startIndex += 2;
      int endIndex = startIndex + utfLen;
      int strLen = 0;
      int st = 0;
      @SuppressWarnings("QuestionableName") char cc = 0;

      while (startIndex < endIndex) {
         int c = code[startIndex++];

         if (st == 0) {
            c &= 0xFF;

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
    * @return the String corresponding to the UTF8 item, or <tt>null</tt> if {@link #codeIndex} points to an item whose value is zero.
    */
   @Nullable
   protected final String readUTF8() {
      int itemIndex = readUnsignedShort();

      if (itemIndex == 0) {
         return null;
      }

      return readString(itemIndex);
   }

   /**
    * Reads an UTF8 string constant pool item in {@link #code}.
    *
    * @param u2CodeIndex the index of an unsigned short value in {@link #code}, whose value is the index of an UTF8 constant pool item.
    * @return the String corresponding to the UTF8 item, or <tt>null</tt> if index is zero or points to an item whose value is zero.
    */
   @Nullable
   protected final String readUTF8(@Nonnegative int u2CodeIndex) {
      if (u2CodeIndex == 0) {
         return null;
      }

      int itemIndex = readUnsignedShort(u2CodeIndex);

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
   public final String readNonnullUTF8() {
      int itemIndex = readUnsignedShort();
      return readString(itemIndex);
   }

   /**
    * Reads the index of an UTF8 item in {@link #code}.
    *
    * @return the UTF8 string found in {@link #strings} at that index
    */
   @Nonnull
   public final String readNonnullUTF8(@Nonnegative int u2CodeIndex) {
      int itemIndex = readUnsignedShort(u2CodeIndex);
      return readString(itemIndex);
   }

   /**
    * Reads a string in {@link #strings} at the given index.
    */
   @Nonnull
   public final String readString(@Nonnegative int itemIndex) {
      String cachedString = strings[itemIndex];

      if (cachedString != null) {
         return cachedString;
      }

      String newString = readUTF(itemIndex);
      strings[itemIndex] = newString;
      return newString;
   }

   /**
    * Reads the index of a constant item in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the UTF8 string found in {@link #strings} at that index
    */
   @Nonnull
   public final Object readConstItem() {
      int constIndex = readUnsignedShort();
      Object cst = readConst(constIndex);
      return cst;
   }

   @Nonnull
   protected final Object readConstItem(@Nonnegative int u2CodeIndex) {
      int itemIndex = readUnsignedShort(u2CodeIndex);
      return readConst(itemIndex);
   }

   /**
    * Reads a numeric or string constant pool item in {@link #code}.
    *
    * @param itemIndex the index of a constant pool item.
    * @return the {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link JavaType} or {@link MethodHandle}
    * corresponding to the given constant pool item.
    */
   @Nonnull
   protected final Object readConst(@Nonnegative int itemIndex) {
      int constCodeIndex = items[itemIndex];
      byte itemType = code[constCodeIndex - 1];

      switch (itemType) {
         case INT:    return readInt(constCodeIndex);
         case FLOAT:  return readFloat(constCodeIndex);
         case LONG:   return readLong(constCodeIndex);
         case DOUBLE: return readDouble(constCodeIndex);
         case STR:    return readNonnullUTF8(constCodeIndex);
         case CLASS:
            String typeDesc = readNonnullUTF8(constCodeIndex);
            return ReferenceType.createFromInternalName(typeDesc);
         case MTYPE:
            String methodDesc = readNonnullUTF8(constCodeIndex);
            return MethodType.create(methodDesc);
      // case HANDLE_BASE + [1..9]:
         default:
            return readMethodHandle(constCodeIndex);
      }
   }

   @Nonnull
   public final MethodHandle readMethodHandle() {
      int itemIndex = readUnsignedShort();
      return readMethodHandle(items[itemIndex]);
   }

   @Nonnull
   protected final MethodHandle readMethodHandleItem(@Nonnegative int bsmCodeIndex) {
      int itemIndex = readUnsignedShort(bsmCodeIndex);
      return readMethodHandle(items[itemIndex]);
   }

   @Nonnull
   private MethodHandle readMethodHandle(@Nonnegative int bsmCodeIndex) {
      int tag = readUnsignedByte(bsmCodeIndex);

      int classIndex = readItem(bsmCodeIndex + 1);
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
   protected final String readClass() {
      int itemCodeIndex = readItem();
      String classDesc = readUTF8(itemCodeIndex);
      return classDesc;
   }

   /**
    * Reads a class descriptor in {@link #code}, incrementing {@link #codeIndex} by 2.
    */
   @Nonnull
   public final String readNonnullClass() {
      int itemCodeIndex = readItem();
      String classDesc = readNonnullUTF8(itemCodeIndex);
      return classDesc;
   }

   @Nonnull
   public final String readNonnullClass(@Nonnegative int u2CodeIndex) {
      int itemCodeIndex = readItem(u2CodeIndex);
      String classDesc = readNonnullUTF8(itemCodeIndex);
      return classDesc;
   }

   /**
    * Reads an item index in {@link #code}, incrementing {@link #codeIndex} by 2.
    *
    * @return the item at that index in {@link #items}
    */
   @Nonnegative
   public final int readItem() {
      int itemIndex = readUnsignedShort();
      return items[itemIndex];
   }

   @Nonnegative
   public final int readItem(@Nonnegative int u2CodeIndex) {
      int itemIndex = readUnsignedShort(u2CodeIndex);
      return items[itemIndex];
   }
}
