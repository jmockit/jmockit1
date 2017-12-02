package mockit.external.asm;

import javax.annotation.*;

public final class ArrayType extends ReferenceType
{
   @Nonnull
   public static ArrayType create(@Nonnull String typeDesc) {
      return new ArrayType(typeDesc.toCharArray());
   }

   @Nonnull
   static ArrayType create(@Nonnull char[] buf, @Nonnegative int off) {
      int len = findNumberOfDimensions(buf, off);

      if (buf[off + len] == 'L') {
         len = findTypeNameLength(buf, off, len);
      }

      return new ArrayType(buf, off, len + 1);
   }

   @Nonnegative
   private static int findNumberOfDimensions(@Nonnull char[] buf, @Nonnegative int off) {
      int dimensions = 1;

      while (buf[off + dimensions] == '[') {
         dimensions++;
      }

      return dimensions;
   }

   ArrayType(@Nonnull char[] buf) { super(buf); }

   /**
    * Constructs an array type.
    *
    * @param buf a buffer containing the descriptor of the array type.
    * @param off the offset of the descriptor in the buffer.
    * @param len the length of the descriptor.
    */
   ArrayType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) { super(buf, off, len); }

   /**
    * Returns the number of dimensions of this array type.
    */
   @Nonnegative
   public int getDimensions() {
      return findNumberOfDimensions(buf, off);
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
