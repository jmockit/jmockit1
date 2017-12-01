package mockit.external.asm;

import javax.annotation.*;

final class MethodType extends ReferenceType
{
   /**
    * Returns the Java type corresponding to the given method descriptor.
    */
   static MethodType create(@Nonnull String methodDescriptor) {
      char[] buf = methodDescriptor.toCharArray();
      return new MethodType(buf, 0, buf.length);
   }

   /**
    * Constructs a method type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   MethodType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      super(buf, off, len);
   }

   @Nonnull @Override public String getClassName() { throw new UnsupportedOperationException(); }
   @Override public int getSize() { throw new UnsupportedOperationException(); }
   @Override public int getOpcode(int opcode) { throw new UnsupportedOperationException(); }
}
