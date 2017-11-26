package mockit.external.asm;

import javax.annotation.*;

public final class ArrayType extends ReferenceType
{
   @Nonnull
   public static ArrayType create(@Nonnull String typeDesc) {
      return create(typeDesc.toCharArray(), 0);
   }

   @Nonnull
   static ArrayType create(@Nonnull char[] buf, @Nonnegative int off) {
      int len = 1;

      while (buf[off + len] == '[') {
         len++;
      }

      if (buf[off + len] == 'L') {
         len++;

         while (buf[off + len] != ';') {
            len++;
         }
      }

      return new ArrayType(buf, off, len + 1);
   }

   /**
    * Constructs an array type.
    *
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   ArrayType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      super(Sort.ARRAY, buf, off, len);
   }

   /**
    * Returns the number of dimensions of this array type.
    */
   @Nonnegative
   public int getDimensions() {
      int i = 1;

      while (buf[off + i] == '[') {
         i++;
      }

      return i;
   }

   /**
    * Returns the type of the elements of this array type.
    */
   @Nonnull
   public JavaType getElementType() {
      int dimensions = getDimensions();
      return getType(buf, off + dimensions);
   }

   @Nonnull @Override
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
