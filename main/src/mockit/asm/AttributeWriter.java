package mockit.asm;

import javax.annotation.*;

import mockit.asm.constantPool.*;

public abstract class AttributeWriter
{
   @Nonnull protected final ConstantPoolGeneration cp;

   /**
    * The index of the constant pool item that contains the name of the associated attribute.
    */
   @Nonnegative private int attributeIndex;

   protected AttributeWriter(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   AttributeWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String attributeName) {
      this.cp = cp;
      setAttribute(attributeName);
   }

   protected final void setAttribute(@Nonnull String attributeName) {
      attributeIndex = cp.newUTF8(attributeName);
   }

   @Nonnegative
   protected abstract int getSize();

   protected void put(@Nonnull ByteVector out) {
      put(out, 2);
   }

   protected final void put(@Nonnull ByteVector out, @Nonnegative int size) {
      out.putShort(attributeIndex).putInt(size);
   }
}
