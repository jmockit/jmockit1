package mockit.asm.constantPool;

import javax.annotation.*;

import mockit.asm.util.*;

public abstract class AttributeWriter
{
   @Nonnull protected final ConstantPoolGeneration cp;

   /**
    * The index of the constant pool item that contains the name of the associated attribute.
    */
   @Nonnegative protected int attributeIndex;

   protected AttributeWriter(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   protected AttributeWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String attributeName) {
      this.cp = cp;
      setAttribute(attributeName);
   }

   protected final void setAttribute(@Nonnull String attributeName) {
      attributeIndex = cp.newUTF8(attributeName);
   }

   @Nonnegative
   public abstract int getSize();

   public void put(@Nonnull ByteVector out) {
      put(out, 2);
   }

   protected final void put(@Nonnull ByteVector out, @Nonnegative int size) {
      out.putShort(attributeIndex).putInt(size);
   }
}
