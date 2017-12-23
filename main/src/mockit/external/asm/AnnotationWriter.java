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
 */
final class AnnotationWriter extends AnnotationVisitor
{
   /**
    * The constant pool to which this annotation must be added.
    */
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * The number of values in this annotation.
    */
   @Nonnegative private int size;

   /**
    * <tt>true<tt> if values are named, <tt>false</tt> otherwise.
    * Annotation writers used for annotation default and annotation arrays use unnamed values.
    */
   private final boolean named;

   /**
    * The annotation values in bytecode form. This byte vector only contains the values themselves, i.e. the number of
    * values must be stored as an unsigned short just before these bytes.
    */
   @Nonnull private final ByteVector bv;

   /**
    * Where the number of values of this annotation must be stored in {@link #bv}.
    */
   @Nonnegative private final int offset;

   AnnotationWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String typeDesc) {
      this.cp = cp;
      named = true;
      bv = new ByteVector();
      bv.putShort(cp.newUTF8(typeDesc));
      bv.putShort(0); // reserve space for value count
      offset = 2;
   }

   private AnnotationWriter(@Nonnull AnnotationWriter parent, boolean named) {
      cp = parent.cp;
      this.named = named;
      bv = parent.bv;
      offset = bv.length - 2;
   }

   @Nonnegative @Override
   protected int getByteLength() { return bv.length; }

   @Override
   public void visit(@Nullable String name, @Nonnull Object value) {
      putName(name);

      if (value instanceof String) {
         putString('s', (String) value);
      }
      else if (putValueWhenPrimitive(value)) {
         // OK
      }
      else if (value instanceof JavaType) {
         putType((JavaType) value);
      }
      else {
         putElementValuesWhenArray(value);
      }
   }

   private void putName(@Nullable String name) {
      size++;

      if (named) {
         //noinspection ConstantConditions
         putString(name);
      }
   }

   private boolean putValueWhenPrimitive(@Nonnull Object value) {
      if (value instanceof Boolean) {
         putBoolean((Boolean) value);
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
      else {
         return false;
      }

      return true;
   }

   private void putItem(int typeCode, @Nonnull Item item) {
      bv.put12(typeCode, item.index);
   }

   private void putBoolean(boolean value) {
      putInteger('Z', value ? 1 : 0);
   }

   private void putInteger(int typeCode, int value) {
      Item item = cp.newInteger(value);
      putItem(typeCode, item);
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

   private void putType(@Nonnull JavaType type) {
      String typeDescriptor = type.getDescriptor();
      putString('c', typeDescriptor);
   }

   private void putString(int b, @Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.put12(b, itemIndex);
   }

   private void putString(@Nonnull String value) {
      int itemIndex = cp.newUTF8(value);
      bv.putShort(itemIndex);
   }

   private void putArrayLength(@Nonnegative int length) {
      bv.put12('[', length);
   }

   private void putElementValuesWhenArray(@Nonnull Object value) {
      if (value instanceof byte[]) {
         putArrayElementValues('B', value);
      }
      else if (value instanceof boolean[]) {
         putArrayElementValues('Z', value);
      }
      else if (value instanceof short[]) {
         putArrayElementValues('S', value);
      }
      else if (value instanceof char[]) {
         putArrayElementValues('C', value);
      }
      else if (value instanceof int[]) {
         putArrayElementValues('I', value);
      }
      else if (value instanceof long[]) {
         putArrayElementValues('J', value);
      }
      else if (value instanceof float[]) {
         putArrayElementValues('F', value);
      }
      else if (value instanceof double[]) {
         putArrayElementValues('D', value);
      }
   }

   private void putArrayElementValues(char elementType, @Nonnull Object array) {
      int length = Array.getLength(array);
      putArrayLength(length);

      for (int i = 0; i < length; i++) {
         if (elementType == 'J') {
            long value = Array.getLong(array, i);
            putLong(value);
         }
         else if (elementType == 'F') {
            float value = Array.getFloat(array, i);
            putFloat(value);
         }
         else if (elementType == 'D') {
            double value = Array.getDouble(array, i);
            putDouble(value);
         }
         else if (elementType == 'Z') {
            boolean value = Array.getBoolean(array, i);
            putBoolean(value);
         }
         else {
            int value = Array.getInt(array, i);
            putInteger(elementType, value);
         }
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

      // Write tag and type, and reserve space for value count.
      putString('@', desc);
      bv.putShort(0);

      return new AnnotationWriter(this, true);
   }

   @Nonnull @Override
   public AnnotationVisitor visitArray(@Nullable String name) {
      putName(name);

      // Write tag, and reserve space for array size.
      putArrayLength(0);

      return new AnnotationWriter(this, false);
   }

   @Override
   public void visitEnd() {
      byte[] data = bv.data;
      data[offset] = (byte) (size >>> 8);
      data[offset + 1] = (byte) size;
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
         aw.prev = last;
         last = aw;
         aw = aw.next;
      }

      out.putInt(size);
      out.putShort(n);
      putFromLastToFirst(out, last);
   }

   private static void putFromLastToFirst(@Nonnull ByteVector out, @Nullable AnnotationWriter aw) {
      while (aw != null) {
         out.putByteVector(aw.bv);
         aw = aw.prev;
      }
   }

   /**
    * Puts the given annotation lists into the given byte vector.
    *
    * @param out  where the annotations must be put.
    * @param anns an array of annotation writer lists.
    */
   static void put(@Nonnull ByteVector out, @Nonnull AnnotationWriter[] anns) {
      putNumberAndSizeOfAnnotations(out, anns);

      for (AnnotationWriter ann : anns) {
         AnnotationWriter last = putNumberOfAnnotations(out, ann);
         putFromLastToFirst(out, last);
      }
   }

   private static void putNumberAndSizeOfAnnotations(@Nonnull ByteVector out, @Nonnull AnnotationWriter[] anns) {
      int numAnns = anns.length;
      int size = 1 + 2 * numAnns;

      for (AnnotationWriter aw : anns) {
         if (aw != null) {
            size += aw.getSize();
         }
      }

      out.putInt(size).putByte(numAnns);
   }

   @Nullable
   private static AnnotationWriter putNumberOfAnnotations(@Nonnull ByteVector out, @Nullable AnnotationWriter aw) {
      AnnotationWriter last = null;
      int n = 0;

      while (aw != null) {
         n++;
         aw.prev = last;
         last = aw;
         aw = aw.next;
      }

      out.putShort(n);
      return last;
   }
}
