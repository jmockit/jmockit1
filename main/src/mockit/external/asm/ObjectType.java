package mockit.external.asm;

import javax.annotation.*;

public final class ObjectType extends ReferenceType
{
   @Nonnull
   public static ObjectType create(@Nonnull String internalName) {
      return new ObjectType(internalName.toCharArray());
   }

   @Nonnull
   static ObjectType create(@Nonnull char[] buf, @Nonnegative int off) {
      int len = findTypeNameLength(buf, off, 0);
      return new ObjectType(buf, off + 1, len - 1);
   }

   ObjectType(@Nonnull char[] buf) { super(buf); }

   /**
    * Constructs an object reference type.
    *
    * @param buf a buffer containing the descriptor of the type.
    * @param off the offset of the descriptor in the buffer.
    * @param len the length of the descriptor.
    */
   ObjectType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) { super(buf, off, len); }

   @Override
   void getDescriptor(@Nonnull StringBuilder buf) {
      buf.append('L');
      super.getDescriptor(buf);
      buf.append(';');
   }

   @Nonnull @Override
   public String getClassName() {
      return getInternalName().replace('/', '.');
   }
}
