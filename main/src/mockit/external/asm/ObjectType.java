package mockit.external.asm;

import javax.annotation.*;

public final class ObjectType extends ReferenceType
{
   @Nonnull
   static ObjectType create(@Nonnull char[] buf, @Nonnegative int off) {
      int len = 1;

      while (buf[off + len] != ';') {
         len++;
      }

      return new ObjectType(buf, off + 1, len - 1);
   }

   /**
    * Constructs a reference type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   ObjectType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      super(buf, off, len);
   }

   @Override
   void getDescriptor(@Nonnull StringBuilder buf) {
      buf.append('L');
      super.getDescriptor(buf);
      buf.append(';');
   }

   @Nonnull @Override
   public String getClassName() {
      return new String(buf, off, len).replace('/', '.');
   }
}
