/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import javax.annotation.*;

/**
 * A dynamically extensible vector of bytes.
 * This class is roughly equivalent to a DataOutputStream on top of a ByteArrayOutputStream, but is more efficient.
 */
final class ByteVector
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
   ByteVector() {
      data = new byte[64];
   }

   /**
    * Constructs a new ByteVector with the given initial size.
    */
   ByteVector(@Nonnegative int initialSize) {
      data = new byte[initialSize];
   }

   /**
    * Puts a byte into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector.
    */
   @Nonnull
   ByteVector putByte(int b) {
      int length = getLengthEnlargingIfNeeded(1);
      data[length++] = (byte) b;
      this.length = length;
      return this;
   }

   @Nonnegative
   private int getLengthEnlargingIfNeeded(@Nonnegative int bytesToAdd) {
      int length = this.length;

      if (length + bytesToAdd > data.length) {
         enlarge(bytesToAdd);
      }

      return length;
   }

   /**
    * Enlarge this byte vector so that it can receive n more bytes.
    *
    * @param size number of additional bytes that this byte vector should be able to receive.
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
    * @return this byte vector.
    */
   @Nonnull
   ByteVector put11(int b1, int b2) {
      int length = getLengthEnlargingIfNeeded(2);
      byte[] data = this.data;
      data[length++] = (byte) b1;
      data[length++] = (byte) b2;
      this.length = length;
      return this;
   }

   /**
    * Puts a short into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector.
    */
   @Nonnull
   ByteVector putShort(int s) {
      return put11(s >>> 8, s);
   }

   /**
    * Puts a byte and a short into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector.
    */
   @Nonnull
   ByteVector put12(int b, int s) {
      int length = getLengthEnlargingIfNeeded(3);
      byte[] data = this.data;
      data[length++] = (byte) b;
      data[length++] = (byte) (s >>> 8);
      data[length++] = (byte) s;
      this.length = length;
      return this;
   }

   /**
    * Puts an int into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @return this byte vector.
    */
   @Nonnull
   ByteVector putInt(int i) {
      int length = getLengthEnlargingIfNeeded(4);
      byte[] data = this.data;
      data[length++] = (byte) (i >>> 24);
      data[length++] = (byte) (i >>> 16);
      data[length++] = (byte) (i >>> 8);
      data[length++] = (byte) i;
      this.length = length;
      return this;
   }

   /**
    * Puts a long into this byte vector. The byte vector is automatically enlarged if necessary.
    */
   void putLong(long l) {
      int i1 = (int) (l >>> 32);
      int i2 = (int) l;
      putInt(i1);
      putInt(i2);
   }

   /**
    * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @param string a String whose UTF8 encoded length must be less than 65536.
    */
   void putUTF8(@Nonnull String string) {
      int charLength = string.length();

      if (charLength > 65535) {
         throw new IllegalArgumentException("String too long: " + charLength);
      }

      int len = getLengthEnlargingIfNeeded(2 + charLength);
      byte[] data = this.data;

      // Optimistic algorithm: instead of computing the byte length and then serializing the string (which requires
      // two loops), we assume the byte length is equal to char length (which is the most frequent case), and we start
      // serializing the string right away.
      // During the serialization, if we find that this assumption is wrong, we continue with the general method.
      data[len++] = (byte) (charLength >>> 8);
      data[len++] = (byte) charLength;

      for (int i = 0; i < charLength; i++) {
         char c = string.charAt(i);

         if (c >= '\001' && c <= '\177') {
            data[len++] = (byte) c;
         }
         else {
            length = len;
            encodeUTF8(string, i);
         }
      }

      length = len;
   }

   /**
    * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if necessary.
    * The string length is encoded in two bytes before the encoded characters, if there is space for that (i.e. if
    * this.length - i - 2 >= 0).
    *
    * @param string the String to encode.
    * @param startIndex the index of the first character to encode. The previous characters are supposed to have already
    *                   been encoded, using only one byte per character.
    */
   private void encodeUTF8(@Nonnull String string, @Nonnegative int startIndex) {
      int byteLength = computeByteLength(string, startIndex);

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

      putEncodedCharacters(string, startIndex);
   }

   @Nonnegative
   private static int computeByteLength(@Nonnull String string, @Nonnegative int startIndex) {
      int byteLength = startIndex;

      for (int i = startIndex, n = string.length(); i < n; i++) {
         char c = string.charAt(i);

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

   private void putEncodedCharacters(@Nonnull String string, @Nonnegative int startIndex) {
      byte[] data = this.data;
      int len = length;

      for (int i = startIndex, n = string.length(); i < n; i++) {
         char c = string.charAt(i);

         if (c >= '\001' && c <= '\177') {
            data[len++] = (byte) c;
         }
         else if (c > '\u07FF') {
            data[len++] = (byte) (0xE0 | c >> 12 & 0xF);
            data[len++] = (byte) (0x80 | c >> 6 & 0x3F);
            data[len++] = (byte) (0x80 | c & 0x3F);
         }
         else {
            data[len++] = (byte) (0xC0 | c >> 6 & 0x1F);
            data[len++] = (byte) (0x80 | c & 0x3F);
         }
      }

      length = len;
   }

   /**
    * Puts an array of bytes into this byte vector. The byte vector is automatically enlarged if necessary.
    *
    * @param bytes an array of bytes.
    * @param off index of the first byte of code that must be copied.
    * @param len number of bytes of code that must be copied.
    */
   void putByteArray(@Nonnull byte[] bytes, @Nonnegative int off, @Nonnegative int len) {
      int length = getLengthEnlargingIfNeeded(len);
      System.arraycopy(bytes, off, data, length, len);
      this.length += len;
   }

   void putByteVector(@Nonnull ByteVector another) {
      putByteArray(another.data, 0, another.length);
   }

   void increaseLengthBy(@Nonnegative int len) {
      getLengthEnlargingIfNeeded(len);
      length += len;
   }
}
