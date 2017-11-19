package mockit.external.asm;

import javax.annotation.*;

final class OuterClass
{
   /**
    * The constant pool item that contains the name of the attribute to be produced.
    */
   @Nonnegative private final int attributeName;

   /**
    * The constant pool item that contains the name of the enclosing class of this class.
    */
   @Nonnegative private final int enclosingMethodOwner;

   /**
    * The constant pool item that contains the name and descriptor of the enclosing method of this class.
    */
   @Nonnegative private final int enclosingMethod;

   OuterClass(@Nonnull ConstantPoolGeneration cp, @Nonnull String owner, @Nullable String name, @Nullable String desc) {
      attributeName = cp.newUTF8("EnclosingMethod");
      enclosingMethodOwner = cp.newClass(owner);
      enclosingMethod = name != null && desc != null ? cp.newNameType(name, desc) : 0;
   }

   @Nonnegative
   int getSize() { return 10; }

   void put(@Nonnull ByteVector out) {
      out.putShort(attributeName).putInt(4);
      out.putShort(enclosingMethodOwner).putShort(enclosingMethod);
   }
}
