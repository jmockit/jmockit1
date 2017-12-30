package mockit.external.asm;

import javax.annotation.*;

final class OuterClassWriter extends AttributeWriter
{
   /**
    * The constant pool item that contains the name of the enclosing class of this class.
    */
   @Nonnegative private final int enclosingMethodOwner;

   /**
    * The constant pool item that contains the name and descriptor of the enclosing method of this class.
    */
   @Nonnegative private final int enclosingMethod;

   OuterClassWriter(
      @Nonnull ConstantPoolGeneration cp, @Nonnull String owner, @Nullable String name, @Nullable String desc
   ) {
      super(cp, "EnclosingMethod");
      enclosingMethodOwner = cp.newClass(owner);
      enclosingMethod = name != null && desc != null ? cp.newNameType(name, desc) : 0;
   }

   @Nonnegative @Override
   int getSize() { return 10; }

   @Override
   void put(@Nonnull ByteVector out) {
      put(out, 4);
      out.putShort(enclosingMethodOwner).putShort(enclosingMethod);
   }
}
