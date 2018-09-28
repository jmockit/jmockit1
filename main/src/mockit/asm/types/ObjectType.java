package mockit.asm.types;

import javax.annotation.*;

public final class ObjectType extends ReferenceType
{
   @Nonnull
   public static ObjectType create(@Nonnull String internalName) {
      return new ObjectType(internalName.toCharArray());
   }

   /**
    * Initializes an object reference type.
    *
    * @param typeDesc a buffer containing the descriptor of the type
    * @param off the offset of the descriptor in the buffer
    */
   @Nonnull
   static ObjectType create(@Nonnull char[] typeDesc, @Nonnegative int off) {
      int len = findTypeNameLength(typeDesc, off, 0);
      return new ObjectType(typeDesc, off + 1, len - 1);
   }

   private ObjectType(@Nonnull char[] typeDesc, @Nonnegative int off, @Nonnegative int len) { super(typeDesc, off, len); }

   ObjectType(@Nonnull char[] internalName) { super(internalName); }

   @Override
   void getDescriptor(@Nonnull StringBuilder typeDesc) {
      typeDesc.append('L');
      super.getDescriptor(typeDesc);
      typeDesc.append(';');
   }

   @Nonnull @Override
   public String getClassName() {
      return getInternalName().replace('/', '.');
   }
}
