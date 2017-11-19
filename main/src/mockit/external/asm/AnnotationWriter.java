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

import java.lang.reflect.*;
import javax.annotation.*;

/**
 * An {@link AnnotationVisitor} that generates annotations in bytecode form.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class AnnotationWriter extends AnnotationVisitor
{
   /**
    * The constant pool to which this annotation must be added.
    */
   private final ConstantPoolGeneration cp;

   /**
    * The number of values in this annotation.
    */
   private int size;

   /**
    * <tt>true<tt> if values are named, <tt>false</tt> otherwise.
    * Annotation writers used for annotation default and annotation arrays use unnamed values.
    */
   private final boolean named;

   /**
    * The annotation values in bytecode form. This byte vector only contains the values themselves, i.e. the number of
    * values must be stored as an unsigned short just before these bytes.
    */
   private final ByteVector bv;

   /**
    * The byte vector to be used to store the number of values of this annotation. See {@link #bv}.
    */
   private final ByteVector parent;

   /**
    * Where the number of values of this annotation must be stored in {@link #parent}.
    */
   private final int offset;

   /**
    * Constructs a new {@link AnnotationWriter}.
    *
    * @param cp     the constant pool to which this annotation must be added.
    * @param named  <tt>true<tt> if values are named, <tt>false</tt> otherwise.
    * @param bv     where the annotation values must be stored.
    * @param parent where the number of annotation values must be stored.
    * @param offset where in <tt>parent</tt> the number of annotation values must be stored.
    */
   AnnotationWriter(ConstantPoolGeneration cp, boolean named, ByteVector bv, ByteVector parent, int offset) {
      this.cp = cp;
      this.named = named;
      this.bv = bv;
      this.parent = parent;
      this.offset = offset;
   }

   @Override
   protected int getByteLength() { return bv.length; }

   @Override
   public void visit(@Nullable String name, @Nonnull Object value) {
      putName(name);

      if (value instanceof String) {
         putString('s', (String) value);
      }
      else if (value instanceof Boolean) {
         int v = (Boolean) value ? 1 : 0;
         putInteger('Z', v);
      }
      else if (value instanceof Integer) {
         putInteger('I', (Integer) value);
      }
      else if (value instanceof Double) {
         putDouble((Double) value);
      }
      else if (value instanceof Float) {
         putFloat((Float) value);
      }
      else if (value instanceof Long) {
         putLong((Long) value);
      }
      else if (value instanceof Byte) {
         putInteger('B', (Byte) value);
      }
      else if (value instanceof Character) {
         putInteger('C', (Character) value);
      }
      else if (value instanceof Short) {
         putInteger('S', (Short) value);
      }
      else if (value instanceof JavaType) {
         String typeDescriptor = ((JavaType) value).getDescriptor();
         putString('c', typeDescriptor);
      }
      else if (value instanceof byte[]) {
         readAnnotationValues('B', value);
      }
      else if (value instanceof boolean[]) {
         readAnnotationValues('Z', value);
      }
      else if (value instanceof short[]) {
         readAnnotationValues('S', value);
      }
      else if (value instanceof char[]) {
         readAnnotationValues('C', value);
      }
      else if (value instanceof int[]) {
         readAnnotationValues('I', value);
      }
      else if (value instanceof long[]) {
         readAnnotationValues('J', value);
      }
      else if (value instanceof float[]) {
         readAnnotationValues('F', value);
      }
      else if (value instanceof double[]) {
         readAnnotationValues('D', value);
      }
   }

   private void putName(@Nullable String name) {
      size++;

      if (named) {
         //noinspection ConstantConditions
         putString(name);
      }
   }

   private void putItem(int b, @Nonnull Item item) {
      bv.put12(b, item.index);
   }

   private void putInteger(int b, int value) {
      Item item = cp.newInteger(value);
      putItem(b, item);
   }

   private void putDouble(double value) {
      Item item = cp.newDouble(value);
      putItem('D', item);
   }

   private void putFloat(float value) {
      Item item = cp.newFloat(value);
      putItem('F', item);
   }

   private void putLong(long value) {
      Item item = cp.newLong(value);
      putItem('J', item);
   }

   private void putString(int b, @Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.put12(b, itemIndex);
   }

   private void putString(@Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.putShort(itemIndex);
   }

   private void putArrayLength(int length) {
      bv.put12('[', length);
   }

   private void readAnnotationValues(char arrayType, @Nonnull Object arrayValue) {
      int length = Array.getLength(arrayValue);
      putArrayLength(length);

      for (int i = 0; i < length; i++) {
         Item item;

         if (arrayType == 'J') {
            long elementValue = Array.getLong(arrayValue, i);
            item = cp.newLong(elementValue);
         }
         else if (arrayType == 'F') {
            float elementValue = Array.getFloat(arrayValue, i);
            item = cp.newFloat(elementValue);
         }
         else if (arrayType == 'D') {
            double elementValue = Array.getDouble(arrayValue, i);
            item = cp.newDouble(elementValue);
         }
         else {
            int value = arrayType == 'Z' ? Array.getBoolean(arrayValue, i) ? 1 : 0 : Array.getInt(arrayValue, i);
            item = cp.newInteger(value);
         }

         putItem(arrayType, item);
      }
   }

   @Override
   public void visitEnum(@Nullable String name, @Nonnull String desc, @Nonnull String value) {
      putName(name);
      putString('e', desc);
      putString(value);
   }

   @Nonnull @Override
   public AnnotationVisitor visitAnnotation(@Nullable String name, @Nonnull String desc) {
      putName(name);

      // Write tag and type, and reserve space for values count.
      putString('@', desc);
      bv.putShort(0);

      return new AnnotationWriter(cp, true, bv, bv, bv.length - 2);
   }

   @Nonnull @Override
   public AnnotationVisitor visitArray(@Nullable String name) {
      putName(name);

      // Write tag, and reserve space for array size.
      putArrayLength(0);

      return new AnnotationWriter(cp, false, bv, bv, bv.length - 2);
   }

   @Override
   public void visitEnd() {
      if (parent != null) {
         byte[] data = parent.data;
         data[offset] = (byte) (size >>> 8);
         data[offset + 1] = (byte) size;
      }
   }

   // ------------------------------------------------------------------------
   // Utility methods
   // ------------------------------------------------------------------------

   /**
    * Puts the annotations of this annotation writer list into the given byte vector.
    *
    * @param out where the annotations must be put.
    */
   void put(@Nonnull ByteVector out) {
      AnnotationWriter aw = this;
      AnnotationWriter last = null;
      int n = 0;
      int size = 2;

      while (aw != null) {
         n++;
         size += aw.getByteLength();
         aw.visitEnd(); // in case user forgot to call visitEnd
         aw.prev = last;
         last = aw;
         aw = aw.next;
      }

      out.putInt(size);
      out.putShort(n);
      putFromLastToFirst(out, last);
   }

   private static void putFromLastToFirst(@Nonnull ByteVector out, AnnotationWriter aw) {
      while (aw != null) {
         out.putByteVector(aw.bv);
         aw = aw.prev;
      }
   }

   /**
    * Puts the given annotation lists into the given byte vector.
    *
    * @param anns an array of annotation writer lists.
    * @param out  where the annotations must be put.
    */
   static void put(@Nonnull AnnotationWriter[] anns, @Nonnull ByteVector out) {
      int numAnns = anns.length;
      int size = 1 + 2 * numAnns;

      for (int i = 0; i < numAnns; ++i) {
         AnnotationWriter aw = anns[i];
         size += aw == null ? 0 : aw.getSize();
      }

      out.putInt(size).putByte(numAnns);

      for (int i = 0; i < numAnns; i++) {
         AnnotationWriter aw = anns[i];
         AnnotationWriter last = null;
         int n = 0;

         while (aw != null) {
            n++;
            aw.visitEnd(); // in case user forgot to call visitEnd
            aw.prev = last;
            last = aw;
            aw = aw.next;
         }

         out.putShort(n);
         putFromLastToFirst(out, last);
      }
   }
}
