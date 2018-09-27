package mockit.asm;

import javax.annotation.*;

/**
 * A dynamically extensible vector of bytes.
 * This class is roughly equivalent to a DataOutputStream on top of a ByteArrayOutputStream, but is more efficient.
 */
@SuppressWarnings({"NumericCastThatLosesPrecision", "CharUsedInArithmeticContext"})
public final class ByteVector
{
   /**
    * The content of this vector.
    */
   @Nonnull byte[] data;

   /**
    * Actual number of bytes in this vector.
    */
   @Nonnegative int length;

   /**
    * Constructs a new ByteVector with a default initial size.
    */
   public ByteVector() {
      data = new byte[64];
   }

   /**
    * Constructs a new ByteVector with the given initial size.
    */
   ByteVector(@Nonnegative int initialSize) {
      data = new byte[initialSize];
   }

   /**
    * Returns the {@link #length} of this vector, in bytes.
    */
   @Nonnegative
   public int getLength() { return length; }

   /**
    * Puts a byte into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector
    */
   @Nonnull
   public ByteVector putByte(int b) {
      int len = getLengthEnlargingIfNeeded(1);
      data[len++] = (byte) b;
      length = len;
      return this;
   }

   @Nonnegative
   private int getLengthEnlargingIfNeeded(@Nonnegative int bytesToAdd) {
      int len = length;

      if (len + bytesToAdd > data.length) {
         enlarge(bytesToAdd);
      }

      return len;
   }

   /**
    * Enlarge this byte vector so that it can receive n more bytes.
    *
    * @param size number of additional bytes that this byte vector should be able to receive
    */
   private void enlarge(@Nonnegative int size) {
      int length1 = 2 * data.length;
      int length2 = length + size;
      byte[] newData = new byte[length1 > length2 ? length1 : length2];
      System.arraycopy(data, 0, newData, 0, length);
      data = newData;
   }

   /**
    * Puts two bytes into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector
    */
   @Nonnull
   public ByteVector put11(int b1, int b2) {
      int len = getLengthEnlargingIfNeeded(2);
      byte[] bytes = data;
      bytes[len++] = (byte) b1;
      bytes[len++] = (byte) b2;
      length = len;
      return this;
   }

   /**
    * Puts a short into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector
    */
   @Nonnull
   public ByteVector putShort(int s) {
      return put11(s >>> 8, s);
   }

   /**
    * Puts a byte and a short into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector
    */
   @Nonnull
   public ByteVector put12(int b, int s) {
      int len = getLengthEnlargingIfNeeded(3);
      byte[] bytes = data;
      bytes[len++] = (byte) b;
      bytes[len++] = (byte) (s >>> 8);
      bytes[len++] = (byte) s;
      length = len;
      return this;
   }

   /**
    * Puts an int into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector
    */
   @Nonnull
   public ByteVector putInt(int i) {
      int len = getLengthEnlargingIfNeeded(4);
      byte[] bytes = data;
      bytes[len++] = (byte) (i >>> 24);
      bytes[len++] = (byte) (i >>> 16);
      bytes[len++] = (byte) (i >>> 8);
      bytes[len++] = (byte) i;
      length = len;
      return this;
   }

   /**
    * Puts a long into this byte vector. The byte vector is automatically enlarged if necessary.
    */
   public void putLong(long l) {
      int i1 = (int) (l >>> 32);
      int i2 = (int) l;
      putInt(i1);
      putInt(i2);
   }

   /**
    * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @param utf8String a String whose UTF8 encoded length must be less than 65536
    */
   public void putUTF8(@Nonnull String utf8String) {
      int charLength = utf8String.length();

      if (charLength > 65535) {
         throw new IllegalArgumentException("String too long: " + charLength);
      }

      int len = getLengthEnlargingIfNeeded(2 + charLength);
      byte[] characters = data;

      // Optimistic algorithm: instead of computing the byte length and then serializing the string (which requires
      // two loops), we assume the byte length is equal to char length (which is the most frequent case), and we start
      // serializing the string right away.
      // During the serialization, if we find that this assumption is wrong, we continue with the general method.
      characters[len++] = (byte) (charLength >>> 8);
      characters[len++] = (byte) charLength;

      for (int i = 0; i < charLength; i++) {
         char c = utf8String.charAt(i);

         if (c >= '\001' && c <= '\177') {
            characters[len++] = (byte) c;
         }
         else {
            length = len;
            encodeUTF8(utf8String, i);
         }
      }

      length = len;
   }

   /**
    * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if necessary. The string length is encoded in two
    * bytes before the encoded characters, if there is space for that (i.e. if this.length - i - 2 >= 0).
    *
    * @param utf8String the String to encode
    * @param startIndex the index of the first character to encode. The previous characters are supposed to have already
    *                   been encoded, using only one byte per character.
    */
   private void encodeUTF8(@Nonnull String utf8String, @Nonnegative int startIndex) {
      int byteLength = computeByteLength(utf8String, startIndex);

      if (byteLength > 65535) {
         throw new IllegalArgumentException("String too long for UTF8 encoding: " + byteLength);
      }

      int start = length - startIndex - 2;

      if (start >= 0) {
         data[start] = (byte) (byteLength >>> 8);
         data[start + 1] = (byte) byteLength;
      }

      if (length + byteLength - startIndex > data.length) {
         enlarge(byteLength - startIndex);
      }

      putEncodedCharacters(utf8String, startIndex);
   }

   @Nonnegative
   private static int computeByteLength(@Nonnull String utf8String, @Nonnegative int startIndex) {
      int byteLength = startIndex;

      for (int i = startIndex, n = utf8String.length(); i < n; i++) {
         char c = utf8String.charAt(i);

         if (c >= '\001' && c <= '\177') {
            byteLength++;
         }
         else if (c > '\u07FF') {
            byteLength += 3;
         }
         else {
            byteLength += 2;
         }
      }

      return byteLength;
   }

   private void putEncodedCharacters(@Nonnull String utf8String, @Nonnegative int startIndex) {
      byte[] characters = data;
      int len = length;

      for (int i = startIndex, n = utf8String.length(); i < n; i++) {
         char c = utf8String.charAt(i);

         if (c >= '\001' && c <= '\177') {
            characters[len++] = (byte) c;
         }
         else {
            if (c > '\u07FF') {
               characters[len++] = (byte) (0xE0 | c >> 12 & 0xF);
               characters[len++] = (byte) (0x80 | c >> 6 & 0x3F);
            }
            else {
               characters[len++] = (byte) (0xC0 | c >> 6 & 0x1F);
            }

            characters[len++] = (byte) (0x80 | c & 0x3F);
         }
      }

      length = len;
   }

   /**
    * Puts an array of bytes into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @param bytes an array of bytes
    * @param offset index of the first byte of code that must be copied
    * @param numBytes number of bytes of code that must be copied
    */
   public void putByteArray(@Nonnull byte[] bytes, @Nonnegative int offset, @Nonnegative int numBytes) {
      int len = getLengthEnlargingIfNeeded(numBytes);
      System.arraycopy(bytes, offset, data, len, numBytes);
      length += numBytes;
   }

   public void putByteVector(@Nonnull ByteVector another) {
      putByteArray(another.data, 0, another.length);
   }

   void increaseLengthBy(@Nonnegative int len) {
      getLengthEnlargingIfNeeded(len);
      length += len;
   }
}
