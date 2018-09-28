package mockit.asm.types;

import javax.annotation.*;

public final class MethodType extends ReferenceType
{
   /**
    * Returns the Java type corresponding to the given method descriptor.
    */
   public static MethodType create(@Nonnull String methodDescriptor) {
      char[] typeDesc = methodDescriptor.toCharArray();
      return new MethodType(typeDesc, 0, typeDesc.length);
   }

   /**
    * Initializes a method type.
    *
    * @param typeDesc a buffer containing the descriptor of the method type
    * @param off the offset of this descriptor in the previous buffer
    * @param len the length of this descriptor
    */
   MethodType(@Nonnull char[] typeDesc, @Nonnegative int off, @Nonnegative int len) {
      super(typeDesc, off, len);
   }

   @Nonnull @Override public String getClassName() { throw new UnsupportedOperationException(); }
   @Override public int getSize() { throw new UnsupportedOperationException(); }
   @Override public int getOpcode(int opcode) { throw new UnsupportedOperationException(); }
}
