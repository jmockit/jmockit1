package mockit.external.asm;

import javax.annotation.*;

final class ArrayType extends ReferenceType
{
   /**
    * Constructs an array type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   ArrayType(char[] buf, int off, int len) {
      super(Sort.ARRAY, buf, off, len);
   }

   @Override
   public int getDimensions() {
      int i = 1;

      while (buf[off + i] == '[') {
         i++;
      }

      return i;
   }

   @Nonnull @Override
   public JavaType getElementType() {
      int dimensions = getDimensions();
      return getType(buf, off + dimensions);
   }

   @Override
   public String getClassName() {
      String className = getElementType().getClassName();
      StringBuilder sb = new StringBuilder(className);
      int dimensions = getDimensions();

      for (int i = dimensions; i > 0; i--) {
         sb.append("[]");
      }

      return sb.toString();
   }
}
