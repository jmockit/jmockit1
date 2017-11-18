package mockit.external.asm;

import javax.annotation.*;

final class ObjectType extends ReferenceType
{
   /**
    * Constructs a reference type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   ObjectType(char[] buf, int off, int len) {
      super(Sort.OBJECT, buf, off, len);
   }

   @Override
   void getDescriptor(@Nonnull StringBuffer buf) {
      buf.append('L');
      super.getDescriptor(buf);
      buf.append(';');
   }

   @Override
   public String getClassName() {
      return new String(buf, off, len).replace('/', '.');
   }
}
