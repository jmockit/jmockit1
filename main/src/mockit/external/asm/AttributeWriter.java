package mockit.external.asm;

import javax.annotation.*;

abstract class AttributeWriter
{
   @Nonnull final ConstantPoolGeneration cp;

   /**
    * The index of the constant pool item that contains the name of the associated attribute.
    */
   @Nonnegative private int attributeIndex;

   AttributeWriter(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   AttributeWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String attributeName) {
      this.cp = cp;
      setAttribute(attributeName);
   }

   final void setAttribute(@Nonnull String attributeName) {
      attributeIndex = cp.newUTF8(attributeName);
   }

   @Nonnegative
   int getAttributeCount() { return attributeIndex == 0 ? 0 : 1; }

   @Nonnegative
   abstract int getSize();

   void put(@Nonnull ByteVector out) {
      put(out, 2);
   }

   final void put(@Nonnull ByteVector out, @Nonnegative int size) {
      out.putShort(attributeIndex).putInt(size);
   }
}
