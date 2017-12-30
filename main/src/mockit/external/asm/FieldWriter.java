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
 * A {@link FieldVisitor} that generates Java fields in bytecode form.
 */
final class FieldWriter extends FieldVisitor
{
   /**
    * The index of the constant pool item that contains the name of this field.
    */
   @Nonnegative private final int name;

   /**
    * The index of the constant pool item that contains the descriptor of this field.
    */
   @Nonnegative private final int desc;

   @Nullable private final SignatureWriter signatureWriter;

   /**
    * The index of the constant pool item that contains the constant value of this field.
    */
   @Nonnegative private final int value;

   /**
    * Initializes a new Field Writer.
    *
    * @param cw        the class writer to which this field must be added.
    * @param access    the field's access flags (see {@link Opcodes}).
    * @param name      the field's name.
    * @param desc      the field's descriptor (see {@link JavaType}).
    * @param signature the field's signature.
    * @param value     the field's constant value.
    */
   FieldWriter(
      @Nonnull ClassWriter cw, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature,
      @Nullable Object value
   ) {
      cp = cw.cp;
      this.access = access;
      this.name = cp.newUTF8(name);
      this.desc = cp.newUTF8(desc);
      signatureWriter = signature == null ? null : new SignatureWriter(cp, signature);
      this.value = value == null ? 0 : cp.newConstItem(value).index;

      createMarkerAttributes(cw.version);
   }

   @Nonnull @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return addAnnotation(desc);
   }

   /**
    * Returns the size of this field.
    */
   @Nonnegative
   int getSize() {
      int size = 8 + getMarkerAttributesSize() + getAnnotationsSize();

      if (value != 0) {
         cp.newUTF8("ConstantValue");
         size += 8;
      }

      if (signatureWriter != null) {
         size += signatureWriter.getSize();
      }

      return size;
   }

   /**
    * Puts the content of this field into the given byte vector.
    */
   @Override
   void put(@Nonnull ByteVector out) {
      putAccess(out, 0);
      out.putShort(name);
      out.putShort(desc);

      int attributeCount = getAttributeCount();
      out.putShort(attributeCount);

      if (value != 0) {
         out.putShort(cp.newUTF8("ConstantValue"));
         out.putInt(2).putShort(value);
      }

      putMarkerAttributes(out);

      if (signatureWriter != null) {
         signatureWriter.put(out);
      }

      putAnnotations(out);
   }

   @Nonnegative
   private int getAttributeCount() {
      int attributeCount = getMarkerAttributeCount();

      if (value != 0) {
         attributeCount++;
      }

      if (signatureWriter != null) {
         attributeCount++;
      }

      if (annotations != null) {
         attributeCount++;
      }

      return attributeCount;
   }
}
