package mockit.external.asm;

final class MethodType extends ReferenceType
{
   /**
    * Constructs a method type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   MethodType(char[] buf, int off, int len) {
      super(Sort.METHOD, buf, off, len);
   }

   @Override public String getClassName() { return null; }
   @Override public int getSize() { throw new UnsupportedOperationException(); }
   @Override public int getOpcode(int opcode) { throw new UnsupportedOperationException(); }
}
