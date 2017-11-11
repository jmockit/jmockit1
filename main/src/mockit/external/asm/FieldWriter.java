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

/**
 * A {@link FieldVisitor} that generates Java fields in bytecode form.
 *
 * @author Eric Bruneton
 */
final class FieldWriter extends FieldVisitor
{
   /**
    * The class writer to which this field must be added.
    */
   private final ClassWriter cw;

   /**
    * Access flags of this field.
    */
   private final int access;

   /**
    * The index of the constant pool item that contains the name of this field.
    */
   private final int name;

   /**
    * The index of the constant pool item that contains the descriptor of this field.
    */
   private final int desc;

   /**
    * The index of the constant pool item that contains the signature of this field.
    */
   private final int signature;

   /**
    * The index of the constant pool item that contains the constant value of this field.
    */
   private final int value;

   /**
    * Constructs a new {@link FieldWriter}.
    *
    * @param cw        the class writer to which this field must be added.
    * @param access    the field's access flags (see {@link Opcodes}).
    * @param name      the field's name.
    * @param desc      the field's descriptor (see {@link Type}).
    * @param signature the field's signature. May be <tt>null</tt>.
    * @param value     the field's constant value. May be <tt>null</tt>.
    */
   FieldWriter(ClassWriter cw, int access, String name, String desc, String signature, Object value) {
      this.cw = cw;
      this.access = access;
      this.name = cw.newUTF8(name);
      this.desc = cw.newUTF8(desc);
      this.signature = signature == null ? 0 : cw.newUTF8(signature);
      this.value = value == null ? 0 : cw.newConstItem(value).index;
   }

   // ------------------------------------------------------------------------
   // Implementation of the FieldVisitor base class
   // ------------------------------------------------------------------------

   @Override
   public AnnotationVisitor visitAnnotation(String desc) {
      return visitAnnotation(cw, desc);
   }

   // ------------------------------------------------------------------------
   // Utility methods
   // ------------------------------------------------------------------------

   /**
    * Returns the size of this field.
    */
   int getSize() {
      int size = 8;

      if (value != 0) {
         cw.newUTF8("ConstantValue");
         size += 8;
      }

      if (isSynthetic()) {
         cw.newUTF8("Synthetic");
         size += 6;
      }

      if (Access.isDeprecated(access)) {
         cw.newUTF8("Deprecated");
         size += 6;
      }

      if (signature != 0) {
         cw.newUTF8("Signature");
         size += 8;
      }

      size += getAnnotationsSize(cw);

      return size;
   }

   private boolean isSynthetic() {
      return cw.isSynthetic(access);
   }

   /**
    * Puts the content of this field into the given byte vector.
    *
    * @param out where the content of this field must be put.
    */
   void put(ByteVector out) {
      int accessFlag = Access.computeFlag(access, 0);
      out.putShort(accessFlag);

      out.putShort(name);
      out.putShort(desc);

      int attributeCount = 0;

      if (value != 0) {
         ++attributeCount;
      }

      boolean synthetic = isSynthetic();

      if (synthetic) {
         ++attributeCount;
      }

      boolean deprecated = Access.isDeprecated(access);

      if (deprecated) {
         ++attributeCount;
      }

      if (signature != 0) {
         ++attributeCount;
      }

      if (annotations != null) {
         ++attributeCount;
      }

      out.putShort(attributeCount);

      if (value != 0) {
         out.putShort(cw.newUTF8("ConstantValue"));
         out.putInt(2).putShort(value);
      }

      if (synthetic) {
         out.putShort(cw.newUTF8("Synthetic")).putInt(0);
      }

      if (deprecated) {
         out.putShort(cw.newUTF8("Deprecated")).putInt(0);
      }

      if (signature != 0) {
         out.putShort(cw.newUTF8("Signature"));
         out.putInt(2).putShort(signature);
      }

      putAnnotations(out, cw);
   }
}
