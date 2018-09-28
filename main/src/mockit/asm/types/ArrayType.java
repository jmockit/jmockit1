package mockit.asm.types;

import javax.annotation.*;

public final class ArrayType extends ReferenceType
{
   @Nonnull
   public static ArrayType create(@Nonnull String typeDesc) {
      return new ArrayType(typeDesc.toCharArray());
   }

   /**
    * Initializes an array type.
    *
    * @param typeDesc a buffer containing the descriptor of the array type
    * @param off the offset of the descriptor in the buffer
    */
   @Nonnull
   static ArrayType create(@Nonnull char[] typeDesc, @Nonnegative int off) {
      int len = findNumberOfDimensions(typeDesc, off);

      if (typeDesc[off + len] == 'L') {
         len = findTypeNameLength(typeDesc, off, len);
      }

      return new ArrayType(typeDesc, off, len + 1);
   }

   @Nonnegative
   private static int findNumberOfDimensions(@Nonnull char[] typeDesc, @Nonnegative int off) {
      int dimensions = 1;

      while (typeDesc[off + dimensions] == '[') {
         dimensions++;
      }

      return dimensions;
   }

   private ArrayType(@Nonnull char[] typeDesc, @Nonnegative int off, @Nonnegative int len) { super(typeDesc, off, len); }

   ArrayType(@Nonnull char[] typeDesc) { super(typeDesc); }

   /**
    * Returns the number of dimensions of this array type.
    */
   @Nonnegative
   public int getDimensions() {
      return findNumberOfDimensions(typeDescChars, off);
   }

   /**
    * Returns the type of the elements of this array type.
    */
   @Nonnull
   public JavaType getElementType() {
      int dimensions = getDimensions();
      return getType(typeDescChars, off + dimensions);
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
